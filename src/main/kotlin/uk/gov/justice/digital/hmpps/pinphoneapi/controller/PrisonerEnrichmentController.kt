package uk.gov.justice.digital.hmpps.pinphoneapi.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.pinphoneapi.service.pinphoneenrichment.PinPhonePrisonerEnrichmentService

@Tag(
  name = "Prisoner Enrichment",
  description = "Prisoner enrichment API",
)
@RestController
@Validated
@PreAuthorize("hasRole('ROLE_PIN_PHONE_CREDIT_API')")
@RequestMapping(value = ["/api"], produces = [MediaType.APPLICATION_JSON_VALUE])
class PrisonerEnrichmentController(
  private val pinPhonePrisonerEnrichmentService: PinPhonePrisonerEnrichmentService,
) {
  @Operation(
    summary = "Enriches prisoner with information from BT, and prison API for balance information",
    responses = [
      ApiResponse(responseCode = "200", description = "Enriched prisoner details returned successfully"),
      ApiResponse(responseCode = "404", description = "Prisoner not found"),
      ApiResponse(responseCode = "400", description = "Bad request or upstream error"),
      ApiResponse(responseCode = "500", description = "Internal server error"),
    ],
  )
  @GetMapping("/prisoner-enrichment/{prisonerNumber}")
  fun getPrisonerEnrichment(
    @PathVariable
    @Parameter(description = "The prisoner number", example = "A1234BC", required = true)
    prisonerNumber: String,
  ) = pinPhonePrisonerEnrichmentService.getEnrichedPrisoner(prisonerNumber)
}
