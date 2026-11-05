package uk.gov.justice.digital.hmpps.pinphoneapi.model.sar

data class SarPinPhone(
  val prisonCode: String,
  val orderStatus: String,
  val orderCreatedAt: String,
  val orderUpdatedAt: String? = null,
  val amountInPounds: String,
  val rawAmount: String,
  val paymentStatus: String,
  val paymentAuthorizedAt: String,
  val paymentCreatedAt: String,
  val paymentUpdatedAt: String? = null,
  val amountInPence: String,
  val paymentOutcome: String,
  val btCreditLimit: String,
  val btBalanceBeforeTransaction: String,
  val btBalanceAfterTransaction: String,
  val errorCode: String? = null,
  val errorDescription: String? = null,
)