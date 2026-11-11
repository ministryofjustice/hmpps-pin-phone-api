package uk.gov.justice.digital.hmpps.pinphoneapi.mapping.sar

import uk.gov.justice.digital.hmpps.pinphoneapi.client.medusaapiclient.generated.SarRequestContent
import uk.gov.justice.digital.hmpps.pinphoneapi.model.sar.SarPinPhone

fun SarRequestContent.toSarPinPhone() = SarPinPhone(
  prisonCode = this.prisonCode,
  orderStatus = this.orderStatus?.value,
  orderCreatedAt = this.orderCreatedAt,
  orderUpdatedAt = this.orderUpdatedAt,
  amountInPounds = this.amountInPounds,
  rawAmount = this.rawAmount,
  paymentStatus = this.paymentStatus?.value,
  paymentAuthorizedAt = this.paymentAuthorizedAt,
  paymentCreatedAt = this.paymentCreatedAt,
  paymentUpdatedAt = this.paymentUpdatedAt,
  amountInPence = this.amountInPence,
  paymentOutcome = this.paymentOutcome?.value,
  btCreditLimit = this.btCreditLimit,
  btBalanceBeforeTransaction = this.btBalanceBeforeTransaction,
  btBalanceAfterTransaction = this.btBalanceAfterTransaction,
  errorCode = this.errorCode,
  errorDescription = this.errorDescription,
)

fun List<SarRequestContent>.toSarPinPhoneList(): List<SarPinPhone> = this.map { it.toSarPinPhone() }
