package uk.gov.justice.digital.hmpps.pinphoneapi.client.medusaclient

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientRequestException
import org.springframework.web.reactive.function.client.WebClientResponseException
import uk.gov.justice.digital.hmpps.pinphoneapi.client.WebClientErrorHandler
import uk.gov.justice.digital.hmpps.pinphoneapi.client.medusaapiclient.generated.AddItemsRequest
import uk.gov.justice.digital.hmpps.pinphoneapi.client.medusaapiclient.generated.CartResponse
import uk.gov.justice.digital.hmpps.pinphoneapi.client.medusaapiclient.generated.CompleteCartResponse
import uk.gov.justice.digital.hmpps.pinphoneapi.client.medusaapiclient.generated.CreateCartRequest
import uk.gov.justice.digital.hmpps.pinphoneapi.client.medusaapiclient.generated.PaymentRequest
import uk.gov.justice.digital.hmpps.pinphoneapi.config.UpstreamException
import kotlin.jvm.java

@Component
class MedusaInternalClient(
  @Qualifier("medusaInternalWebClient") private val medusaInternalClient: WebClient,
  private val errorHandler: WebClientErrorHandler,
) {

  companion object {
    val logger: Logger = LoggerFactory.getLogger(MedusaInternalClient::class.java)
  }

  fun createCart(createCartRequest: CreateCartRequest): CartResponse = medusaInternalClient
    .post()
    .uri("/internal/pin-phone/carts")
    .bodyValue(createCartRequest)
    .retrieve()
    .bodyToMono(CartResponse::class.java)
    .onErrorMap(WebClientResponseException::class.java) { ex ->
      if (ex.statusCode.is5xxServerError) {
        UpstreamException("Medusa service is currently unavailable")
      } else {
        val errorResponse = errorHandler.handleError(ex)
        logger.error("Create cart failed: ${ex.responseBodyAsString}")
        UpstreamException(errorResponse.userMessage ?: "Create cart failed")
      }
    }
    .onErrorMap(WebClientRequestException::class.java) { ex ->
      logger.error("Create cart failed due to connection issue", ex)
      UpstreamException("Medusa service is currently unavailable")
    }
    .block()!!

  fun addPinPhoneItemsToCart(addItemsRequest: AddItemsRequest, cartId: String): CartResponse = medusaInternalClient
    .post()
    .uri("/internal/pin-phone/carts/$cartId/add-items")
    .bodyValue(addItemsRequest)
    .retrieve()
    .bodyToMono(CartResponse::class.java)
    .onErrorMap(WebClientResponseException::class.java) { ex ->
      if (ex.statusCode.is5xxServerError) {
        UpstreamException("Medusa service is currently unavailable")
      } else {
        val errorResponse = errorHandler.handleError(ex)
        logger.error("Add line item request failed: ${ex.responseBodyAsString}")
        UpstreamException(errorResponse.userMessage ?: "Add line item request failed")
      }
    }
    .onErrorMap(WebClientRequestException::class.java) { ex ->
      logger.error("Add line item failed due to connection issue", ex)
      UpstreamException("Medusa service is currently unavailable")
    }
    .block()!!

  fun completeCart(cartId: String, paymentRequest: PaymentRequest): CompleteCartResponse = medusaInternalClient
    .post()
    .uri("/internal/pin-phone/carts/$cartId/complete")
    .bodyValue(mapOf("PaymentRequest" to paymentRequest))
    .retrieve()
    .bodyToMono(CompleteCartResponse::class.java)
    .onErrorMap(WebClientResponseException::class.java) { ex ->
      if (ex.statusCode.is5xxServerError) {
        UpstreamException("Medusa service is currently unavailable")
      } else {
        val errorResponse = errorHandler.handleError(ex)
        logger.error("Cart completion failed: ${ex.responseBodyAsString}")
        UpstreamException(errorResponse.userMessage ?: "Cart completion failed")
      }
    }
    .onErrorMap(WebClientRequestException::class.java) { ex ->
      logger.error("Cart completion failed due to connection issue", ex)
      UpstreamException("Medusa service is currently unavailable")
    }
    .block()!!
}
