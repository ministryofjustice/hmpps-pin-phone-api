package uk.gov.justice.digital.hmpps.pinphoneapi.integration.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext

class PrisonApiExtension :
  BeforeAllCallback,
  AfterAllCallback,
  BeforeEachCallback {
  companion object {
    val prisonApi = PrisonApiMockServer()
  }

  override fun beforeAll(context: ExtensionContext) {
    prisonApi.start()
  }

  override fun beforeEach(context: ExtensionContext) {
    prisonApi.resetRequests()
  }

  override fun afterAll(context: ExtensionContext) {
    prisonApi.stop()
  }
}

class PrisonApiMockServer : WireMockServer(WIREMOCK_PORT) {
  companion object {
    private const val WIREMOCK_PORT = 8098
  }

  fun stubAddHold(prisonId: String, offenderNo: String): StubMapping = stubFor(
    post(urlEqualTo("/api/finance-holds/prison/$prisonId/offenders/$offenderNo/add-hold"))
      .willReturn(
        aResponse()
          .withHeader("Content-Type", "application/json")
          .withBody(
            "{\n" +
              "  \"holdNumber\": 12345\n" +
              "}",
          )
          .withStatus(200),
      ),
  )

  fun stubAddHoldFailure(prisonId: String, offenderNo: String, status: Int, userMessage: String): StubMapping = stubFor(
    post(urlEqualTo("/api/finance-holds/prison/$prisonId/offenders/$offenderNo/add-hold"))
      .willReturn(
        aResponse()
          .withHeader("Content-Type", "application/json")
          .withBody(
            "{\n" +
              "  \"status\": $status,\n" +
              "  \"userMessage\": \"$userMessage\"\n" +
              "}",
          )
          .withStatus(status),
      ),
  )

  fun stubReleaseHold(prisonId: String, offenderNo: String, holdNumber: Number): StubMapping = stubFor(
    post(urlEqualTo("/api/finance-holds/prison/$prisonId/offenders/$offenderNo/release-hold/$holdNumber"))
      .willReturn(
        aResponse()
          .withStatus(201)
          .withBody("")
          .withHeader("Content-Length", "0")
          .withHeader("Connection", "close"),
      ),
  )

  @Suppress("MaxLineLength")
  fun stubReleaseHoldFailure(prisonId: String, offenderNo: String, holdNumber: Number, status: Int, userMessage: String): StubMapping = stubFor(
    post(urlEqualTo("/api/finance-holds/prison/$prisonId/offenders/$offenderNo/release-hold/$holdNumber"))
      .willReturn(
        aResponse()
          .withHeader("Content-Type", "application/json")
          .withBody(
            "{\n" +
              "  \"status\": $status,\n" +
              "  \"userMessage\": \"$userMessage\"\n" +
              "}",
          )
          .withStatus(status),
      ),
  )

  @Suppress("MaxLineLength")
  fun stubRelaseHoldAndCreateTransaction(prisonId: String, offenderNo: String, holdNumber: Number): StubMapping = stubFor(
    post(urlEqualTo("/api/finance-holds/prison/$prisonId/offenders/$offenderNo/release-hold-transaction/$holdNumber"))
      .willReturn(
        aResponse()
          .withHeader("Content-Type", "application/json")
          .withBody(
            "{\n" +
              "  \"id\": \"468198331-1\"\n" +
              "}",
          )
          .withStatus(200),
      ),
  )

  @Suppress("MaxLineLength")
  fun stubRelaseHoldAndCreateTransactionFailure(prisonId: String, offenderNo: String, holdNumber: Number, status: Int, userMessage: String): StubMapping = stubFor(
    post(urlEqualTo("/api/finance-holds/prison/$prisonId/offenders/$offenderNo/release-hold-transaction/$holdNumber"))
      .willReturn(
        aResponse()
          .withHeader("Content-Type", "application/json")
          .withBody(
            "{\n" +
              "  \"status\": $status,\n" +
              "  \"userMessage\": \"$userMessage\"\n" +
              "}",
          )
          .withStatus(status),
      ),
  )

  @Suppress("MaxLineLength")
  fun stubGetPrisonerBalance(bookingId: String): StubMapping = stubFor(
    get(urlEqualTo("/api/bookings/$bookingId/balances"))
      .willReturn(
        aResponse()
          .withHeader("Content-Type", "application/json")
          .withBody(
            "{\n" +
              "  \"spends\": 12.22,\n" +
              "  \"cash\": 10.00,\n" +
              "  \"savings\": 2.22,\n" +
              "  \"damageObligations\": 10.00,\n" +
              "  \"currency\": \"GBP\"\n" +
              "}",
          )
          .withStatus(200),
      ),
  )

  @Suppress("MaxLineLength")
  fun stubGetPrisonerBalanceTransactionFailure(bookingId: String, status: Int, userMessage: String): StubMapping = stubFor(
    get(urlEqualTo("/api/bookings/$bookingId/balances"))
      .willReturn(
        aResponse()
          .withHeader("Content-Type", "application/json")
          .withBody(
            "{\n" +
              "  \"status\": $status,\n" +
              "  \"userMessage\": \"$userMessage\"\n" +
              "}",
          )
          .withStatus(status),
      ),
  )
}
