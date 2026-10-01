package uk.gov.justice.digital.hmpps.pinphoneapi.client.productenrichment

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.pinphoneapi.client.productenrichment.dto.ProductDetailsResponse

/**
 * Abstract client for retrieving product enrichment information using a given WebClient instance.
 * Provides a reusable framework for making API requests to fetch product details based on an EAN code.
 * Subclasses need to implement specific client behavior by passing the appropriate WebClient instance and client name.
 */
abstract class ProductEnrichmentInfoClient(
  private val webClient: WebClient,
  private val clientName: String,
) {
  private val log = LoggerFactory.getLogger(this::class.java)

  fun getProductDetails(ean: String): Mono<ProductDetailsResponse> {
    return webClient.get()
      .uri("/api/v0/product/{ean}.json", ean)
      .retrieve()
      .bodyToMono(ProductDetailsResponse::class.java)
      .flatMap { response ->
        if (response.status == 0) {
          log.info(
            "Product not found (status 0) from {} for ean {}. status_verbose={}",
            clientName,
            ean,
            response.statusVerbose,
          )
          return@flatMap Mono.empty()
        }
        Mono.just(response)
      }.onErrorResume { e ->
        log.info("Error retrieving product from {} for ean {}: {}", clientName, ean, e.message)
        Mono.empty()
      }
  }
}

@Component
class OpenFoodFactsClient(
  openFoodFactsWebClient: WebClient,
) : ProductEnrichmentInfoClient(
  openFoodFactsWebClient,
  "OpenFoodFacts",
)

@Component
class OpenProductsFactsClient(
  openProductsFactsWebClient: WebClient,
) : ProductEnrichmentInfoClient(
  openProductsFactsWebClient,
  "OpenProductsFacts",
)
