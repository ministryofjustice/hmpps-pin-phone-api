package uk.gov.justice.digital.hmpps.pinphoneapi.integration.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import tools.jackson.databind.json.JsonMapper
import uk.gov.justice.digital.hmpps.pinphoneapi.client.btPinPhoneClient.generated.AccountCreditResponse
import uk.gov.justice.digital.hmpps.pinphoneapi.client.btPinPhoneClient.generated.BtPinPhoneBalanceResponse
import uk.gov.justice.digital.hmpps.pinphoneapi.client.btPinPhoneClient.generated.BtPinPhoneControlledNumbersResponse
import uk.gov.justice.digital.hmpps.pinphoneapi.integration.PinPhoneTestFixture.contactList

class BtMockServer : WireMockServer(WIREMOCK_PORT) {
  companion object {
    private const val WIREMOCK_PORT = 8099
  }

  private val mapper: JsonMapper = JsonMapper.builder().build()

  fun stubGetBtAuthToken(): StubMapping = stubFor(
    post("/auth/token")
      .willReturn(
        aResponse()
          .withHeader("Content-Type", "application/json")
          .withBody(
            mapper.writeValueAsString(
              mapOf(
                "accessToken" to "bt-test-token",
                "tokenType" to "Bearer",
                "expiresIn" to 3600,
              ),
            ),
          )
          .withStatus(200),
      ),
  )

  fun stubBtGetBalance(): StubMapping {
    stubGetBtAuthToken()
    return stubFor(
      post("/pcs/Balance")
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(
              mapper.writeValueAsString(
                BtPinPhoneBalanceResponse(
                  "testReference",
                  "xyz",
                  200001,
                  500000,
                ),
              ),
            )
            .withStatus(200),
        ),
    )
  }

  fun stubBtGetContacts(): StubMapping {
    stubGetBtAuthToken()
    return stubFor(
      post("/pcs/ControlledNumbers")
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(
              mapper.writeValueAsString(
                BtPinPhoneControlledNumbersResponse(
                  reference = "testReference",
                  prisonerId = "xyz",
                  controlledNumbers = contactList,
                ),
              ),
            )
            .withStatus(200),
        ),
    )
  }

  fun stubBtAddCredit(): StubMapping {
    stubGetBtAuthToken()
    return stubFor(
      post("/pcs/AccountCredit")
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(
              mapper.writeValueAsString(
                AccountCreditResponse(
                  reference = "testReference",
                  prisonerId = "xyz123",
                  creditLimitPence = 5000,
                  preBalancePence = 1000,
                  newBalancePence = 1100,
                ),
              ),
            )
            .withStatus(200),
        ),
    )
  }
}

class BtApiExtension :
  BeforeAllCallback,
  AfterAllCallback,
  BeforeEachCallback {
  companion object {
    @JvmField
    val btApi = BtMockServer()
  }

  override fun beforeAll(context: ExtensionContext): Unit = btApi.start()
  override fun beforeEach(context: ExtensionContext) {
    btApi.resetAll()
    btApi.stubGetBtAuthToken()
    btApi.stubBtGetBalance()
  }

  override fun afterAll(context: ExtensionContext): Unit = btApi.stop()
}
