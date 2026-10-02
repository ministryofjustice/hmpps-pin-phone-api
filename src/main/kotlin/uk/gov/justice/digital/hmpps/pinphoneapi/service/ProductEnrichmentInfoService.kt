package uk.gov.justice.digital.hmpps.pinphoneapi.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.pinphoneapi.client.productenrichment.OpenFoodFactsClient
import uk.gov.justice.digital.hmpps.pinphoneapi.client.productenrichment.OpenProductsFactsClient
import uk.gov.justice.digital.hmpps.pinphoneapi.client.productenrichment.dto.ProductDetailsResponse

/**
 * Service responsible for fetching and processing product enrichment information.
 *
 * This service interacts with external APIs, OpenFoodFacts and OpenProductsFacts, to retrieve
 * product details. Upon retrieval, it processes the responses to clean and format data
 * by removing unnecessary non-English tags and prefixes.
 */
@Service
class ProductEnrichmentInfoService(
  private val openFoodFactsClient: OpenFoodFactsClient,
  private val openProductsFactsClient: OpenProductsFactsClient,
) {
  private val log = LoggerFactory.getLogger(ProductEnrichmentInfoService::class.java)

  companion object {
    private const val MIN_TAG_LENGTH = 3
  }

  fun getProductEnrichmentDetails(
    ean: String,
  ): Mono<ProductDetailsResponse> = openFoodFactsClient.getProductDetails(ean)
    .map { cleanResponse(it) }
    .switchIfEmpty(
      openProductsFactsClient.getProductDetails(ean)
        .map { cleanResponse(it) },
    )

  private fun cleanResponse(response: ProductDetailsResponse): ProductDetailsResponse {
    val product = response.product ?: return response

    fun List<String>?.cleanTags() = this
      ?.map { it.trim() }
      ?.filter { it.startsWith("en:") && it.length > MIN_TAG_LENGTH }
      ?.map { it.removePrefix("en:") }

    fun String?.cleanAllergens() = this
      ?.split(",")
      ?.map { it.trim() }
      ?.filter { it.startsWith("en:") && it.length > MIN_TAG_LENGTH }
      ?.joinToString(",") { it.removePrefix("en:") }

    return response.copy(
      product = product.copy(
        ingredientsAnalysisTags = product.ingredientsAnalysisTags.cleanTags(),
        packagingTags = product.packagingTags.cleanTags(),
        categoriesTags = product.categoriesTags.cleanTags(),
        allergens = product.allergens.cleanAllergens(),
      ),
    )
  }
}
