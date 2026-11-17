package uk.gov.justice.digital.hmpps.pinphoneapi.mapping.sar

import uk.gov.justice.digital.hmpps.pinphoneapi.client.medusaapiclient.generated.SarRequestContent
import uk.gov.justice.digital.hmpps.pinphoneapi.model.sar.SarPinPhone
import java.math.BigDecimal
import java.time.format.DateTimeFormatter

val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

fun SarRequestContent.toSarPinPhone() = SarPinPhone(
  prisonCode = this.prisonCode,
  orderStatus = this.orderStatus?.value,
  orderCreatedAt = formatter.format(this.orderCreatedAt),
  orderUpdatedAt = this.orderUpdatedAt?.format(formatter),
  amountInPounds = this.amountInPounds,
  rawAmount = this.rawAmount,
  paymentStatus = this.paymentStatus?.value,
  paymentAuthorizedAt = formatter.format(this.paymentAuthorizedAt),
  paymentCreatedAt = formatter.format(this.paymentCreatedAt),
  paymentUpdatedAt = this.paymentUpdatedAt?.format(formatter),
  amountInPence = this.amountInPence,
  paymentOutcome = this.paymentOutcome?.value,
  btCreditLimit = this.btCreditLimit,
  btBalanceBeforeTransaction = this.btBalanceBeforeTransaction?.toBigDecimalOrNull()?.divide(BigDecimal("100"))?.toPlainString() ?: "0.00",
  btBalanceAfterTransaction = this.btBalanceAfterTransaction?.toBigDecimalOrNull()?.divide(BigDecimal("100"))?.toPlainString() ?: "0.00",
  errorCode = this.errorCode,
  errorDescription = this.errorDescription,
)

fun List<SarRequestContent>.toSarPinPhoneList(): List<SarPinPhone> = this.map { it.toSarPinPhone() }
