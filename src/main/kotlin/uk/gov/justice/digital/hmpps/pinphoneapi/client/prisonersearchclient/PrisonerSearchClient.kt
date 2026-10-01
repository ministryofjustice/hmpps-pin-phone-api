package uk.gov.justice.digital.hmpps.pinphoneapi.client.prisonersearchclient

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.pinphoneapi.client.WebClientErrorHandler
import uk.gov.justice.digital.hmpps.pinphoneapi.client.prisonerSearch.generated.Prisoner
import uk.gov.justice.digital.hmpps.pinphoneapi.config.UpstreamException

@Component
class PrisonerSearchClient(
  @Qualifier("prisonerSearchWebClient") private val prisonerSearchClient: WebClient,
  private val errorHandler: WebClientErrorHandler,
) {

  companion object {
    val logger: Logger = LoggerFactory.getLogger(PrisonerSearchClient::class.java)
  }

  fun getPrisoner(prisonerNumber: String): Mono<Prisoner> = prisonerSearchClient.get()
    .uri("/prisoner/{prisonerNumber}", prisonerNumber)
    .retrieve()
    .bodyToMono(Prisoner::class.java)
    .onErrorMap(WebClientResponseException::class.java) { ex ->
      val errorResponse = errorHandler.handleError(ex)
      logger.error("GET prisoner request failed for prisoner: $prisonerNumber", errorResponse)
      UpstreamException(errorResponse.userMessage ?: "getPrisoner request failed")
    }
}
