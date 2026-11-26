package uk.gov.justice.digital.hmpps.pinphoneapi.service

import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.pinphoneapi.client.prisonfinance.PrisonFinanceClient
import uk.gov.justice.digital.hmpps.pinphoneapi.client.prisonfinance.generated.AddHoldTransaction
import uk.gov.justice.digital.hmpps.pinphoneapi.client.prisonfinance.generated.CreateTransaction
import uk.gov.justice.digital.hmpps.pinphoneapi.client.prisonfinance.generated.HoldDetails
import uk.gov.justice.digital.hmpps.pinphoneapi.client.prisonfinance.generated.ReleaseHoldAndCreateTransaction
import uk.gov.justice.digital.hmpps.pinphoneapi.client.prisonfinance.generated.ReleaseHoldTransaction
import uk.gov.justice.digital.hmpps.pinphoneapi.client.prisonfinance.generated.Transaction
import java.util.UUID
import kotlin.String

/**
 * Service responsible for managing prison finance operations.
 */
@Service
class PrisonFinanceService(
  private val prisonFinanceClient: PrisonFinanceClient,
) {

  companion object {
    const val HOLD_DESCRIPTION = "HOLD"
    const val REMOVE_HOLD_DESCRIPTION = "Remove HOLD"
    const val CLIENT_REFERENCE_PREFIX = "CLIENT-"
    const val CLIENT_TRANSACTION_ID_LENGTH = 12
    const val HOLD = "HOLD"
    const val RELEASE = "RELEASE"
    const val REMOVE = "REMOVE"
    const val PHONE = "PHONE"

    // new
    const val ON_CREATE_X_CLIENT_NAME = "PHONE"
    const val ON_REFUND_X_CLIENT_NAME = "REFUND-PHONE"
    const val CREATE_PHONE_TRANSACTION_DESCRIPTION = "PHONE, Debiting %dp, for purchase of pin phone credit"
    const val REFUND_PHONE_TRANSACTION_DESCRIPTION = "PINS, Crediting %dp, for refund of purchase of pin phone credit, PHONE debit transaction ID: %s"
  }


  fun createTransactionPhone(prisonId: String, offenderNo: String, amountPence: Long, cartId: String): Transaction {
    val request = CreateTransaction(
      type = CreateTransaction.Type.PHONE,
      description = CREATE_PHONE_TRANSACTION_DESCRIPTION.format(amountPence),
      amount = amountPence,
      clientTransactionId = generateClientTransactionId(),
      clientUniqueRef = cartId
    )
    return prisonFinanceClient.createTransactionPhone(prisonId, offenderNo, ON_CREATE_X_CLIENT_NAME, request)
  }

  fun refundTransactionPhone(prisonId: String, offenderNo: String, amountPence: Long, cartId: String, transactionId: String): Transaction {
    val request = CreateTransaction(
      //todo: change to PINS refund type
      type = CreateTransaction.Type.REFND,
      description = REFUND_PHONE_TRANSACTION_DESCRIPTION.format(amountPence, transactionId),
      amount = amountPence,
      clientTransactionId = generateClientTransactionId(),
      clientUniqueRef = cartId
    )
    return prisonFinanceClient.createTransactionPhone(prisonId, offenderNo, ON_REFUND_X_CLIENT_NAME, request)
  }

  /**
   * Adds a hold to the prisoner.
   */
  fun addHold(prisonId: String, offenderNo: String, amount: Long, cartId: String): HoldDetails {
    val request = AddHoldTransaction(
      description = HOLD_DESCRIPTION,
      amount = amount,
      clientTransactionId = generateClientTransactionId(),
      clientName = offenderNo,
      clientUniqueReference = uniqueReference(HOLD, cartId),
    )

    return prisonFinanceClient.addHold(prisonId, offenderNo, request)
  }

  /**
   * Releases a hold.
   */
  fun releaseHold(prisonId: String, offenderNo: String, holdNumber: Number, cartId: String): ResponseEntity<Void> {
    val request = ReleaseHoldTransaction(
      description = REMOVE_HOLD_DESCRIPTION,
      clientTransactionId = generateClientTransactionId(),
      clientName = offenderNo,
      clientUniqueReference = uniqueReference(RELEASE, cartId),
    )

    return prisonFinanceClient.releaseHold(prisonId, offenderNo, holdNumber, request)
  }

  /**
   * Creates a transaction for the release of a hold.
   */
  fun releaseHoldAndCreateTransaction(
    prisonId: String,
    offenderNo: String,
    holdNumber: Number,
    clientRequestType: ReleaseHoldAndCreateTransaction.Type,
    cartId: String,
  ): Transaction {
    val request = ReleaseHoldAndCreateTransaction(
      type = clientRequestType,
      removeDescription = REMOVE_HOLD_DESCRIPTION,
      createDescription = "$HOLD_DESCRIPTION for $clientRequestType",
      clientTransactionId = generateClientTransactionId(),
      clientName = offenderNo,
      removeClientUniqueReference = uniqueReference(REMOVE, cartId),
      createClientUniqueReference = uniqueReference(PHONE, cartId),
    )

    return prisonFinanceClient.releaseHoldCreateTransaction(prisonId, offenderNo, holdNumber, request)
  }

  private fun generateClientReference(): String = UUID.randomUUID().toString()

  private fun String.toClientTransactionId(): String = take(CLIENT_TRANSACTION_ID_LENGTH)

  private fun String.toClientUniqueReference(): String = "$CLIENT_REFERENCE_PREFIX$this"

  private fun uniqueReference(operation: String, cartId: String): String = "$CLIENT_REFERENCE_PREFIX$operation-$cartId"

  private fun generateClientTransactionId(): String = UUID.randomUUID().toString().take(CLIENT_TRANSACTION_ID_LENGTH)
}
