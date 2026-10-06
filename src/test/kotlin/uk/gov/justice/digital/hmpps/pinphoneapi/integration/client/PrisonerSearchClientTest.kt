package uk.gov.justice.digital.hmpps.pinphoneapi.integration.client

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClient
import tools.jackson.databind.json.JsonMapper
import uk.gov.justice.digital.hmpps.pinphoneapi.client.WebClientErrorHandler
import uk.gov.justice.digital.hmpps.pinphoneapi.client.prisonersearchclient.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.pinphoneapi.integration.PRISONER_NUMBER
import uk.gov.justice.digital.hmpps.pinphoneapi.integration.wiremock.PrisonerSearchMockServer
import java.time.LocalDate

class PrisonerSearchClientTest {
  private lateinit var client: PrisonerSearchClient

  @BeforeEach
  fun resetMocks() {
    server.resetRequests()
    val webClient = WebClient.create("http://localhost:${server.port()}")
    val mapper = JsonMapper.builder()
      .findAndAddModules()
      .build()
    val webClientErrorHandler = WebClientErrorHandler(mapper)
    client = PrisonerSearchClient(webClient, webClientErrorHandler)
  }

  @Test
  fun `getPrisoner - successfully returns prisoner`() {
    server.stubGetPrisoner()

    val result = client.getPrisoner(PRISONER_NUMBER).block()

    assertThat(result).isNotNull
    assertThat(result!!.prisonerNumber).isEqualTo(PRISONER_NUMBER)
    assertThat(result.bookNumber).isEqualTo("12345")
    assertThat(result.bookingId).isEqualTo("A1234BC")
    assertThat(result.dateOfBirth).isEqualTo(LocalDate.of(1990, 1, 15))
    assertThat(result.youthOffender).isEqualTo(false)
    assertThat(result.gender).isEqualTo("Female")
    assertThat(result.prisonId).isEqualTo("MDI")
    assertThat(result.currentIncentive?.level?.code).isEqualTo("STD")
    assertThat(result.currentIncentive?.level?.description).isEqualTo("Standard")
  }

  companion object {
    @JvmField
    internal val server = PrisonerSearchMockServer()

    @BeforeAll
    @JvmStatic
    fun startMocks() {
      server.start()
    }

    @AfterAll
    @JvmStatic
    fun stopMocks() {
      server.stop()
    }
  }
}
