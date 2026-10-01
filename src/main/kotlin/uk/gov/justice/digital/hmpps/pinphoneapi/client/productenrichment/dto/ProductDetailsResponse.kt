package uk.gov.justice.digital.hmpps.pinphoneapi.client.productenrichment.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class ProductDetailsResponse(
  val code: String,
  val product: ProductEnrichmentInfo?,
  val status: Int,
  @JsonProperty("status_verbose")
  val statusVerbose: String,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ProductEnrichmentInfo(
  @JsonProperty("product_name")
  val productName: String?,
  val brands: String?,
  val quantity: String?,
  @JsonProperty("image_url")
  val imageUrl: String?,
  val allergens: String?,
  @JsonProperty("ingredients_analysis_tags")
  val ingredientsAnalysisTags: List<String>?,
  @JsonProperty("packaging_tags")
  val packagingTags: List<String>?,
  @JsonProperty("categories_tags")
  val categoriesTags: List<String>?,
  val nutriments: Nutriments?,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Nutriments(
  @JsonProperty("energy-kcal_100g")
  val energyKcal100g: Double?,
  @JsonProperty("fat_100g")
  val fat100g: Double?,
  @JsonProperty("saturated-fat_100g")
  val saturatedFat100g: Double?,
  @JsonProperty("carbohydrates_100g")
  val carbohydrates100g: Double?,
  @JsonProperty("sugars_100g")
  val sugars100g: Double?,
  @JsonProperty("proteins_100g")
  val proteins100g: Double?,
  @JsonProperty("salt_100g")
  val salt100g: Double?,
  @JsonProperty("sodium_100g")
  val sodium100g: String?,
)
