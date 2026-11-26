package uk.gov.justice.digital.hmpps.pinphoneapi.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.pinphoneapi.client.btPinPhoneClient.BtPinPhoneClient
import uk.gov.justice.digital.hmpps.pinphoneapi.client.btPinPhoneClient.generated.AccountCreditRequest
import uk.gov.justice.digital.hmpps.pinphoneapi.client.btPinPhoneClient.generated.AccountCreditResponse
import uk.gov.justice.digital.hmpps.pinphoneapi.client.medusaapiclient.generated.CompleteCartResponse
import uk.gov.justice.digital.hmpps.pinphoneapi.client.medusaapiclient.generated.PaymentRequestV2
import uk.gov.justice.digital.hmpps.pinphoneapi.client.medusaclient.MedusaInternalClient
import uk.gov.justice.digital.hmpps.pinphoneapi.client.prisonfinance.DuplicateFinanceRequestException
import uk.gov.justice.digital.hmpps.pinphoneapi.config.UpstreamException

@Service
class PinPhoneBuyCreditOrchestrationServiceV2(
    private val financeService: PrisonFinanceService,
    private val medusaStoreClient: MedusaInternalClient,
    private val btPinPhoneClient: BtPinPhoneClient,
) {
  companion object {
    private const val MAX_BT_ATTEMPTS = 3
    private const val DUPLICATE_POLL_ATTEMPTS = 10
    private const val DUPLICATE_POLL_INTERVAL_MS = 200L
    private val log: Logger = LoggerFactory.getLogger("PinPhoneBuyCreditOrchestrationServiceV2")
  }

  fun processCheckout(paymentRequest: PaymentRequestV2, cartId: String): CompleteCartResponse {
    val offenderNo = requireNotNull(paymentRequest.offenderNo) { "offenderNo must not be null" }
    val amountPence = requireNotNull(paymentRequest.amountPence) { "amountPence must not be null" }
    val prisonId = requireNotNull(paymentRequest.prisonId) { "prisonId must not be null" }

    log.info("Starting checkout for cart {} prisoner {} amountPence {}", cartId, offenderNo, amountPence)

    val createTransactionPhone = try {
      financeService.createTransactionPhone(prisonId, offenderNo, amountPence, cartId)
    } catch (e: DuplicateFinanceRequestException) {
      return awaitOriginalCheckout(cartId, offenderNo)
    } catch (e: UpstreamException) {
      log.error("Failed to create transaction for cart {} prisoner {}: {}", cartId, offenderNo, e.message)
      val medusaRequest = PaymentRequestV2(
          amountPence = amountPence,
          offenderNo = offenderNo,
          prisonId = prisonId,
          paymentStatus = PaymentRequestV2.PaymentStatus.ERROR,
          errorCode = PaymentRequestV2.ErrorCode.PHONE_TRANSACTION_FAILED,
          errorMessage = e.message,
      )
      return recordInMedusa(cartId, offenderNo, medusaRequest, paymentSuccessful = false)
    }

    log.info("Transaction placed for prisoner: {} transaction ID: {}", offenderNo, createTransactionPhone.id)

    val btResponse = try {
      callBtApiWithRetry(offenderNo, amountPence.toInt(), cartId)
    } catch (e: UpstreamException) {
      log.error("BT payment failed for cart {} prisoner {}: {}", cartId, offenderNo, e.message)
      val errorCode = PaymentRequestV2.ErrorCode.BT_PAYMENT_FAILED
      return handleCheckoutError(createTransactionPhone.id, prisonId, offenderNo, cartId, amountPence, errorCode, e.message)
    }

    val medusaRequest = PaymentRequestV2(
      amountPence = amountPence,
      offenderNo = offenderNo,
      prisonId = prisonId,
      paymentStatus = PaymentRequestV2.PaymentStatus.AUTHORIZED,
      financeTransactionReference = createTransactionPhone.id,
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
  private fun callBtApiWithRetry(offenderNo: String, amountPence: Int, cartId: String): AccountCreditResponse {
    val reference = cartId.removePrefix("cart_").take(20)
    var lastException: Exception? = null
    for (attempt in 1..MAX_BT_ATTEMPTS) {
      try {
        val request = AccountCreditRequest(
            reference = reference,
            prisonerId = offenderNo,
            amountPence = amountPence,
        )
        val response = btPinPhoneClient.addCredit(request).block()
          ?: throw RuntimeException("BT API returned null response")
        log.info("BT credit added for prisoner: {} on attempt: {}, reference: {}", offenderNo, attempt, response.reference)
        return response
      } catch (e: UpstreamException) {
        log.warn("Failed to add credit to BT for prisoner {} on attempt {}: {}", offenderNo, attempt, e.message)
        lastException = e
      }
    }
    throw lastException ?: RuntimeException("BT API call failed after $MAX_BT_ATTEMPTS attempts")
  }

  /**
   * Handles a failed payment by refunding a transaction
   * Records the failure in Medusa.
   * Called when BT or the finance transaction fails
   */
  private fun handleCheckoutError(
    transactionId: String,
    prisonId: String,
    offenderNo: String,
    cartId: String,
    amountPence: Long,
    errorCode: PaymentRequestV2.ErrorCode,
    errorMessage: String?,
  ): CompleteCartResponse {
    val actualErrorCode = try {
      financeService.refundTransactionPhone(prisonId, offenderNo, amountPence, cartId, transactionId)
      log.info("Transaction refunded for prisoner {} transaction ID {}", offenderNo, transactionId)
      errorCode
    } catch (e: UpstreamException) {
      log.error("Failed to refund transaction for prisoner {} transaction ID {}: {}", offenderNo, transactionId, e.message)
      PaymentRequestV2.ErrorCode.PHONE_REFUND_TRANSACTION_FAILED
    }

    val request = PaymentRequestV2(
      amountPence = amountPence,
      offenderNo = offenderNo,
      prisonId = prisonId,
      paymentStatus = PaymentRequestV2.PaymentStatus.ERROR,
      financeTransactionReference = transactionId,
      errorCode = actualErrorCode,
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
      request: PaymentRequestV2,
      paymentSuccessful: Boolean,
  ): CompleteCartResponse = try {
    val cartResponse = medusaStoreClient.completeCartV2(cartId, request)
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

  /**
   * A transaction already exists on this cart,
   * wait for the original request to complete the cart
   * and return its outcome
   */
  private fun awaitOriginalCheckout(cartId: String, offenderNo: String): CompleteCartResponse {
    log.info("Duplicate checkout for cart {} prisoner {}: wait for original request", cartId, offenderNo)
    repeat(DUPLICATE_POLL_ATTEMPTS) {
      val cart = runCatching { medusaStoreClient.getCart(cartId) }.getOrNull()
      if (cart?.completedAt != null) {
        log.info("Original checkout for cart {} completed – returning its result", cartId)
        return CompleteCartResponse(
            paymentSuccessful = true,
            orderStatusRecorded = true,
            orderId = cart.orderId,
            cartId = cartId,
        )
      }
      Thread.sleep(DUPLICATE_POLL_INTERVAL_MS)
    }
    log.warn("Original checkout for cart {} prisoner {} not complete after polling", cartId, offenderNo)
    throw Exception("Original checkout for cart $cartId not complete after polling")
  }

}