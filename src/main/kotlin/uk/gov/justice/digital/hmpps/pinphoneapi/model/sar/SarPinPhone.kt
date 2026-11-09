package uk.gov.justice.digital.hmpps.pinphoneapi.model.sar

import java.time.LocalDateTime

data class SarPinPhone(
  val prisonCode: String?,
  val orderStatus: String?,
  val orderCreatedAt: LocalDateTime?,
  val orderUpdatedAt: LocalDateTime? = null,
  val amountInPounds: String?,
  val rawAmount: String?,
  val paymentStatus: String?,
  val paymentAuthorizedAt: LocalDateTime?,
  val paymentCreatedAt: LocalDateTime?,
  val paymentUpdatedAt: LocalDateTime? = null,
  val amountInPence: String?,
  val paymentOutcome: String?,
  val btCreditLimit: String?,
  val btBalanceBeforeTransaction: String?,
  val btBalanceAfterTransaction: String?,
  val errorCode: String? = null,
  val errorDescription: String? = null,
)
