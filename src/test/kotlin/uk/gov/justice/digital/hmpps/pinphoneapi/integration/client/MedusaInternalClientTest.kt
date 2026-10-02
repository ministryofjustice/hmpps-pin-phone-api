package uk.gov.justice.digital.hmpps.pinphoneapi.integration.client

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.web.reactive.function.client.WebClient
import tools.jackson.databind.json.JsonMapper
import uk.gov.justice.digital.hmpps.pinphoneapi.client.WebClientErrorHandler
import uk.gov.justice.digital.hmpps.pinphoneapi.client.medusaapiclient.generated.AddItemsRequest
import uk.gov.justice.digital.hmpps.pinphoneapi.client.medusaapiclient.generated.CartMetadata
import uk.gov.justice.digital.hmpps.pinphoneapi.client.medusaapiclient.generated.CreateCartRequest
import uk.gov.justice.digital.hmpps.pinphoneapi.client.medusaapiclient.generated.PaymentRequest
import uk.gov.justice.digital.hmpps.pinphoneapi.client.medusaclient.MedusaInternalClient
import uk.gov.justice.digital.hmpps.pinphoneapi.config.UpstreamException
import uk.gov.justice.digital.hmpps.pinphoneapi.integration.wiremock.MedusaMockServer

class MedusaInternalClientTest {
  private lateinit var client: MedusaInternalClient

  @BeforeEach
  fun resetMocks() {
    server.resetRequests()
    val webClient = WebClient.create("http://localhost:${server.port()}")
    val mapper = JsonMapper.builder()
      .findAndAddModules()
      .build()
    val webClientErrorHandler = WebClientErrorHandler(mapper)
    client = MedusaInternalClient(webClient, webClientErrorHandler)
  }

  @Test
  fun `createCart- successfully creates a cart`() {
    server.stubCreateCart()
    val cartRequest = CreateCartRequest(
      CartMetadata(
        prisonId = "MDI",
        offenderNo = "A1234AA",
        firstName = "John",
        secondName = "Doe",
      ),
    )

    val result = client.createCart(cartRequest)

    assertThat(result).isNotNull
    assertThat(result.cart?.id).isEqualTo("test-cart-id")
  }

  @Test
  fun `addPinPhoneItemsToCart - successfully adds line item`() {
    val cartId = "test-cart-id"
    server.stubAddLineItem(cartId)
    val addItemsRequest = AddItemsRequest(amount = 500)

    val result = client.addPinPhoneItemsToCart(addItemsRequest, cartId)

    assertThat(result).isNotNull
    assertThat(result.cart?.id).isEqualTo(cartId)
  }

  @Test
  fun `completeCart- successfully completes a cart`() {
    val cartId = "test-cart-id"
    server.stubCompleteCart(cartId)
    val paymentRequest = PaymentRequest(
      amountPence = 1000,
      offenderNo = "A1234AA",
      prisonId = "XYZ",
      paymentStatus = PaymentRequest.PaymentStatus.AUTHORIZED,
      financeTransactionReference = "ref-123",
      financeHoldNumber = 12345,
      btCreditLimitPence = 50000,
      btPreBalancePence = 1000,
      btNewBalancePence = 2000,
      errorCode = null,
      errorMessage = null,
    )

    val result = client.completeCart(cartId, paymentRequest)

    assertThat(result).isNotNull
    assertThat(result.orderId).isEqualTo("test-order-id")
  }

  @Test
  fun `createCart - returns custom error message when medusa is down`() {
    // simulate "service down"
    server.stop()
    try {
      val cartRequest = CreateCartRequest(
        CartMetadata(
          prisonId = "MDI",
          offenderNo = "A1234AA",
          firstName = "John",
          secondName = "Doe",
        ),
      )
      val exception = assertThrows<UpstreamException> {
        client.createCart(cartRequest)
      }
      assertThat(exception.message).contains("Medusa service is currently unavailable")
    } finally {
      server.start()
    }
  }

  companion object {
    @JvmField
    internal val server = MedusaMockServer()

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
