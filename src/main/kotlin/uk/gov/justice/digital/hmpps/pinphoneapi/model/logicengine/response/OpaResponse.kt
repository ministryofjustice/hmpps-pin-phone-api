package uk.gov.justice.digital.hmpps.pinphoneapi.model.logicengine.response

data class OpaResponse(
  val result: Result,
)

data class Result(
  val hidden: Boolean,
  val decision: String,
  val provider: String? = null,
  val productId: String? = null,
  val prisonId: String? = null,

  val creditLimitEnabled: Boolean = false,
  val creditLimit: Int = 0,
  val currentBalance: Int = 0,
  val maxAvailableCredit: Int = 0,
  val accountSource: List<String> = emptyList(),
  val warnings: List<String> = emptyList(),
  val errors: List<String> = emptyList(),
)
