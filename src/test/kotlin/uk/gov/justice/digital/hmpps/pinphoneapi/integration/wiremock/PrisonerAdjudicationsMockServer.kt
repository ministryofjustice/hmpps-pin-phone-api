package uk.gov.justice.digital.hmpps.pinphoneapi.integration.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import tools.jackson.databind.json.JsonMapper
import uk.gov.justice.digital.hmpps.pinphoneapi.integration.OFFENDER_BOOKING_ID
import uk.gov.justice.digital.hmpps.pinphoneapi.integration.PinPhoneTestFixture

class PrisonerAdjudicationsMockServer : WireMockServer(WIREMOCK_PORT) {
  companion object {
    private const val WIREMOCK_PORT = 8081
  }

  private val mapper: JsonMapper = JsonMapper.builder().build()

  fun stubGetPrisonerAdjudication(offenderBookingId: String = OFFENDER_BOOKING_ID): StubMapping = stubFor(
    get("/reported-adjudications/punishments/$offenderBookingId/active")
      .willReturn(
        aResponse()
          .withHeader("Content-Type", "application/json")
          .withBody(
            mapper.writeValueAsString(
              PinPhoneTestFixture.activePunishments(),
            ),
          )
          .withStatus(200),
      ),
  )
}
