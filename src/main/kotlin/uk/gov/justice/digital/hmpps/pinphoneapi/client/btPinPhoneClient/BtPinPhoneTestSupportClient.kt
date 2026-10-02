package uk.gov.justice.digital.hmpps.pinphoneapi.client.btPinPhoneClient

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.pinphoneapi.client.btPinPhoneClient.generated.BtPinPhoneBalanceRequest
import uk.gov.justice.digital.hmpps.pinphoneapi.client.btPinPhoneClient.generated.BtPinPhoneBalanceResponse
import uk.gov.justice.digital.hmpps.pinphoneapi.client.btPinPhoneClient.generated.BtTokenRequest
import uk.gov.justice.digital.hmpps.pinphoneapi.client.btPinPhoneClient.generated.BtTokenResponse

@Component
class BtPinPhoneTestSupportClient(
  @Qualifier("btPinPhoneWebClient") private val btPinPhoneWebClient: WebClient,
  @Value("\${bt.client.id}") private val clientId: String,
  @Value("\${bt.client.secret}") private val clientSecret: String,
) {

  companion object {
    val logger: Logger = LoggerFactory.getLogger(BtPinPhoneTestSupportClient::class.java)
  }

  fun getBtToken(): Mono<String> = btPinPhoneWebClient
    .post()
    .uri("/auth/token")
    .bodyValue(BtTokenRequest(clientId = clientId, clientSecret = clientSecret))
    .retrieve()
    .bodyToMono(BtTokenResponse::class.java)
    .map { it.accessToken }

  fun getPrisonerBalance(btPinPhoneBalanceRequest: BtPinPhoneBalanceRequest): Mono<BtPinPhoneBalanceResponse> = getBtToken().flatMap { token ->
    btPinPhoneWebClient
      .post()
      .uri("/pcs/Balance")
      .headers { it.setBearerAuth(token) }
      .bodyValue(btPinPhoneBalanceRequest)
      .retrieve()
      .bodyToMono<BtPinPhoneBalanceResponse>()
      .onErrorResume(WebClientResponseException::class.java) { ex ->
        logger.error("BT response [${ex.statusCode}]: ${ex.responseBodyAsString}")
        Mono.error(ex)
      }
  }

  fun createAccount(request: CreateAccountRequest): Mono<CreateAccountResponse> = getBtToken().flatMap { token ->
    btPinPhoneWebClient
      .post()
      .uri("/pcs/Account")
      .headers { it.setBearerAuth(token) }
      .bodyValue(request)
      .retrieve()
      .bodyToMono(CreateAccountResponse::class.java)
      .onErrorResume(WebClientResponseException::class.java) { ex ->
        logger.error("BT response [${ex.statusCode}]: ${ex.responseBodyAsString}")
        Mono.error(ex)
      }
  }

  fun createControlledNumber(request: CreateControlledNumberRequest): Mono<CreateControlledNumberResponse> = getBtToken().flatMap { token ->
    btPinPhoneWebClient
      .post()
      .uri("/pcs/ControlledNumber")
      .headers { it.setBearerAuth(token) }
      .bodyValue(request)
      .retrieve()
      .bodyToMono(CreateControlledNumberResponse::class.java)
      .onErrorResume(WebClientResponseException::class.java) { ex ->
        logger.error("BT response [${ex.statusCode}]: ${ex.responseBodyAsString}")
        Mono.error(ex)
      }
  }

  fun getRelationships(): Mono<BtRelationshipsResponse> = getBtToken().flatMap { token ->
    btPinPhoneWebClient
      .post()
      .uri("/pcs/Relationships")
      .headers { it.setBearerAuth(token) }
      .bodyValue(emptyMap<String, Any>())
      .retrieve()
      .bodyToMono(BtRelationshipsResponse::class.java)
      .onErrorResume(WebClientResponseException::class.java) { ex ->
        logger.error("BT response [${ex.statusCode}]: ${ex.responseBodyAsString}")
        Mono.error(ex)
      }
  }

  fun accountCredit(request: CreditAccountRequest): Mono<CreditAccountResponse> = getBtToken().flatMap { token ->
    btPinPhoneWebClient
      .post()
      .uri("/pcs/AccountCredit")
      .headers { it.setBearerAuth(token) }
      .bodyValue(request)
      .retrieve()
      .bodyToMono(CreditAccountResponse::class.java)
      .onErrorResume(WebClientResponseException::class.java) { ex ->
        logger.error("BT response [${ex.statusCode}]: ${ex.responseBodyAsString}")
        Mono.error(ex)
      }
  }
}

data class CreateAccountRequest(
  val reference: String,
  val prisonerId: String,
  val prisonCode: String,
  val firstName: String,
  val middleName: String,
  val lastName: String,
  val statusActive: Boolean,
)

data class CreateControlledNumberRequest(
  val reference: String,
  val prisonerId: String,
  val name: String,
  val number: String,
  val relationshipId: Int,
  val controlStatus: Boolean,
  val alert: Boolean,
  val legal: Boolean,
  val override: Boolean,
  val allowMonitor: Boolean,
  val callAllowed: Boolean,
)

data class CreateAccountResponse(
  val reference: String,
  val prisonerId: String,
)

data class CreateControlledNumberResponse(
  val reference: String,
  val prisonerId: String,
  val id: Int,
)

data class BtRelationshipsResponse(
  val relationships: List<BtRelationship>,
)

data class BtRelationship(
  val id: Int,
  val description: String,
)

data class CreditAccountRequest(
  val reference: String,
  val prisonerId: String,
  val amountPence: Int,
)

data class CreditAccountResponse(
  val reference: String,
  val prisonerId: String,
  val creditLimitPence: Int,
  val preBalancePence: Int,
  val newBalancePence: Int,
)
