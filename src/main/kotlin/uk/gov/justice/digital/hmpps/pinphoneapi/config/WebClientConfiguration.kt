package uk.gov.justice.digital.hmpps.pinphoneapi.config

import io.netty.handler.ssl.SslContextBuilder
import io.netty.handler.ssl.util.InsecureTrustManagerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient
import uk.gov.justice.hmpps.kotlin.auth.authorisedWebClient
import uk.gov.justice.hmpps.kotlin.auth.healthWebClient
import java.time.Duration

@Configuration
class WebClientConfiguration(
  @param:Value("\${api.hmpps-auth.base-url}") private val hmppsAuthBaseUri: String,
  @param:Value("\${api.hmpps-auth.timeout-ms:20s}") private val hmppsAuthHealthTimeout: Duration,

  @param:Value("\${api.health-and-medication.base-url}") val healthAndMedicationBaseUri: String,
  @param:Value("\${api.health-and-medication.timeout-ms:20s}") val healthAndMedicationTimeout: Duration,

  @param:Value("\${api.prisoner-search.base-url}") val prisonerSearchBaseUri: String,
  @param:Value("\${api.prisoner-search.timeout-ms:20s}") val prisonerSearchTimeout: Duration,

  @param:Value("\${api.prisoner-adjudications.base-url}") val prisonerAdjudicationsBaseUri: String,
  @param:Value("\${api.prisoner-adjudications.timeout-ms:20s}") val prisonerAdjudicationsTimeout: Duration,

  @param:Value("\${api.prisoner-incentives.base-url}") val prisonerIncentivesBaseUri: String,
  @param:Value("\${api.prisoner-incentives.timeout-ms:20s}") val prisonerIncentivesTimeout: Duration,

  @param:Value("\${api.open-food-facts.base-url}") val openFoodFactsBaseUri: String,
  @param:Value("\${api.open-food-facts.timeout-ms:20s}") val openFoodFactsTimeout: Duration,

  @param:Value("\${api.open-products-facts.base-url}") val openProductsFactsBaseUri: String,
  @param:Value("\${api.open-products-facts.timeout-ms:20s}") val openProductsFactsTimeout: Duration,

  @param:Value("\${api.medusa.base-url}") val medusaBaseUri: String,
  @param:Value("\${api.medusa.timeout-ms:20s}") val medusaTimeout: Duration,

  @param:Value("\${api.prison-api.base-url}") val prisonApiBaseUri: String,
  @param:Value("\${api.prison-api.timeout-ms:20s}") val prisonApiTimeout: Duration,

  @param:Value("\${api.bt.base-url}") val btPinPhoneBaseUri: String,

  @param:Value("\${opa.url}") val opaBaseUri: String,

  private val builder: WebClient.Builder,
) {
  @Bean
  @Suppress("MaxLineLength")
  fun hmppsAuthHealthWebClient(builder: WebClient.Builder): WebClient = builder.healthWebClient(hmppsAuthBaseUri, hmppsAuthHealthTimeout)

  // todo: consider removing, not required for pin phone, but will be for canteen
  @Bean
  @Suppress("MaxLineLength")
  fun prisonerHealthAndMedicationWebClient(authorizedClientManager: OAuth2AuthorizedClientManager) = builder.authorisedWebClient(
    authorizedClientManager,
    "hmpps-pin-phone-api",
    healthAndMedicationBaseUri,
    healthAndMedicationTimeout,
  )

  @Bean
  @Suppress("MaxLineLength")
  fun prisonerSearchWebClient(authorizedClientManager: OAuth2AuthorizedClientManager) = builder.authorisedWebClient(
    authorizedClientManager,
    "hmpps-pin-phone-api",
    prisonerSearchBaseUri,
    prisonerSearchTimeout,
  )

  @Bean
  @Suppress("MaxLineLength")
  fun prisonerAdjudicationsWebClient(authorizedClientManager: OAuth2AuthorizedClientManager) = builder.authorisedWebClient(
    authorizedClientManager,
    "hmpps-pin-phone-api",
    prisonerAdjudicationsBaseUri,
    prisonerAdjudicationsTimeout,
  )

  // todo: consider removing if we decide to rely on prisoner search for incentive info
  @Bean
  @Suppress("MaxLineLength")
  fun prisonerIncentivesWebClient(authorizedClientManager: OAuth2AuthorizedClientManager) = builder.authorisedWebClient(
    authorizedClientManager,
    "hmpps-pin-phone-api",
    prisonerIncentivesBaseUri,
    prisonerIncentivesTimeout,
  )

  @Bean
  @Suppress("MaxLineLength")
  fun medusaInternalWebClient(authorizedClientManager: OAuth2AuthorizedClientManager) = builder.authorisedWebClient(
    authorizedClientManager,
    "hmpps-pin-phone-api",
    medusaBaseUri,
    medusaTimeout,
  )

  @Bean
  @Suppress("MaxLineLength")
  fun medusaAdminWebClient(builder: WebClient.Builder): WebClient = builder.baseUrl(medusaBaseUri).build()

  @Bean
  fun openFoodFactsWebClient(builder: WebClient.Builder): WebClient = builder.baseUrl(openFoodFactsBaseUri).build()

  @Bean
  @Suppress("MaxLineLength")
  fun openProductsFactsWebClient(builder: WebClient.Builder): WebClient = builder.baseUrl(openProductsFactsBaseUri).build()

  @Bean
  @Suppress("MaxLineLength")
  fun prisonApiWebClient(authorizedClientManager: OAuth2AuthorizedClientManager) = builder.authorisedWebClient(
    authorizedClientManager,
    "hmpps-pin-phone-api",
    prisonApiBaseUri,
    prisonApiTimeout,
  )

  @Bean
  fun opaWebClient(builder: WebClient.Builder) = builder
    .baseUrl(opaBaseUri)
    .build()

  // todo: remove insecure cert (bt is currently on QA environment)
  @Bean
  fun btPinPhoneWebClient(builder: WebClient.Builder): WebClient {
    val sslContext = SslContextBuilder.forClient()
      .trustManager(InsecureTrustManagerFactory.INSTANCE)
      .build()
    val httpClient = HttpClient.create()
      .secure { it.sslContext(sslContext) }
    return builder
      .baseUrl(btPinPhoneBaseUri)
      .clientConnector(ReactorClientHttpConnector(httpClient))
      .build()
  }
}
