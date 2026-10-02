package uk.gov.justice.digital.hmpps.pinphoneapi.mapping.prisonerenrichment

import uk.gov.justice.digital.hmpps.pinphoneapi.client.prisonerSearch.generated.Prisoner
import java.time.LocalDate

fun Prisoner.toPrisonerSearchResponseDto() = PrisonerSearchResponseDto(
  prisonerNumber = prisonerNumber,
  prisonId = prisonId,
  prisonName = prisonName,
  bookNumber = bookNumber,
  bookingId = bookingId,
  dateOfBirth = dateOfBirth,
  youthOffender = youthOffender,
  gender = gender,
)

data class PrisonerSearchResponseDto(
  val prisonerNumber: String,
  val prisonId: String?,
  val prisonName: String?,
  val bookNumber: String?,
  val bookingId: String?,
  val dateOfBirth: LocalDate?,
  val youthOffender: Boolean?,
  val gender: String?,
)
