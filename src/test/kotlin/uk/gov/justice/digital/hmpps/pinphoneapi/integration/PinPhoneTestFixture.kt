package uk.gov.justice.digital.hmpps.pinphoneapi.integration

import uk.gov.justice.digital.hmpps.pinphoneapi.client.btPinPhoneClient.generated.BtPinPhoneBalanceResponse
import uk.gov.justice.digital.hmpps.pinphoneapi.client.btPinPhoneClient.generated.ControlledNumber
import uk.gov.justice.digital.hmpps.pinphoneapi.client.prisonerSearch.generated.CurrentIncentive
import uk.gov.justice.digital.hmpps.pinphoneapi.client.prisonerSearch.generated.IncentiveLevel
import uk.gov.justice.digital.hmpps.pinphoneapi.client.prisonerSearch.generated.Prisoner
import uk.gov.justice.digital.hmpps.pinphoneapi.client.prisoneradjudications.generated.ActivePunishmentDto
import uk.gov.justice.digital.hmpps.pinphoneapi.client.prisonfinance.generated.Account
import uk.gov.justice.digital.hmpps.pinphoneapi.mapping.prisonerenrichment.BalanceResponseDto
import uk.gov.justice.digital.hmpps.pinphoneapi.mapping.prisonerenrichment.BtPinPhoneResponseDto
import uk.gov.justice.digital.hmpps.pinphoneapi.mapping.prisonerenrichment.PrisonerSearchResponseDto
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.String

object PinPhoneTestFixture {
  const val PRISONER_NUMBER = "A1234BC"
  const val BOOKING_ID = "A1234BC"
  const val REFERENCE = "mark_FN"

  fun Prisoner(prisonerNumber: String = PRISONER_NUMBER) = Prisoner(
    prisonerNumber = prisonerNumber,
    prisonId = "MDI",
    prisonName = "Moorland (HMP & YOI)",
    bookNumber = "12345",
    bookingId = BOOKING_ID,
    dateOfBirth = LocalDate.of(1990, 1, 15),
    youthOffender = false,
    gender = "Female",
    currentIncentive = CurrentIncentive(
      level = IncentiveLevel(
        code = "STD",
        description = "Standard",
      ),
      dateTime = LocalDateTime.of(2020, 1, 1, 15, 0),
      nextReviewDate = LocalDate.of(2020, 2, 1),
    ),
  )

  fun prisonerSearchResponseDto(prisonerNumber: String = PRISONER_NUMBER) = PrisonerSearchResponseDto(
    prisonerNumber = prisonerNumber,
    prisonId = "MDI",
    prisonName = "Moorland (HMP & YOI)",
    bookNumber = "12345",
    bookingId = BOOKING_ID,
    dateOfBirth = LocalDate.of(1990, 1, 15),
    youthOffender = false,
    gender = "Female",
  )

  fun activePunishments() = listOf(
    ActivePunishmentDto(
      chargeNumber = "12345",
      punishmentType = ActivePunishmentDto.PunishmentType.PRIVILEGE,
      privilegeType = ActivePunishmentDto.PrivilegeType.CANTEEN,
      otherPrivilege = "none",
      duration = 5,
      measurement = ActivePunishmentDto.Measurement.DAYS,
      startDate = LocalDate.parse("2025-01-01"),
      lastDay = LocalDate.parse("2025-01-31"),
      amount = 0.1,
      stoppagePercentage = 0,
      activatedFrom = "today",
    ),
  )

  fun balanceDto() = Account(
    spends = BigDecimal(88.88),
    cash = BigDecimal(0.0),
    savings = BigDecimal(1.25),
    damageObligations = BigDecimal(0.0),
    currency = "GBP",
  )

  fun balanceResponseDto() = BalanceResponseDto(
    spendsPence = 8888,
    cashPence = 0,
    savingsPence = 125,
    damageObligationsPence = 0,
    currency = "GBP",
  )

  fun btPinPhoneDto(prisonerNumber: String = PRISONER_NUMBER) = BtPinPhoneBalanceResponse(
    reference = REFERENCE,
    prisonerId = prisonerNumber,
    balancePence = 1288,
    creditLimitPence = 5000,
  )

  fun btPinPhoneResponseDto(prisonerNumber: String = PRISONER_NUMBER) = BtPinPhoneResponseDto(
    reference = REFERENCE,
    prisonerId = prisonerNumber,
    balancePence = 1288,
    creditLimitPence = 5000,
    isFn = REFERENCE.endsWith("_FN"),
  )

  var contactList = listOf(
    ControlledNumber(
      id = 162439,
      name = "John Doe",
      phoneNumber = "07700900351",
      controlStatus = true,
      callAllowed = true,
      legal = true,
      allowMonitor = false,
      alert = true,
      override = true,
      relationshipId = 2,
    ),
    ControlledNumber(
      id = 162440,
      name = "Jane Smith",
      phoneNumber = "07700900352",
      controlStatus = true,
      callAllowed = true,
      legal = true,
      allowMonitor = false,
      alert = true,
      override = true,
      relationshipId = 1,
    ),
    ControlledNumber(
      id = 162441,
      name = "Robert Brown",
      phoneNumber = "07700900353",
      controlStatus = true,
      callAllowed = true,
      legal = false,
      allowMonitor = true,
      alert = false,
      override = false,
      relationshipId = 25,
    ),
    ControlledNumber(
      id = 162442,
      name = "Sarah Williams",
      phoneNumber = "07700900354",
      controlStatus = true,
      callAllowed = true,
      legal = true,
      allowMonitor = false,
      alert = true,
      override = true,
      relationshipId = 28,
    ),
    ControlledNumber(
      id = 162443,
      name = "Michael Jones",
      phoneNumber = "07700900355",
      controlStatus = true,
      callAllowed = false,
      legal = false,
      allowMonitor = true,
      alert = true,
      override = false,
      relationshipId = 5,
    ),
  )
}
