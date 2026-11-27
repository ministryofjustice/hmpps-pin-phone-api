package uk.gov.justice.digital.hmpps.pinphoneapi.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.pinphoneapi.client.btPinPhoneClient.BtPinPhoneClient
import uk.gov.justice.digital.hmpps.pinphoneapi.client.btPinPhoneClient.generated.AccountCreditResponse
import uk.gov.justice.digital.hmpps.pinphoneapi.client.medusaapiclient.generated.CompleteCartResponse
import uk.gov.justice.digital.hmpps.pinphoneapi.client.medusaapiclient.generated.GetCartResponse
import uk.gov.justice.digital.hmpps.pinphoneapi.client.medusaapiclient.generated.PaymentRequest
import uk.gov.justice.digital.hmpps.pinphoneapi.client.medusaclient.MedusaInternalClient
import uk.gov.justice.digital.hmpps.pinphoneapi.client.prisonfinance.DuplicateFinanceRequestException
import uk.gov.justice.digital.hmpps.pinphoneapi.client.prisonfinance.generated.Transaction
import uk.gov.justice.digital.hmpps.pinphoneapi.config.UpstreamException
import uk.gov.justice.digital.hmpps.pinphoneapi.integration.CART_ID
import uk.gov.justice.digital.hmpps.pinphoneapi.integration.PRISONER_ID
import uk.gov.justice.digital.hmpps.pinphoneapi.integration.PRISONER_NUMBER
import uk.gov.justice.digital.hmpps.pinphoneapi.integration.TRANSACTION_ID
import java.time.LocalDateTime

class PinPhoneBuyCreditOrchestrationServiceV2Test {

  private val financeService: PrisonFinanceService = mock()
  private val medusaStoreClient: MedusaInternalClient = mock()
  private val btPinPhoneClient: BtPinPhoneClient = mock()

  private lateinit var service: PinPhoneBuyCreditOrchestrationServiceV2

  @BeforeEach
  fun setUp() {
    service = PinPhoneBuyCreditOrchestrationServiceV2(
      financeService,
      medusaStoreClient,
      btPinPhoneClient,
    )
  }

  @Test
  fun `processCheckout successfully processes payment`() {
    // Given
    val cartId = CART_ID
    val amount: Long = 100
    val paymentRequest = PaymentRequest(amountPence = 100, offenderNo = "A1234BC", prisonId = "MDI")

    whenever(financeService.createTransactionPhone(paymentRequest.prisonId, paymentRequest.offenderNo, paymentRequest.amountPence, cartId))
      .thenReturn(Transaction(id = TRANSACTION_ID))

    whenever(btPinPhoneClient.addCredit(any()))
      .thenReturn(
        Mono.just(
          AccountCreditResponse(
            reference = "ref-123",
            prisonerId = PRISONER_ID,
            creditLimitPence = 5000,
            preBalancePence = 1000,
            newBalancePence = 1100,
          ),
        ),
      )

    whenever(medusaStoreClient.completeCart(any(), any()))
      .thenReturn(
        CompleteCartResponse(
          paymentSuccessful = true,
          orderStatusRecorded = true,
          orderId = "test-order-id",
          cartId = cartId,
        ),
      )

    // When
    val result = service.processCheckout(paymentRequest, cartId)

    // Then
    assertEquals(true, result.paymentSuccessful)
    assertEquals(true, result.orderStatusRecorded)
    assertEquals("test-order-id", result.orderId)
    assertEquals(cartId, result.cartId)

    verify(financeService).createTransactionPhone(eq(PRISONER_ID), eq(PRISONER_NUMBER), eq(amount), eq(cartId))
    verify(btPinPhoneClient).addCredit(any())

    val paymentResultCaptor = argumentCaptor<PaymentRequest>()
    verify(medusaStoreClient).completeCart(eq(cartId), paymentResultCaptor.capture())
    assertEquals(PaymentRequest.PaymentStatus.AUTHORIZED, paymentResultCaptor.firstValue.paymentStatus)
  }

  @Test
  fun `processCheckout retries BT API and succeeds`() {
    // Given
    val cartId = CART_ID
    val paymentRequest = PaymentRequest(amountPence = 100, offenderNo = "A1234BC", prisonId = "MDI")

    whenever(financeService.createTransactionPhone(paymentRequest.prisonId, paymentRequest.offenderNo, paymentRequest.amountPence, cartId))
      .thenReturn(Transaction(id = TRANSACTION_ID))

    whenever(btPinPhoneClient.addCredit(any()))
      .thenReturn(Mono.error(UpstreamException("BT failed")))
      .thenReturn(
        Mono.just(
          AccountCreditResponse(
            reference = "ref-123",
            prisonerId = PRISONER_ID,
            creditLimitPence = 5000,
            preBalancePence = 1000,
            newBalancePence = 1100,
          ),
        ),
      )

    whenever(medusaStoreClient.completeCart(any(), any()))
      .thenReturn(
        CompleteCartResponse(
          paymentSuccessful = true,
          orderStatusRecorded = true,
          orderId = "test-order-id",
          cartId = cartId,
        ),
      )

    // When
    val result = service.processCheckout(paymentRequest, cartId)

    // Then
    assertEquals(true, result.paymentSuccessful)
    assertEquals(true, result.orderStatusRecorded)
    assertEquals("test-order-id", result.orderId)
    assertEquals(cartId, result.cartId)
    verify(btPinPhoneClient, times(2)).addCredit(any())
  }

  @Test
  fun `processCheckout retries BT API and fails`() {
    // Given
    val cartId = CART_ID
    val paymentRequest = PaymentRequest(amountPence = 100, offenderNo = "A1234BC", prisonId = "MDI")

    whenever(financeService.createTransactionPhone(paymentRequest.prisonId, paymentRequest.offenderNo, paymentRequest.amountPence, cartId))
      .thenReturn(Transaction(id = TRANSACTION_ID))

    whenever(btPinPhoneClient.addCredit(any()))
      .thenReturn(Mono.error(UpstreamException("BT failed")))
      .thenReturn(Mono.error(UpstreamException("BT failed again")))
      .thenReturn(Mono.error(UpstreamException("BT Down")))

    whenever(medusaStoreClient.completeCart(any(), any()))
      .thenReturn(
        CompleteCartResponse(
          paymentSuccessful = false,
          orderStatusRecorded = true,
          orderId = null,
          cartId = cartId,
        ),
      )
    val result = service.processCheckout(paymentRequest, cartId)

    // Then
    assertEquals(false, result.paymentSuccessful)
    assertEquals(true, result.orderStatusRecorded)
    assertEquals(null, result.orderId)
    assertEquals(cartId, result.cartId)
    verify(btPinPhoneClient, times(3)).addCredit(any())
    verify(financeService).refundTransactionPhone(eq("MDI"), eq("A1234BC"), eq(100), eq(cartId), eq(TRANSACTION_ID))
    verify(financeService, times(0)).releaseHoldAndCreateTransaction(any(), any(), any(), any(), eq(cartId))

    val captor = argumentCaptor<PaymentRequest>()
    verify(medusaStoreClient).completeCart(eq(CART_ID), captor.capture())
    assertEquals(PaymentRequest.PaymentStatus.ERROR, captor.firstValue.paymentStatus)
    assertEquals(PaymentRequest.ErrorCode.BT_PAYMENT_FAILED, captor.firstValue.errorCode)
  }

  @Test
  fun `processCheckout returns error when refund transaction fails`() {
    // Given
    val cartId = CART_ID
    val paymentRequest = PaymentRequest(amountPence = 100, offenderNo = "A1234BC", prisonId = "MDI")

    whenever(financeService.createTransactionPhone(paymentRequest.prisonId, paymentRequest.offenderNo, paymentRequest.amountPence, cartId))
      .thenReturn(Transaction(id = TRANSACTION_ID))

    whenever(btPinPhoneClient.addCredit(any()))
      .thenReturn(Mono.error(UpstreamException("BT failed")))

    whenever(financeService.refundTransactionPhone(any(), any(), any(), any(), any()))
      .thenThrow(UpstreamException("Refund failed"))

    // When
    val result = service.processCheckout(paymentRequest, cartId)
    // Then
    assertEquals(false, result.paymentSuccessful)
    assertEquals(true, result.orderStatusRecorded)
    assertEquals(null, result.orderId)
    assertEquals(cartId, result.cartId)

    val paymentResultCaptor = argumentCaptor<PaymentRequest>()
    verify(medusaStoreClient).completeCart(eq(cartId), paymentResultCaptor.capture())
    assertEquals(PaymentRequest.PaymentStatus.ERROR, paymentResultCaptor.firstValue.paymentStatus)
    assertEquals(PaymentRequest.ErrorCode.PHONE_REFUND_TRANSACTION_FAILED, paymentResultCaptor.firstValue.errorCode)
  }

  @Test
  fun `processCheckout returns paymentSuccessful true when payment succeeds but Medusa recording fails`() {
    val cartId = CART_ID
    val paymentRequest = PaymentRequest(amountPence = 100, offenderNo = "A1234BC", prisonId = "MDI")

    whenever(financeService.createTransactionPhone(paymentRequest.prisonId, paymentRequest.offenderNo, paymentRequest.amountPence, cartId))
      .thenReturn(Transaction(id = TRANSACTION_ID))

    whenever(btPinPhoneClient.addCredit(any()))
      .thenReturn(
        Mono.just(
          AccountCreditResponse(
            reference = "ref-123",
            prisonerId = PRISONER_ID,
            creditLimitPence = 5000,
            preBalancePence = 1000,
            newBalancePence = 1100,
          ),
        ),
      )

    whenever(medusaStoreClient.completeCart(any(), any()))
      .thenThrow(RuntimeException("Some medusa issue"))

    val result = service.processCheckout(paymentRequest, CART_ID)

    assertEquals(true, result.paymentSuccessful)
    assertEquals(false, result.orderStatusRecorded)
    assertEquals(null, result.orderId)
    assertEquals(CART_ID, result.cartId)

    // Verify transaction not refunded, issues with medusa should not impact payment as will be already processed
    verify(financeService, times(0)).refundTransactionPhone(any(), any(), any(), any(), any())
  }

  @Test
  fun `processCheckout returns paymentSuccessful false when both BT and Medusa recording fail`() {
    val cartId = CART_ID
    val paymentRequest = PaymentRequest(amountPence = 100, offenderNo = "A1234BC", prisonId = "MDI")

    whenever(financeService.createTransactionPhone(paymentRequest.prisonId, paymentRequest.offenderNo, paymentRequest.amountPence, cartId))
      .thenReturn(Transaction(id = TRANSACTION_ID))

    whenever(btPinPhoneClient.addCredit(any()))
      .thenReturn(Mono.error(UpstreamException("BT failed")))

    whenever(medusaStoreClient.completeCart(any(), any()))
      .thenThrow(RuntimeException("Some medusa issue"))

    val result = service.processCheckout(paymentRequest, CART_ID)

    assertEquals(false, result.paymentSuccessful)
    assertEquals(false, result.orderStatusRecorded)
    assertEquals(null, result.orderId)
    verify(financeService).refundTransactionPhone(eq("MDI"), eq("A1234BC"), eq(100), eq(CART_ID), eq(TRANSACTION_ID))
  }

  @Test
  fun `processCheckout create transaction fails, medusa is called for recording`() {
    val cartId = CART_ID
    val paymentRequest = PaymentRequest(amountPence = 100, offenderNo = "A1234BC", prisonId = "MDI")

    whenever(financeService.createTransactionPhone(paymentRequest.prisonId, paymentRequest.offenderNo, paymentRequest.amountPence, cartId))
      .thenThrow(UpstreamException("Finance service unavailable"))

    // When
    val result = service.processCheckout(paymentRequest, cartId)
    // Then
    assertEquals(false, result.paymentSuccessful)
    assertEquals(true, result.orderStatusRecorded)
    assertEquals(null, result.orderId)
    assertEquals(cartId, result.cartId)

    val paymentResultCaptor = argumentCaptor<PaymentRequest>()
    verify(medusaStoreClient).completeCart(eq(cartId), paymentResultCaptor.capture())
    assertEquals(PaymentRequest.PaymentStatus.ERROR, paymentResultCaptor.firstValue.paymentStatus)

    verify(btPinPhoneClient, times(0)).addCredit(any())
    verify(financeService, times(0)).refundTransactionPhone(any(), any(), any(), any(), any())
    verify(medusaStoreClient, times(1)).completeCart(any(), any())
  }

  @Test
  fun `processCheckout returns success when create transaction is duplicate and original completes after polling`() {
    val cartId = CART_ID
    val paymentRequest = PaymentRequest(amountPence = 100, offenderNo = "A1234BC", prisonId = "MDI")

    whenever(financeService.createTransactionPhone(paymentRequest.prisonId, paymentRequest.offenderNo, paymentRequest.amountPence, cartId))
      .thenThrow(DuplicateFinanceRequestException("CLIENT-HOLD-$cartId"))

    whenever(medusaStoreClient.getCart(cartId))
      .thenReturn(
        GetCartResponse(
          cartId = cartId,
          completed = false,
          completedAt = null,
          orderId = null,
          offenderNo = "A1234BC",
        ),
      )
      .thenReturn(
        GetCartResponse(
          cartId = cartId,
          completed = false,
          completedAt = null,
          orderId = null,
          offenderNo = "A1234BC",
        ),
      )
      .thenReturn(
        GetCartResponse(
          cartId = cartId,
          completed = true,
          completedAt = LocalDateTime.now().minusDays(1),
          orderId = "order_123",
          offenderNo = "A1234BC",
        ),
      )

    // When
    val result = service.processCheckout(paymentRequest, cartId)

    // Then
    assertEquals(true, result.paymentSuccessful)
    assertEquals(true, result.orderStatusRecorded)
    assertEquals("order_123", result.orderId)
    assertEquals(cartId, result.cartId)

    verify(medusaStoreClient, times(3)).getCart(cartId)
    verify(btPinPhoneClient, never()).addCredit(any())
    verify(medusaStoreClient, never()).completeCart(any(), any())
  }
}
