package uk.gov.justice.digital.hmpps.pinphoneapi.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.pinphoneapi.client.btPinPhoneClient.BtPinPhoneClient
import uk.gov.justice.digital.hmpps.pinphoneapi.client.btPinPhoneClient.generated.AccountCreditResponse
import uk.gov.justice.digital.hmpps.pinphoneapi.client.medusaapiclient.generated.CartMetadata
import uk.gov.justice.digital.hmpps.pinphoneapi.client.medusaapiclient.generated.CartResponse
import uk.gov.justice.digital.hmpps.pinphoneapi.client.medusaapiclient.generated.CartResponseCart
import uk.gov.justice.digital.hmpps.pinphoneapi.client.medusaapiclient.generated.CompleteCartResponse
import uk.gov.justice.digital.hmpps.pinphoneapi.client.medusaapiclient.generated.CreateCartRequest
import uk.gov.justice.digital.hmpps.pinphoneapi.client.medusaapiclient.generated.PaymentRequest
import uk.gov.justice.digital.hmpps.pinphoneapi.client.medusaclient.MedusaInternalClient
import uk.gov.justice.digital.hmpps.pinphoneapi.client.prisonfinance.generated.HoldDetails
import uk.gov.justice.digital.hmpps.pinphoneapi.client.prisonfinance.generated.Transaction
import uk.gov.justice.digital.hmpps.pinphoneapi.config.UpstreamException
import uk.gov.justice.digital.hmpps.pinphoneapi.integration.CART_ID
import uk.gov.justice.digital.hmpps.pinphoneapi.integration.HOLD_NUMBER
import uk.gov.justice.digital.hmpps.pinphoneapi.integration.PRISONER_ID
import uk.gov.justice.digital.hmpps.pinphoneapi.integration.PRISONER_NUMBER

class PinPhoneBuyCreditOrchestrationServiceTest {

  private val financeService: PrisonFinanceService = mock()
  private val medusaStoreClient: MedusaInternalClient = mock()
  private val btPinPhoneClient: BtPinPhoneClient = mock()

  private lateinit var service: PinPhoneBuyCreditOrchestrationService

  @BeforeEach
  fun setUp() {
    service = PinPhoneBuyCreditOrchestrationService(
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

    whenever(financeService.addHold(paymentRequest.prisonId, paymentRequest.offenderNo, paymentRequest.amountPence))
      .thenReturn(HoldDetails(holdNumber = HOLD_NUMBER))

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

    whenever(financeService.releaseHoldAndCreateTransaction(any(), any(), any(), any()))
      .thenReturn(Transaction(id = "tx123"))

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

    verify(financeService).addHold(eq(PRISONER_ID), eq(PRISONER_NUMBER), eq(amount))
    verify(btPinPhoneClient).addCredit(any())
    verify(financeService).releaseHoldAndCreateTransaction(eq(PRISONER_ID), eq(PRISONER_NUMBER), eq(HOLD_NUMBER), any())

    val paymentResultCaptor = argumentCaptor<PaymentRequest>()
    verify(medusaStoreClient).completeCart(eq(cartId), paymentResultCaptor.capture())
    assertEquals(PaymentRequest.PaymentStatus.AUTHORIZED, paymentResultCaptor.firstValue.paymentStatus)
  }

  @Test
  fun `processCheckout retries BT API and succeeds`() {
    // Given
    val cartId = CART_ID
    val paymentRequest = PaymentRequest(amountPence = 100, offenderNo = "A1234BC", prisonId = "MDI")

    whenever(financeService.addHold(paymentRequest.prisonId, paymentRequest.offenderNo, paymentRequest.amountPence))
      .thenReturn(HoldDetails(holdNumber = HOLD_NUMBER))

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

    whenever(financeService.releaseHoldAndCreateTransaction(any(), any(), any(), any()))
      .thenReturn(Transaction(id = "tx123"))

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

    whenever(financeService.addHold(paymentRequest.prisonId, paymentRequest.offenderNo, paymentRequest.amountPence))
      .thenReturn(HoldDetails(holdNumber = HOLD_NUMBER))
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
    verify(financeService).releaseHold(eq("MDI"), eq("A1234BC"), eq(HOLD_NUMBER))
    verify(financeService, times(0)).releaseHoldAndCreateTransaction(any(), any(), any(), any())

    val captor = argumentCaptor<PaymentRequest>()
    verify(medusaStoreClient).completeCart(eq(CART_ID), captor.capture())
    assertEquals(PaymentRequest.PaymentStatus.ERROR, captor.firstValue.paymentStatus)
    assertEquals(PaymentRequest.ErrorCode.BT_PAYMENT_FAILED, captor.firstValue.errorCode)
  }

  @Test
  fun `processCheckout returns error and releases hold when transaction fails`() {
    // Given
    val cartId = CART_ID
    val paymentRequest = PaymentRequest(amountPence = 100, offenderNo = "A1234BC", prisonId = "MDI")

    whenever(financeService.addHold(paymentRequest.prisonId, paymentRequest.offenderNo, paymentRequest.amountPence))
      .thenReturn(HoldDetails(holdNumber = HOLD_NUMBER))

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

    whenever(financeService.releaseHoldAndCreateTransaction(any(), any(), any(), any()))
      .thenThrow(UpstreamException("Transaction failed"))

    // When
    val result = service.processCheckout(paymentRequest, cartId)
    // Then
    assertEquals(false, result.paymentSuccessful)
    assertEquals(true, result.orderStatusRecorded)
    assertEquals(null, result.orderId)
    assertEquals(cartId, result.cartId)
    verify(financeService).releaseHold(eq(PRISONER_ID), eq(PRISONER_NUMBER), eq(HOLD_NUMBER))

    val paymentResultCaptor = argumentCaptor<PaymentRequest>()
    verify(medusaStoreClient).completeCart(eq(cartId), paymentResultCaptor.capture())
    assertEquals(PaymentRequest.PaymentStatus.ERROR, paymentResultCaptor.firstValue.paymentStatus)
    assertEquals(PaymentRequest.ErrorCode.RELEASE_HOLD_CREATE_TRANSACTION_FAILED, paymentResultCaptor.firstValue.errorCode)
  }

  @Test
  fun `processCheckout returns error when releases hold fails`() {
    // Given
    val cartId = CART_ID
    val paymentRequest = PaymentRequest(amountPence = 100, offenderNo = "A1234BC", prisonId = "MDI")

    whenever(financeService.addHold(paymentRequest.prisonId, paymentRequest.offenderNo, paymentRequest.amountPence))
      .thenReturn(HoldDetails(holdNumber = HOLD_NUMBER))

    whenever(btPinPhoneClient.addCredit(any()))
      .thenReturn(Mono.error(UpstreamException("BT failed")))

    whenever(financeService.releaseHold(any(), any(), any()))
      .thenThrow(UpstreamException("Release failed"))

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
    assertEquals(PaymentRequest.ErrorCode.RELEASE_HOLD_FAILED, paymentResultCaptor.firstValue.errorCode)
  }

  @Test
  fun `processCheckout returns error and releases hold when upstream error occurs`() {
    // Given
    val cartId = CART_ID
    val paymentRequest = PaymentRequest(amountPence = 100, offenderNo = "A1234BC", prisonId = "MDI")

    whenever(financeService.addHold(any(), any(), any()))
      .thenReturn(HoldDetails(holdNumber = HOLD_NUMBER))

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

    whenever(financeService.releaseHoldAndCreateTransaction(any(), any(), any(), any()))
      .thenThrow(UpstreamException("Upstream error"))

    // When
    val result = service.processCheckout(paymentRequest, cartId)

    // Then
    assertEquals(false, result.paymentSuccessful)
    assertEquals(true, result.orderStatusRecorded)
    assertEquals(null, result.orderId)
    assertEquals(cartId, result.cartId)
    verify(financeService).releaseHold(eq(PRISONER_ID), eq(PRISONER_NUMBER), eq(HOLD_NUMBER))

    val paymentResultCaptor = argumentCaptor<PaymentRequest>()
    verify(medusaStoreClient).completeCart(eq(CART_ID), paymentResultCaptor.capture())
    assertEquals(PaymentRequest.PaymentStatus.ERROR, paymentResultCaptor.firstValue.paymentStatus)
    assertEquals("Upstream error", paymentResultCaptor.firstValue.errorMessage)
    assertEquals(PaymentRequest.ErrorCode.RELEASE_HOLD_CREATE_TRANSACTION_FAILED, paymentResultCaptor.firstValue.errorCode)
  }

  @Test
  fun `processCheckout returns paymentSuccessful true when payment succeeds but Medusa recording fails`() {
    val paymentRequest = PaymentRequest(amountPence = 100, offenderNo = "A1234BC", prisonId = "MDI")

    whenever(financeService.addHold(any(), any(), any()))
      .thenReturn(HoldDetails(holdNumber = HOLD_NUMBER))

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

    whenever(financeService.releaseHoldAndCreateTransaction(any(), any(), any(), any()))
      .thenReturn(Transaction(id = "tx123"))

    whenever(medusaStoreClient.completeCart(any(), any()))
      .thenThrow(RuntimeException("Some medusa issue"))

    val result = service.processCheckout(paymentRequest, CART_ID)

    assertEquals(true, result.paymentSuccessful)
    assertEquals(false, result.orderStatusRecorded)
    assertEquals(null, result.orderId)
    assertEquals(CART_ID, result.cartId)

    // Verify hold not released, issues with medusa should not impact payment as will be already processed
    verify(financeService, times(0)).releaseHold(any(), any(), any())
  }

  @Test
  fun `processCheckout returns paymentSuccessful false when both BT and Medusa recording fail`() {
    val paymentRequest = PaymentRequest(amountPence = 100, offenderNo = "A1234BC", prisonId = "MDI")

    whenever(financeService.addHold(any(), any(), any()))
      .thenReturn(HoldDetails(holdNumber = HOLD_NUMBER))

    whenever(btPinPhoneClient.addCredit(any()))
      .thenReturn(Mono.error(UpstreamException("BT failed")))

    whenever(medusaStoreClient.completeCart(any(), any()))
      .thenThrow(RuntimeException("Some medusa issue"))

    val result = service.processCheckout(paymentRequest, CART_ID)

    assertEquals(false, result.paymentSuccessful)
    assertEquals(false, result.orderStatusRecorded)
    assertEquals(null, result.orderId)
    verify(financeService).releaseHold(eq("MDI"), eq("A1234BC"), eq(HOLD_NUMBER))
  }

  @Test
  fun `processCheckout addHold fails, medusa is called for recording`() {
    val cartId = CART_ID
    val paymentRequest = PaymentRequest(amountPence = 100, offenderNo = "A1234BC", prisonId = "MDI")

    whenever(financeService.addHold(any(), any(), any()))
      .thenThrow(UpstreamException("Hold failed"))

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
    verify(financeService, times(0)).releaseHold(any(), any(), any())
    verify(medusaStoreClient, times(1)).completeCart(any(), any())
  }

  @Test
  fun `createCart successfully creates a cart`() {
    // Given
    val request = CreateCartRequest(
      CartMetadata(
        prisonId = PRISONER_ID,
        offenderNo = PRISONER_NUMBER,
        firstName = "John",
        secondName = "Doe",
      ),

    )
    val medusaCart = CartResponse(
      CartResponseCart(
        id = CART_ID,
      ),
    )
    val medusaResponse = CartResponse(medusaCart.cart)
    whenever(medusaStoreClient.createCart(any())).thenReturn(medusaResponse)

    // When
    val result = service.createCart(request)

    // Then
    assertEquals(CART_ID, result.body?.cart?.id)
    verify(medusaStoreClient).createCart(any())
  }

  @Test
  fun `createCart throws UpstreamException when client fails`() {
    whenever(medusaStoreClient.createCart(any())).thenThrow(UpstreamException("Create cart failed"))

    assertThrows(UpstreamException::class.java) {
      service.createCart(
        CreateCartRequest(
          metadata = CartMetadata(
            prisonId = PRISONER_ID,
            offenderNo = PRISONER_NUMBER,
            firstName = "John",
            secondName = "Doe",
          ),
        ),
      )
    }
  }
}
