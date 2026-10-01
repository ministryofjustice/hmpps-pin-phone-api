package uk.gov.justice.digital.hmpps.pinphoneapi.client.btPinPhoneClient

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientRequestException
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.pinphoneapi.client.WebClientErrorHandler
import uk.gov.justice.digital.hmpps.pinphoneapi.client.btPinPhoneClient.generated.AccountCreditRequest
import uk.gov.justice.digital.hmpps.pinphoneapi.client.btPinPhoneClient.generated.AccountCreditResponse
import uk.gov.justice.digital.hmpps.pinphoneapi.client.btPinPhoneClient.generated.BtPinPhoneBalanceRequest
import uk.gov.justice.digital.hmpps.pinphoneapi.client.btPinPhoneClient.generated.BtPinPhoneBalanceResponse
import uk.gov.justice.digital.hmpps.pinphoneapi.client.btPinPhoneClient.generated.BtPinPhoneControlledNumbersRequest
import uk.gov.justice.digital.hmpps.pinphoneapi.client.btPinPhoneClient.generated.BtPinPhoneControlledNumbersResponse
import uk.gov.justice.digital.hmpps.pinphoneapi.client.btPinPhoneClient.generated.BtTokenRequest
import uk.gov.justice.digital.hmpps.pinphoneapi.client.btPinPhoneClient.generated.BtTokenResponse
import uk.gov.justice.digital.hmpps.pinphoneapi.config.UpstreamException

@Component
class BtPinPhoneClient(
  @Qualifier("btPinPhoneWebClient") private val btPinPhoneWebClient: WebClient,
  @Value("\${bt.client.id}") private val clientId: String,
  @Value("\${bt.client.secret}") private val clientSecret: String,
  private val errorHandler: WebClientErrorHandler,
) {

  companion object {
    private val logger = LoggerFactory.getLogger(BtPinPhoneClient::class.java)
  }

  fun getBtToken(): Mono<BtTokenResponse> = btPinPhoneWebClient
    .post()
    .uri("/auth/token")
    .bodyValue(BtTokenRequest(clientId = clientId, clientSecret = clientSecret))
    .retrieve()
    .bodyToMono<BtTokenResponse>()
    .onErrorMap(WebClientResponseException::class.java) { ex ->
      val error = errorHandler.handleError(ex)
      logger.error("BT auth token request failed: ${error.userMessage}")
      UpstreamException(error.userMessage ?: "Auth token request failed")
    }
    .onErrorMap(WebClientRequestException::class.java) { ex ->
      logger.error("Get BT token has failed due to connection issue", ex)
      UpstreamException("BT service is currently unavailable")
    }

  fun getPrisonerBalance(btPinPhoneBalanceRequest: BtPinPhoneBalanceRequest): Mono<BtPinPhoneBalanceResponse> = getBtToken().flatMap { btAuthResponse ->
    btPinPhoneWebClient
      .post()
      .uri("/pcs/Balance")
      .headers { it.setBearerAuth(btAuthResponse.accessToken) }
      .bodyValue(btPinPhoneBalanceRequest)
      .retrieve()
      .bodyToMono<BtPinPhoneBalanceResponse>()
      .onErrorMap(WebClientResponseException::class.java) { ex ->
        val error = errorHandler.handleError(ex)
        logger.error("BT balance request failed for prisoner ${btPinPhoneBalanceRequest.prisonerId}: ${ex.responseBodyAsString}")
        UpstreamException(error.userMessage ?: "Balance request failed")
      }
  }

  fun getPrisonerContacts(btPinPhoneControlledNumbersRequest: BtPinPhoneControlledNumbersRequest): Mono<BtPinPhoneControlledNumbersResponse> = getBtToken().flatMap { btAuthResponse ->
    btPinPhoneWebClient
      .post()
      .uri("/pcs/ControlledNumbers")
      .headers { it.setBearerAuth(btAuthResponse.accessToken) }
      .bodyValue(btPinPhoneControlledNumbersRequest)
      .retrieve()
      .bodyToMono<BtPinPhoneControlledNumbersResponse>()
      .onErrorMap(WebClientResponseException::class.java) { ex ->
        val error = errorHandler.handleError(ex)
        logger.error("BT contacts request failed for prisoner ${btPinPhoneControlledNumbersRequest.prisonerId}: ${ex.responseBodyAsString}")
        UpstreamException(error.userMessage ?: "Contacts request failed")
      }
  }

  fun addCredit(accountCreditRequest: AccountCreditRequest): Mono<AccountCreditResponse> = getBtToken().flatMap { btAuthResponse ->
    btPinPhoneWebClient
      .post()
      .uri("/pcs/AccountCredit")
      .headers { it.setBearerAuth(btAuthResponse.accessToken) }
      .bodyValue(accountCreditRequest)
      .retrieve()
      .bodyToMono(AccountCreditResponse::class.java)
      .onErrorMap(WebClientResponseException::class.java) { ex ->
        val error = errorHandler.handleError(ex)
        logger.error("BT add credit request failed for prisoner ${accountCreditRequest.prisonerId}: ${ex.responseBodyAsString}")
        UpstreamException(error.userMessage ?: "Add credit failed")
      }
  }
}
