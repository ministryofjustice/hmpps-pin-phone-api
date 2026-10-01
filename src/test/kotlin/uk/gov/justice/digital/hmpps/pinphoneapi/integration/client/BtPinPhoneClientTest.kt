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
import uk.gov.justice.digital.hmpps.pinphoneapi.client.btPinPhoneClient.generated.AccountCreditRequest
import uk.gov.justice.digital.hmpps.pinphoneapi.client.btPinPhoneClient.generated.BtPinPhoneBalanceRequest
import uk.gov.justice.digital.hmpps.pinphoneapi.client.btPinPhoneClient.generated.BtPinPhoneControlledNumbersRequest
import uk.gov.justice.digital.hmpps.pinphoneapi.config.UpstreamException
import uk.gov.justice.digital.hmpps.pinphoneapi.integration.BT_CLIENT_ID
import uk.gov.justice.digital.hmpps.pinphoneapi.integration.BT_CLIENT_SECRET
import uk.gov.justice.digital.hmpps.pinphoneapi.integration.PinPhoneTestFixture.contactList
import uk.gov.justice.digital.hmpps.pinphoneapi.integration.wiremock.BtMockServer

class BtPinPhoneClientTest {
  private lateinit var client: BtPinPhoneClient

  @BeforeEach
  fun resetMocks() {
    server.resetRequests()
    val webClient = WebClient.create("http://localhost:${server.port()}")
    val mapper = JsonMapper.builder()
      .findAndAddModules()
      .build()
    val webClientErrorHandler = WebClientErrorHandler(mapper)
    client = BtPinPhoneClient(webClient, BT_CLIENT_ID, BT_CLIENT_SECRET, webClientErrorHandler)
  }

  @Nested
  inner class GetBearerToken {

    @Test
    fun `getBearerToken - successfully returns admin token`() {
      server.stubGetBtAuthToken()

      val result = client.getBtToken().block()

      assertThat(result).isNotNull
      assertThat(result?.accessToken).isEqualTo("bt-test-token")
      assertThat(result?.tokenType).isEqualTo("Bearer")
      assertThat(result?.expiresIn).isEqualTo(3600)
    }

    @Test
    fun `getBearerToken - throws UpstreamException on 401`() {
      server.stubFor(
        post("/auth/token")
          .willReturn(
            aResponse()
              .withStatus(401)
              .withHeader("Content-Type", "application/json")
              .withBody("""{"status":401,"userMessage":"Invalid credentials"}"""),
          ),
      )

      assertThatThrownBy { client.getBtToken().block() }
        .isInstanceOf(UpstreamException::class.java)
        .hasMessageContaining("Invalid credentials")
    }

    @Test
    fun `getBearerToken - throws UpstreamException with fallback message on unparseable error`() {
      server.stubFor(
        post("/auth/token")
          .willReturn(
            aResponse()
              .withStatus(500)
              .withHeader("Content-Type", "text/plain")
              .withBody("Internal Server Error"),
          ),
      )

      assertThatThrownBy { client.getBtToken().block() }
        .isInstanceOf(UpstreamException::class.java)
        .hasMessageContaining("Unable to parse error response from server.")
    }
  }

  @Nested
  inner class GetBalance {

    @Test
    fun `getBtBalance successfully returns BT balance and credit limit`() {
      server.stubBtGetBalance()

      val result = client.getPrisonerBalance(BtPinPhoneBalanceRequest("testReference", "xyz")).block()

      assertThat(result).isNotNull
      assertThat(result?.reference).isEqualTo("testReference")
      assertThat(result?.prisonerId).isEqualTo("xyz")
      assertThat(result?.balancePence).isEqualTo(200001)
      assertThat(result?.creditLimitPence).isEqualTo(500000)
    }

    @Test
    fun `getBtBalance - throws UpstreamException on 404`() {
      server.stubGetBtAuthToken()
      server.stubFor(
        post("/pcs/Balance")
          .willReturn(
            aResponse()
              .withStatus(404)
              .withHeader("Content-Type", "application/json")
              .withBody("""{"status":404,"userMessage":"Prisoner not found"}"""),
          ),
      )

      assertThatThrownBy {
        client.getPrisonerBalance(BtPinPhoneBalanceRequest("testReference", "xyz")).block()
      }
        .isInstanceOf(UpstreamException::class.java)
        .hasMessageContaining("Prisoner not found")
    }

    @Test
    fun `getBtBalance - throws UpstreamException on 500`() {
      server.stubGetBtAuthToken()
      server.stubFor(
        post("/pcs/Balance")
          .willReturn(
            aResponse()
              .withStatus(500)
              .withHeader("Content-Type", "application/json")
              .withBody("""{"status":500,"userMessage":"BT service error"}"""),
          ),
      )

      assertThatThrownBy {
        client.getPrisonerBalance(BtPinPhoneBalanceRequest("testReference", "xyz")).block()
      }
        .isInstanceOf(UpstreamException::class.java)
        .hasMessageContaining("BT service error")
    }
  }

  @Nested
  inner class GetContacts {
    @Test
    fun `getContacts successfully returns BT prisoner contacts`() {
      server.stubBtGetContacts()

      val result = client.getPrisonerContacts(BtPinPhoneControlledNumbersRequest("testReference", "xyz")).block()

      assertThat(result).isNotNull
      assertThat(result?.reference).isEqualTo("testReference")
      assertThat(result?.prisonerId).isEqualTo("xyz")
      assertThat(result?.controlledNumbers).isEqualTo(contactList)
    }

    @Test
    fun `getContacts - throws UpstreamException on 404`() {
      server.stubGetBtAuthToken()
      server.stubFor(
        post("/pcs/ControlledNumbers")
          .willReturn(
            aResponse()
              .withStatus(404)
              .withHeader("Content-Type", "application/json")
              .withBody("""{"status":404,"userMessage":"Prisoner not found"}"""),
          ),
      )

      assertThatThrownBy {
        client.getPrisonerContacts(BtPinPhoneControlledNumbersRequest("testReference", "xyz")).block()
      }
        .isInstanceOf(UpstreamException::class.java)
        .hasMessageContaining("Prisoner not found")
    }

    @Test
    fun `getContacts - throws UpstreamException on 500`() {
      server.stubGetBtAuthToken()
      server.stubFor(
        post("/pcs/ControlledNumbers")
          .willReturn(
            aResponse()
              .withStatus(500)
              .withHeader("Content-Type", "application/json")
              .withBody("""{"status":500,"userMessage":"BT service error"}"""),
          ),
      )

      assertThatThrownBy {
        client.getPrisonerContacts(BtPinPhoneControlledNumbersRequest("testReference", "xyz")).block()
      }
        .isInstanceOf(UpstreamException::class.java)
        .hasMessageContaining("BT service error")
    }
  }

  @Nested
  inner class AddCredit {

    @Test
    fun `addBtCredit successfully returns BT balance and credit limit`() {
      server.stubBtAddCredit()

      val result = client.addCredit(AccountCreditRequest("testReference", "xyz", 100)).block()

      assertThat(result).isNotNull
      assertThat(result?.reference).isEqualTo("testReference")
      assertThat(result?.prisonerId).isEqualTo("xyz123")
      assertThat(result?.creditLimitPence).isEqualTo(5000)
      assertThat(result?.preBalancePence).isEqualTo(1000)
      assertThat(result?.newBalancePence).isEqualTo(1100)
    }

    @Test
    fun `addBtCredit - throws UpstreamException on 404`() {
      server.stubGetBtAuthToken()
      server.stubFor(
        post("/pcs/AccountCredit")
          .willReturn(
            aResponse()
              .withStatus(404)
              .withHeader("Content-Type", "application/json")
              .withBody("""{"status":404,"userMessage":"Prisoner not found"}"""),
          ),
      )

      assertThatThrownBy {
        client.addCredit(AccountCreditRequest("testReference", "xyz", 100)).block()
      }
        .isInstanceOf(UpstreamException::class.java)
        .hasMessageContaining("Prisoner not found")
    }

    @Test
    fun `addBtCredit - throws UpstreamException on 500`() {
      server.stubBtAddCredit()
      server.stubFor(
        post("/pcs/AccountCredit")
          .willReturn(
            aResponse()
              .withStatus(500)
              .withHeader("Content-Type", "application/json")
              .withBody("""{"status":500,"userMessage":"BT service error"}"""),
          ),
      )

      assertThatThrownBy {
        client.addCredit(AccountCreditRequest("testReference", "xyz", 100)).block()
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
