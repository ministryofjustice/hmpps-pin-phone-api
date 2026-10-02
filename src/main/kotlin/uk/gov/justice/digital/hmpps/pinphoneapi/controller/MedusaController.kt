package uk.gov.justice.digital.hmpps.pinphoneapi.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.pinphoneapi.client.medusaapiclient.generated.AddItemsRequest
import uk.gov.justice.digital.hmpps.pinphoneapi.client.medusaapiclient.generated.CartResponse
import uk.gov.justice.digital.hmpps.pinphoneapi.client.medusaclient.MedusaInternalClient

@RestController
@PreAuthorize("hasRole('ROLE_PIN_PHONE_CREDIT_API')")
@RequestMapping("/api")
@Tag(
  name = "Pin Phone",
  description = "Pin Phone medusa endpoints",
)
class MedusaController(private val medusaStoreClient: MedusaInternalClient) {

  @Operation(summary = "Add pin phone product to Medusa cart")
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "Line item added successfully"),
      ApiResponse(responseCode = "400", description = "Invalid request"),
      ApiResponse(responseCode = "404", description = "Cart or variant not found"),
      ApiResponse(responseCode = "500", description = "Internal server error"),
    ],
  )
  @PostMapping("/add-line-item/{cartId}", produces = [MediaType.APPLICATION_JSON_VALUE])
  fun addLineItem(
    @PathVariable
    @Parameter(description = "The cart number", example = "cart_1234567890", required = true)
    cartId: String,
    @RequestBody addItemsRequest: AddItemsRequest,
  ): CartResponse = medusaStoreClient.addPinPhoneItemsToCart(addItemsRequest, cartId)
}
