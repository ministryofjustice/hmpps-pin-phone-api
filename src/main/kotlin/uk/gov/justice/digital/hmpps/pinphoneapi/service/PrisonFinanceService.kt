package uk.gov.justice.digital.hmpps.pinphoneapi.service

import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.pinphoneapi.client.prisonfinance.PrisonFinanceClient
import uk.gov.justice.digital.hmpps.pinphoneapi.client.prisonfinance.generated.AddHoldTransaction
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
  }

  /**
   * Adds a hold to the prisoner.
   */
  fun addHold(prisonId: String, offenderNo: String, amount: Long): HoldDetails {
    val clientReference = generateClientReference()
    val request = AddHoldTransaction(
      description = HOLD_DESCRIPTION,
      amount = amount,
      clientTransactionId = clientReference.toClientTransactionId(),
      clientName = offenderNo,
      clientUniqueReference = clientReference.toClientUniqueReference(),
    )

    return prisonFinanceClient.addHold(prisonId, offenderNo, request)
  }

  /**
   * Releases a hold.
   */
  fun releaseHold(prisonId: String, offenderNo: String, holdNumber: Number): ResponseEntity<Void> {
    val clientReference = generateClientReference()
    val request = ReleaseHoldTransaction(
      description = REMOVE_HOLD_DESCRIPTION,
      clientTransactionId = clientReference.toClientTransactionId(),
      clientName = offenderNo,
      clientUniqueReference = clientReference.toClientUniqueReference(),
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
  ): Transaction {
    val createClientReference = generateClientReference()
    val removeClientReference = generateClientReference()
    val request = ReleaseHoldAndCreateTransaction(
      type = clientRequestType,
      removeDescription = REMOVE_HOLD_DESCRIPTION,
      createDescription = "$HOLD_DESCRIPTION for $clientRequestType",
      clientTransactionId = createClientReference.toClientTransactionId(),
      clientName = offenderNo,
      removeClientUniqueReference = removeClientReference.toClientUniqueReference(),
      createClientUniqueReference = createClientReference.toClientUniqueReference(),
    )

    return prisonFinanceClient.releaseHoldCreateTransaction(prisonId, offenderNo, holdNumber, request)
  }

  private fun generateClientReference(): String = UUID.randomUUID().toString()

  private fun String.toClientTransactionId(): String = take(CLIENT_TRANSACTION_ID_LENGTH)

  private fun String.toClientUniqueReference(): String = "$CLIENT_REFERENCE_PREFIX$this"
}
