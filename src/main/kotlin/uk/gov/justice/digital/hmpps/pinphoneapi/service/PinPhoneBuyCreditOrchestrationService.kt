package uk.gov.justice.digital.hmpps.pinphoneapi.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.pinphoneapi.client.btPinPhoneClient.BtPinPhoneClient
import uk.gov.justice.digital.hmpps.pinphoneapi.client.btPinPhoneClient.generated.AccountCreditRequest
import uk.gov.justice.digital.hmpps.pinphoneapi.client.btPinPhoneClient.generated.AccountCreditResponse
import uk.gov.justice.digital.hmpps.pinphoneapi.client.medusaapiclient.generated.CartResponse
import uk.gov.justice.digital.hmpps.pinphoneapi.client.medusaapiclient.generated.CompleteCartResponse
import uk.gov.justice.digital.hmpps.pinphoneapi.client.medusaapiclient.generated.CreateCartRequest
import uk.gov.justice.digital.hmpps.pinphoneapi.client.medusaapiclient.generated.PaymentRequest
import uk.gov.justice.digital.hmpps.pinphoneapi.client.medusaclient.MedusaInternalClient
import uk.gov.justice.digital.hmpps.pinphoneapi.client.prisonfinance.generated.ReleaseHoldAndCreateTransaction
import uk.gov.justice.digital.hmpps.pinphoneapi.config.UpstreamException
import java.util.UUID

@Service
class PinPhoneBuyCreditOrchestrationService(
  private val financeService: PrisonFinanceService,
  private val medusaStoreClient: MedusaInternalClient,
  private val btPinPhoneClient: BtPinPhoneClient,
) {
  companion object {
    private const val MAX_BT_ATTEMPTS = 3
    private val log: Logger = LoggerFactory.getLogger("PinPhoneBuyCreditOrchestrationService")
  }

  /**
   * Orchestrates the full PIN phone credit purchase flow:
   * 1. Places a financial hold on the prisoner's account
   * 2. Adds credit via the BT API (with retry)
   * 3. Releases the hold and creates the debit transaction
   * 4. Records the outcome in Medusa for audit
   */
  fun processCheckout(paymentRequest: PaymentRequest, cartId: String): CompleteCartResponse {
    val offenderNo = requireNotNull(paymentRequest.offenderNo) { "offenderNo must not be null" }
    val amountPence = requireNotNull(paymentRequest.amountPence) { "amountPence must not be null" }
    val prisonId = requireNotNull(paymentRequest.prisonId) { "prisonId must not be null" }

    log.info("Starting checkout for cart {} prisoner {} amountPence {}", cartId, offenderNo, amountPence)

    /**
     * Attempts to add hold, if fails call medusa so failure is captured.
     */
    val holdResponse = try {
      financeService.addHold(prisonId, offenderNo, amountPence)
    } catch (e: UpstreamException) {
      log.error("Failed to place hold for cart {} prisoner {}: {}", cartId, offenderNo, e.message)
      val medusaRequest = PaymentRequest(
        amountPence = amountPence,
        offenderNo = offenderNo,
        prisonId = prisonId,
        paymentStatus = PaymentRequest.PaymentStatus.ERROR,
        errorCode = PaymentRequest.ErrorCode.HOLD_FAILED,
        errorMessage = e.message,
      )
      return recordInMedusa(cartId, offenderNo, medusaRequest, paymentSuccessful = false)
    }

    log.info("Hold placed for prisoner {} holdNumber {}", offenderNo, holdResponse.holdNumber)

    val btResponse = try {
      callBtApiWithRetry(offenderNo, amountPence.toInt())
    } catch (e: UpstreamException) {
      log.error("BT payment failed for cart {} prisoner {}: {}", cartId, offenderNo, e.message)
      val errorCode = PaymentRequest.ErrorCode.BT_PAYMENT_FAILED
      return handleCheckoutError(prisonId, offenderNo, holdResponse.holdNumber, cartId, amountPence, errorCode, e.message)
    }

    val transactionResponse = try {
      financeService.releaseHoldAndCreateTransaction(
        prisonId,
        offenderNo,
        holdResponse.holdNumber,
        ReleaseHoldAndCreateTransaction.Type.PHONE,
      )
    } catch (e: UpstreamException) {
      log.error("Release hold/transaction failed for cart {} prisoner {}: {}", cartId, offenderNo, e.message)
      val errorcode = PaymentRequest.ErrorCode.RELEASE_HOLD_CREATE_TRANSACTION_FAILED
      return handleCheckoutError(prisonId, offenderNo, holdResponse.holdNumber, cartId, amountPence, errorcode, e.message)
    }

    log.info("Transaction created for prisoner {} transactionId {}", offenderNo, transactionResponse.id)

    val medusaRequest = PaymentRequest(
      amountPence = paymentRequest.amountPence,
      offenderNo = paymentRequest.offenderNo,
      prisonId = paymentRequest.prisonId,
      paymentStatus = PaymentRequest.PaymentStatus.AUTHORIZED,
      financeTransactionReference = transactionResponse.id,
      financeHoldNumber = holdResponse.holdNumber,
      btCreditReference = btResponse.reference,
      btCreditLimitPence = btResponse.creditLimitPence,
      btPreBalancePence = btResponse.preBalancePence,
      btNewBalancePence = btResponse.newBalancePence,
    )
    return recordInMedusa(cartId, offenderNo, medusaRequest, paymentSuccessful = true)
  }

  /**
   * Attempts to add credit via the BT API, retrying up to 3 times.
   */
  private fun callBtApiWithRetry(offenderNo: String, amountPence: Int): AccountCreditResponse {
    var lastException: Exception? = null
    for (attempt in 1..MAX_BT_ATTEMPTS) {
      try {
        val reference = UUID.randomUUID().toString().replace("-", "").take(20)
        val request = AccountCreditRequest(
          reference = reference,
          prisonerId = offenderNo,
          amountPence = amountPence,
        )
        val response = btPinPhoneClient.addCredit(request).block()
          ?: throw RuntimeException("BT API returned null response")
        log.info("BT credit added for prisoner {} on attempt {}", offenderNo, attempt)
        return response
      } catch (e: UpstreamException) {
        log.warn("Failed to add credit to BT for prisoner {} on attempt {}: {}", offenderNo, attempt, e.message)
        lastException = e
      }
    }
    throw lastException ?: RuntimeException("BT API call failed after $MAX_BT_ATTEMPTS attempts")
  }

  /**
   * Handles a failed payment by releasing the financial hold and recording
   * the failure in Medusa.
   * Called when BT or the finance transaction fails
   */
  private fun handleCheckoutError(
    prisonId: String,
    offenderNo: String,
    holdNumber: Long,
    cartId: String,
    amountPence: Long,
    errorCode: PaymentRequest.ErrorCode,
    errorMessage: String?,
  ): CompleteCartResponse {
    try {
      financeService.releaseHold(prisonId, offenderNo, holdNumber)
      log.info("Hold released for prisoner {} holdNumber {}", offenderNo, holdNumber)
    } catch (e: UpstreamException) {
      log.error("Failed to release hold for prisoner {} holdNumber {}: {}", offenderNo, holdNumber, e.message)
      val request = PaymentRequest(
        amountPence = amountPence,
        offenderNo = offenderNo,
        prisonId = prisonId,
        paymentStatus = PaymentRequest.PaymentStatus.ERROR,
        financeHoldNumber = holdNumber,
        errorCode = PaymentRequest.ErrorCode.RELEASE_HOLD_FAILED,
        errorMessage = errorMessage,
      )

      return recordInMedusa(cartId, offenderNo, request, paymentSuccessful = false)
    }

    val request = PaymentRequest(
      amountPence = amountPence,
      offenderNo = offenderNo,
      prisonId = prisonId,
      paymentStatus = PaymentRequest.PaymentStatus.ERROR,
      financeHoldNumber = holdNumber,
      errorCode = errorCode,
      errorMessage = errorMessage,
    )

    return recordInMedusa(cartId, offenderNo, request, paymentSuccessful = false)
  }

  /**
   * Records the payment outcome in Medusa for audit purposes.
   * Called for both successful and failed payments
   *
   * If the Medusa itself fails, returns a response with
   * [CompleteCartResponse.orderStatusRecorded] = false
   * Payment outcome not impacted
   */
  private fun recordInMedusa(
    cartId: String,
    offenderNo: String,
    request: PaymentRequest,
    paymentSuccessful: Boolean,
  ): CompleteCartResponse = try {
    val cartResponse = medusaStoreClient.completeCart(cartId, request)
    log.info("Medusa recording complete for cart {} status {}", cartId, request.paymentStatus)
    CompleteCartResponse(
      paymentSuccessful = paymentSuccessful,
      orderStatusRecorded = true,
      orderId = if (paymentSuccessful) cartResponse.orderId else null,
      cartId = cartId,
    )
  } catch (e: Exception) {
    log.error(
      "Medusa recording failed for cart {} prisoner {} status {}: {}",
      cartId,
      offenderNo,
      request.paymentStatus,
      e.message,
    )
    CompleteCartResponse(
      paymentSuccessful = paymentSuccessful,
      orderStatusRecorded = false,
      orderId = null,
      cartId = cartId,
    )
  }

  fun createCart(request: CreateCartRequest): ResponseEntity<CartResponse> {
    log.info("Creating cart for offender {}", request.metadata.offenderNo)
    val response = medusaStoreClient.createCart(request)
    log.info("Successfully created cart {}", response.cart?.id)
    return ResponseEntity.ok(response)
  }
}
