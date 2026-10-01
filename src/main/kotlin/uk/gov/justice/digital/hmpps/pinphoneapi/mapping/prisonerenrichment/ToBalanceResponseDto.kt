package uk.gov.justice.digital.hmpps.pinphoneapi.mapping.prisonerenrichment

import uk.gov.justice.digital.hmpps.pinphoneapi.client.prisonfinance.generated.Account
import java.math.BigDecimal
import java.math.RoundingMode

fun Account.toBalanceResponseDto() = BalanceResponseDto(
  spendsPence = spends.toPence(),
  cashPence = cash.toPence(),
  savingsPence = savings.toPence(),
  damageObligationsPence = damageObligations.toPence(),
  currency = currency,
)

@Suppress("MagicNumber")
private fun BigDecimal.toPence(): Long = this
  .multiply(BigDecimal(100))
  .setScale(0, RoundingMode.HALF_UP)
  .longValueExact()

data class BalanceResponseDto(
  val spendsPence: Long,
  val cashPence: Long,
  val savingsPence: Long,
  val damageObligationsPence: Long,
  val currency: String,
)
