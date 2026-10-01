package uk.gov.justice.digital.hmpps.pinphoneapi.integration.client

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.pinphoneapi.client.prisoneradjudications.generated.ActivePunishmentDto
import uk.gov.justice.digital.hmpps.pinphoneapi.client.prisoneradjudicationsclient.PrisonerAdjudicationsClient
import uk.gov.justice.digital.hmpps.pinphoneapi.integration.OFFENDER_BOOKING_ID
import uk.gov.justice.digital.hmpps.pinphoneapi.integration.wiremock.PrisonerAdjudicationsMockServer
import java.time.LocalDate

class PrisonerAdjudicationsClientTest {
  private lateinit var client: PrisonerAdjudicationsClient

  @BeforeEach
  fun resetMocks() {
    server.resetRequests()
    val webClient = WebClient.create("http://localhost:${server.port()}")
    client = PrisonerAdjudicationsClient(webClient)
  }

  @Test
  fun `getPrisoner - successfully returns prisoner`() {
    server.stubGetPrisonerAdjudication()

    val result = client.getPrisonerAdjudication(OFFENDER_BOOKING_ID).block()

    assertThat(result).isNotNull.hasSize(1)
    with(result!![0]) {
      assertThat(chargeNumber).isEqualTo("12345")
      assertThat(punishmentType).isEqualTo(ActivePunishmentDto.PunishmentType.PRIVILEGE)
      assertThat(privilegeType).isEqualTo(ActivePunishmentDto.PrivilegeType.CANTEEN)
      assertThat(otherPrivilege).isEqualTo("none")
      assertThat(duration).isEqualTo(5)
      assertThat(measurement).isEqualTo(ActivePunishmentDto.Measurement.DAYS)
      assertThat(startDate).isEqualTo(LocalDate.parse("2025-01-01"))
      assertThat(lastDay).isEqualTo(LocalDate.parse("2025-01-31"))
      assertThat(amount).isEqualTo(0.1)
      assertThat(stoppagePercentage).isEqualTo(0)
      assertThat(activatedFrom).isEqualTo("today")
    }
  }

  companion object {
    @JvmField
    internal val server = PrisonerAdjudicationsMockServer()

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
