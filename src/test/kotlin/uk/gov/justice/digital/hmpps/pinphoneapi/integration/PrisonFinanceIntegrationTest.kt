package uk.gov.justice.digital.hmpps.pinphoneapi.integration

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.pinphoneapi.client.prisonfinance.generated.AddHoldTransaction
import uk.gov.justice.digital.hmpps.pinphoneapi.client.prisonfinance.generated.ReleaseHoldAndCreateTransaction
import uk.gov.justice.digital.hmpps.pinphoneapi.integration.wiremock.HmppsAuthApiExtension
import uk.gov.justice.digital.hmpps.pinphoneapi.integration.wiremock.PrisonApiExtension

class PrisonFinanceIntegrationTest : IntegrationTestBase() {

  @Test
  fun `addHold - successfully adds a hold`() {
    val request = AddHoldTransaction(
      amount = 1634,
      clientTransactionId = "test-transaction-id",
      clientUniqueReference = "test-unique-ref",
      description = "HOLD",
      clientName = OFFENDER_ID,
    )

    HmppsAuthApiExtension.Companion.hmppsAuth.stubGrantToken()
    PrisonApiExtension.Companion.prisonApi.stubAddHold(PRISONER_ID, OFFENDER_ID)

    webTestClient.post()
      .uri("/api/finance/prisons/{prisonId}/offenders/{offenderNo}/addHold", PRISONER_ID, OFFENDER_ID)
      .headers(setAuthorisation())
      .bodyValue(request)
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.holdNumber").isEqualTo(12345)
  }

  @Test
  fun `addHold - returns 400 when insufficient balance`() {
    val request = AddHoldTransaction(
      amount = 1000000,
      clientTransactionId = "test-transaction-id",
      clientUniqueReference = "test-unique-ref",
      description = "HOLD",
      clientName = OFFENDER_ID,
    )
    val errorMessage = "Insufficient funds"

    HmppsAuthApiExtension.Companion.hmppsAuth.stubGrantToken()
    PrisonApiExtension.Companion.prisonApi.stubAddHoldFailure(PRISONER_ID, OFFENDER_ID, 400, errorMessage)

    webTestClient.post()
      .uri("/api/finance/prisons/{prisonId}/offenders/{offenderNo}/addHold", PRISONER_ID, OFFENDER_ID)
      .headers(setAuthorisation())
      .bodyValue(request)
      .exchange()
      .expectStatus().isBadRequest
      .expectBody()
      .jsonPath("$.userMessage").isEqualTo(errorMessage)
  }

  @Test
  fun `releaseHold - successfully releases a hold`() {
    HmppsAuthApiExtension.Companion.hmppsAuth.stubGrantToken()
    PrisonApiExtension.Companion.prisonApi.stubReleaseHold(PRISONER_ID, OFFENDER_ID, HOLD_NUMBER)

    webTestClient.post()
      .uri(
        "/api/finance/prisons/{prisonId}/offenders/{offenderNo}/releaseHold/{holdNumber}",
        PRISONER_ID,
        OFFENDER_ID,
        HOLD_NUMBER,
      )
      .headers(setAuthorisation())
      .exchange()
      .expectStatus().isCreated
      .expectBody().isEmpty
  }

  @Test
  fun `releaseHold - returns 400 when release fails`() {
    val errorMessage = "Hold not found"
    HmppsAuthApiExtension.Companion.hmppsAuth.stubGrantToken()
    PrisonApiExtension.Companion.prisonApi.stubReleaseHoldFailure(
      PRISONER_ID,
      OFFENDER_ID,
      HOLD_NUMBER,
      400,
      errorMessage,
    )

    webTestClient.post()
      .uri(
        "/api/finance/prisons/{prisonId}/offenders/{offenderNo}/releaseHold/{holdNumber}",
        PRISONER_ID,
        OFFENDER_ID,
        HOLD_NUMBER,
      )
      .headers(setAuthorisation())
      .exchange()
      .expectStatus().isBadRequest
      .expectBody()
      .jsonPath("$.userMessage").isEqualTo(errorMessage)
  }

  @Test
  fun `releaseHoldAndCreateTransaction - successfully releases a hold and creates a transaction`() {
    val request = ReleaseHoldAndCreateTransaction(
      type = ReleaseHoldAndCreateTransaction.Type.PHONE,
      createDescription = "Pin phone credit",
      clientTransactionId = "test-transaction-id",
      removeClientUniqueReference = "test-remove-ref",
      createClientUniqueReference = "test-create-ref",
    )
    HmppsAuthApiExtension.Companion.hmppsAuth.stubGrantToken()
    PrisonApiExtension.Companion.prisonApi.stubRelaseHoldAndCreateTransaction(PRISONER_ID, OFFENDER_ID, HOLD_NUMBER)

    webTestClient.post()
      .uri(
        "/api/finance/prisons/{prisonId}/offenders/{offenderNo}/releaseHoldCreateTransaction/{holdNumber}",
        PRISONER_ID,
        OFFENDER_ID,
        HOLD_NUMBER,
      )
      .headers(setAuthorisation())
      .bodyValue(request)
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.id").isEqualTo("468198331-1")
  }

  @Test
  fun `releaseHoldAndCreateTransaction - returns 400 when request fails`() {
    val request = ReleaseHoldAndCreateTransaction(
      type = ReleaseHoldAndCreateTransaction.Type.PHONE,
      createDescription = "Pin phone credit",
      clientTransactionId = "test-transaction-id",
      removeClientUniqueReference = "test-remove-ref",
      createClientUniqueReference = "test-create-ref",
    )
    val errorMessage = "Transaction creation failed"
    HmppsAuthApiExtension.Companion.hmppsAuth.stubGrantToken()
    PrisonApiExtension.Companion.prisonApi.stubRelaseHoldAndCreateTransactionFailure(
      PRISONER_ID,
      OFFENDER_ID,
      HOLD_NUMBER,
      400,
      errorMessage,
    )

    webTestClient.post()
      .uri(
        "/api/finance/prisons/{prisonId}/offenders/{offenderNo}/releaseHoldCreateTransaction/{holdNumber}",
        PRISONER_ID,
        OFFENDER_ID,
        HOLD_NUMBER,
      )
      .headers(setAuthorisation())
      .bodyValue(request)
      .exchange()
      .expectStatus().isBadRequest
      .expectBody()
      .jsonPath("$.userMessage").isEqualTo(errorMessage)
  }
}
