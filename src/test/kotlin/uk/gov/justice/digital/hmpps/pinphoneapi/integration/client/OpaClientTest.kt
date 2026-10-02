package uk.gov.justice.digital.hmpps.pinphoneapi.integration.client

import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.containing
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.stubFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.client.WireMock.verify
import com.github.tomakehurst.wiremock.junit5.WireMockTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClient
import tools.jackson.databind.json.JsonMapper
import uk.gov.justice.digital.hmpps.pinphoneapi.client.WebClientErrorHandler
import uk.gov.justice.digital.hmpps.pinphoneapi.client.logicengine.OpaClient
import uk.gov.justice.digital.hmpps.pinphoneapi.model.logicengine.request.Input
import uk.gov.justice.digital.hmpps.pinphoneapi.model.logicengine.request.OpaRequest

@WireMockTest(httpPort = 9890)
class OpaClientTest {

  private lateinit var opaClient: OpaClient

  @BeforeEach
  fun setUp() {
    val webClient = WebClient.builder()
      .baseUrl("http://localhost:9890")
      .build()
    val mapper = JsonMapper.builder()
      .findAndAddModules()
      .build()
    val webClientErrorHandler = WebClientErrorHandler(mapper)
    opaClient = OpaClient(webClient, webClientErrorHandler)
  }

  @Test
  fun `should return successful response`() {
    stubFor(
      post(urlEqualTo("/v1/data/app/main/response"))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody(
              """
                    {
                    "result": {
                    "decision": "ALLOW",
                    "hidden": false,
                    "creditLimit": 2500,
                    "maxAvailableCredit": 1300,
                     "warnings": []
                     }
              }
              """.trimIndent(),
            ),
        ),
    )
    val request = OpaRequest(
      input = Input(
        productId = "BT_PIN_Phone",
        currentBalance = 1200,
        creditLimit = 2500,
      ),
    )
    val response = opaClient.evaluate(request)

    assertEquals("ALLOW", response.result.decision)
    assertEquals(false, response.result.hidden)
    assertEquals(1300, response.result.maxAvailableCredit)
    verify(
      postRequestedFor(
        urlEqualTo("/v1/data/app/main/response"),
      )
        .withRequestBody(
          containing("BT_PIN_Phone"),
        ),
    )
  }
}
