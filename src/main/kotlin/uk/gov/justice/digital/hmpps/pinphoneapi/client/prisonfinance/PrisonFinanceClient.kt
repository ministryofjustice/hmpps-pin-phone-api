package uk.gov.justice.digital.hmpps.pinphoneapi.client.prisonfinance

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.pinphoneapi.client.WebClientErrorHandler
import uk.gov.justice.digital.hmpps.pinphoneapi.client.prisonfinance.generated.Account
import uk.gov.justice.digital.hmpps.pinphoneapi.client.prisonfinance.generated.AddHoldTransaction
import uk.gov.justice.digital.hmpps.pinphoneapi.client.prisonfinance.generated.CreateTransaction
import uk.gov.justice.digital.hmpps.pinphoneapi.client.prisonfinance.generated.HoldDetails
import uk.gov.justice.digital.hmpps.pinphoneapi.client.prisonfinance.generated.ReleaseHoldAndCreateTransaction
import uk.gov.justice.digital.hmpps.pinphoneapi.client.prisonfinance.generated.ReleaseHoldTransaction
import uk.gov.justice.digital.hmpps.pinphoneapi.client.prisonfinance.generated.Transaction
import uk.gov.justice.digital.hmpps.pinphoneapi.config.UpstreamException

@Component
class PrisonFinanceClient(
  @Qualifier("prisonApiWebClient") private val prisonerApiClient: WebClient,
  private val errorHandler: WebClientErrorHandler,
) {
  companion object {
    val logger: Logger = LoggerFactory.getLogger(PrisonFinanceClient::class.java)
  }


  @Suppress("ktlint:standard:function-expression-body")
  fun createTransactionPhone(
    prisonId: String,
    offenderNo: String,
    xClientName: String,
    request: CreateTransaction,
  ): Transaction {
    return try {
      prisonerApiClient.post()
        .uri("/api/v1/prison/{prisonId}/offenders/{offenderNo}/transactions", prisonId, offenderNo)
        .bodyValue(request)
        .header("X-Client-Name", xClientName)
        .retrieve()
        .bodyToMono(Transaction::class.java)
        .block()!!
    } catch (ex: WebClientResponseException.Conflict) {
      logger.info(
        "Phone transaction duplicate for offenderNo: {} clientUniqueReference: {}",
        offenderNo,
        request.clientUniqueRef,
      )
      throw DuplicateFinanceRequestException(request.clientUniqueRef)
    } catch (ex: WebClientResponseException) {
      val error = errorHandler.handleError(ex)
      logger.error("Phone transaction request failed for offenderNo: $offenderNo", error)
      throw UpstreamException(error.userMessage ?: "Phone transaction request failed")
    }
  }


  //todo: below calls will be made legacy and can be removed later

  @Suppress("ktlint:standard:function-expression-body")
  fun addHold(
    prisonId: String,
    offenderNo: String,
    request: AddHoldTransaction,
  ): HoldDetails {
    return try {
      prisonerApiClient.post()
        .uri("/api/finance-holds/prison/{prisonId}/offenders/{offenderNo}/add-hold", prisonId, offenderNo)
        .bodyValue(request)
        .retrieve()
        .bodyToMono(HoldDetails::class.java)
        .block()!!
    } catch (ex: WebClientResponseException.Conflict) {
      logger.info(
        "AddHold duplicate for offenderNo: {} clientUniqueReference: {}",
        offenderNo,
        request.clientUniqueReference,
      )
      throw DuplicateFinanceRequestException(request.clientUniqueReference)
    } catch (ex: WebClientResponseException) {
      val error = errorHandler.handleError(ex)
      logger.error("AddHold request failed for offenderNo: $offenderNo", error)
      throw UpstreamException(error.userMessage ?: "AddHold request failed")
    }
  }

  @Suppress("ktlint:standard:function-expression-body")
  fun releaseHold(
    prisonId: String,
    offenderNo: String,
    holdNumber: Number,
    request: ReleaseHoldTransaction,
  ): ResponseEntity<Void> {
    return try {
      prisonerApiClient.post()
        .uri(
          "/api/finance-holds/prison/{prisonId}/offenders/{offenderNo}/release-hold/{holdNumber}",
          prisonId,
          offenderNo,
          holdNumber,
        )
        .bodyValue(request)
        .retrieve()
        .toBodilessEntity()
        .block()!!
    } catch (ex: WebClientResponseException) {
      val errorResponse = errorHandler.handleError(ex)
      logger.error("ReleaseHold request failed for offenderNo: $offenderNo", errorResponse)
      throw UpstreamException(errorResponse.userMessage ?: "ReleaseHold request failed")
    }
  }

  @Suppress("ktlint:standard:function-expression-body")
  fun releaseHoldCreateTransaction(
    prisonId: String,
    offenderNo: String,
    holdNumber: Number,
    request: ReleaseHoldAndCreateTransaction,
  ): Transaction {
    return try {
      prisonerApiClient.post()
        .uri(
          "/api/finance-holds/prison/{prisonId}/offenders/{offenderNo}/release-hold-transaction/{holdNumber}",
          prisonId,
          offenderNo,
          holdNumber,
        )
        .bodyValue(request)
        .retrieve()
        .bodyToMono(Transaction::class.java)
        .block()!!
    } catch (ex: WebClientResponseException.Conflict) {
      logger.info(
        "release-hold-transaction duplicate for offenderNo: {} createClientUniqueReference: {}",
        offenderNo,
        request.createClientUniqueReference,
      )
      throw DuplicateFinanceRequestException(request.createClientUniqueReference)
    } catch (ex: WebClientResponseException) {
      val errorResponse = errorHandler.handleError(ex)
      logger.error("ReleaseHoldCreateTransaction request failed for offenderNo: $offenderNo", errorResponse)
      throw UpstreamException(errorResponse.userMessage ?: "ReleaseHoldCreateTransaction request failed")
    }
  }

  @Suppress("ktlint:standard:function-expression-body")
  fun getPrisonerBalance(bookingId: String): Mono<Account> {
    return prisonerApiClient.get()
      .uri("/api/bookings/{bookingId}/balances", bookingId)
      .retrieve()
      .bodyToMono(Account::class.java)
      .onErrorMap(WebClientResponseException::class.java) { ex ->
        val errorResponse = errorHandler.handleError(ex)
        logger.error("GET Balance request failed for bookingId: $bookingId", errorResponse)
        UpstreamException(errorResponse.userMessage ?: "getPrisonerBalance request failed")
      }
  }
}

class DuplicateFinanceRequestException(clientUniqueReference: String?) : RuntimeException("Duplicate post: clientUniqueReference/createClientUniqueReference $clientUniqueReference has been used before")
