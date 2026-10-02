package uk.gov.justice.digital.hmpps.pinphoneapi.model.logicengine.request

data class OpaRequest(
  val input: Input,
)

data class Input(
  val productId: String,
  val currentBalance: Int? = null,
  val creditRequested: Int? = null,
  val creditLimit: Int? = null,
  val prisoner: Prisoner? = null,
  val incentives: Incentives? = null,
)

data class Prisoner(
  val prisonId: String,
  val isFN: Boolean = false,
  val isLG: Boolean = false,
)

data class Incentives(
  val iepCode: String,
)
