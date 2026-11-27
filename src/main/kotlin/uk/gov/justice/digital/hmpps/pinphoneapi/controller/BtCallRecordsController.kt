package uk.gov.justice.digital.hmpps.pinphoneapi.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.pinphoneapi.service.callrecords.BtPinPhoneCallRecordsService
import java.time.LocalDateTime

@RestController
@Validated
@PreAuthorize("hasRole('ROLE_PIN_PHONE_CREDIT_API')")
@RequestMapping(
  value = ["/api"],
  produces = [MediaType.APPLICATION_JSON_VALUE],
)
class BtCallRecordsController(
  private val btPinPhoneCallRecordsService: BtPinPhoneCallRecordsService,
) {

  @Operation(
    summary = "Retrieves prisoner call records from BT",
    responses = [
      ApiResponse(responseCode = "200", description = "Prisoner call records returned successfully"),
      ApiResponse(responseCode = "404", description = "Prisoner not found"),
      ApiResponse(responseCode = "400", description = "Bad request or upstream error"),
      ApiResponse(responseCode = "500", description = "Internal server error"),
    ],
  )
  @GetMapping("/call-records/{prisonerNumber}")
  fun getCallRecords(
    @PathVariable
    @Parameter(
      description = "The prisoner number",
      example = "A1234BC",
      required = true,
    )
    prisonerNumber: String,
    @RequestParam
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @Parameter(
      description = "Start date for call history search",
      example = "2026-11-01T00:00:00",
      required = true,
    )
    startDate: LocalDateTime,
    @RequestParam
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @Parameter(
      description = "End date for call history search",
      example = "2026-11-30T23:59:59",
      required = true,
    )
    endDate: LocalDateTime,
  ) = btPinPhoneCallRecordsService.getCallRecords(
    prisonerNumber = prisonerNumber,
    startDate = startDate,
    endDate = endDate,
  )
}
