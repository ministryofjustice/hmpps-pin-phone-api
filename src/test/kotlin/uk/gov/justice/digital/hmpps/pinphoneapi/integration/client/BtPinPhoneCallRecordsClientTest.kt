package uk.gov.justice.digital.hmpps.pinphoneapi.integration.client

import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.post
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClient
import tools.jackson.databind.json.JsonMapper
import uk.gov.justice.digital.hmpps.pinphoneapi.client.WebClientErrorHandler
import uk.gov.justice.digital.hmpps.pinphoneapi.client.btPinPhoneClient.BtPinPhoneClient
import uk.gov.justice.digital.hmpps.pinphoneapi.client.btPinPhoneClient.generated.BtCallRecordsRequest
import uk.gov.justice.digital.hmpps.pinphoneapi.config.UpstreamException
import uk.gov.justice.digital.hmpps.pinphoneapi.integration.BT_CLIENT_ID
import uk.gov.justice.digital.hmpps.pinphoneapi.integration.BT_CLIENT_SECRET
import uk.gov.justice.digital.hmpps.pinphoneapi.integration.wiremock.BtMockServer
import java.time.LocalDate
import java.time.LocalDateTime

class BtPinPhoneCallRecordsClientTest {

  private lateinit var client: BtPinPhoneClient

  @BeforeEach
  fun resetMocks() {
    server.resetRequests()

    val webClient = WebClient.create("http://localhost:${server.port()}")

    val mapper = JsonMapper.builder()
      .findAndAddModules()
      .build()

    val webClientErrorHandler = WebClientErrorHandler(mapper)

    client = BtPinPhoneClient(
      webClient,
      BT_CLIENT_ID,
      BT_CLIENT_SECRET,
      webClientErrorHandler,
    )
  }

  @Nested
  inner class GetCallRecords {

    @Test
    fun `getCallRecords successfully returns BT prisoner call records`() {
      server.stubBtGetCallRecords()

      val result = client.getCallRecords(
        BtCallRecordsRequest(
          reference = "testReference",
          prisonerId = "xyz",
          startDate = LocalDate.of(2026, 11, 1),
          endDate = LocalDate.of(2026, 11, 30),
          pageSize = 50,
          pageNumber = 0,
        ),
      ).block()

      assertThat(result).isNotNull
      assertThat(result).hasSize(2)

      val firstRecord = result?.first()

      assertThat(firstRecord?.id).isEqualTo(1)
      assertThat(firstRecord?.phoneNumber).isEqualTo("07123456789")
      assertThat(firstRecord?.transactionStartTime)
        .isEqualTo(LocalDateTime.of(2026, 11, 20, 14, 30))
      assertThat(firstRecord?.type).isEqualTo(1)
      assertThat(firstRecord?.duration).isEqualTo(300)
      assertThat(firstRecord?.costPence).isEqualTo(150)
      assertThat(firstRecord?.balancePence).isEqualTo(5000)
    }

    @Test
    fun `getCallRecords throws UpstreamException on 404`() {
      server.stubGetBtAuthToken()

      server.stubFor(
        post("/pcs/CallRecords")
          .willReturn(
            aResponse()
              .withStatus(404)
              .withHeader("Content-Type", "application/json")
              .withBody(
                """{"status":404,"userMessage":"Prisoner not found"}""",
              ),
          ),
      )

      assertThatThrownBy {
        client.getCallRecords(
          BtCallRecordsRequest(
            reference = "testReference",
            prisonerId = "xyz",
            startDate = LocalDate.of(2026, 11, 1),
            endDate = LocalDate.of(2026, 11, 30),
            pageSize = 50,
            pageNumber = 0,
          ),
        ).block()
      }
        .isInstanceOf(UpstreamException::class.java)
        .hasMessageContaining("Prisoner not found")
    }

    @Test
    fun `getCallRecords throws UpstreamException on 500`() {
      server.stubGetBtAuthToken()

      server.stubFor(
        post("/pcs/CallRecords")
          .willReturn(
            aResponse()
              .withStatus(500)
              .withHeader("Content-Type", "application/json")
              .withBody(
                """{"status":500,"userMessage":"BT service error"}""",
              ),
          ),
      )

      assertThatThrownBy {
        client.getCallRecords(
          BtCallRecordsRequest(
            reference = "testReference",
            prisonerId = "xyz",
            startDate = LocalDate.of(2026, 11, 1),
            endDate = LocalDate.of(2026, 11, 30),
            pageSize = 50,
            pageNumber = 0,
          ),
        ).block()
      }
        .isInstanceOf(UpstreamException::class.java)
        .hasMessageContaining("BT service error")
    }
  }

  companion object {
    @JvmField
    internal val server = BtMockServer()

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
