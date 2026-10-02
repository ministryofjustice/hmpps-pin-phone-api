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
import uk.gov.justice.digital.hmpps.pinphoneapi.client.medusaapiclient.generated.CartResponse
import uk.gov.justice.digital.hmpps.pinphoneapi.client.medusaapiclient.generated.CartResponseCart
import uk.gov.justice.digital.hmpps.pinphoneapi.client.medusaapiclient.generated.CompleteCartResponse

class MedusaMockServer : WireMockServer(WIREMOCK_PORT) {
  companion object {
    private const val WIREMOCK_PORT = 8087
  }

  private val mapper: JsonMapper = JsonMapper.builder().build()

  fun stubGetAdminToken(): StubMapping = stubFor(
    post("/auth/user/emailpass")
      .willReturn(
        aResponse()
          .withHeader("Content-Type", "application/json")
          .withBody(mapper.writeValueAsString(mapOf("token" to "test-token")))
          .withStatus(200),
      ),
  )

  fun stubCreateCart(): StubMapping = stubFor(
    post("/internal/pin-phone/carts")
      .willReturn(
        aResponse()
          .withHeader("Content-Type", "application/json")
          .withBody(
            mapper.writeValueAsString(
              CartResponse(
                cart = CartResponseCart(id = "test-cart-id"),
              ),
            ),
          )
          .withStatus(200),
      ),
  )

  fun stubAddLineItem(cartId: String = "test-cart-id"): StubMapping = stubFor(
    post("/internal/pin-phone/carts/$cartId/add-items")
      .willReturn(
        aResponse()
          .withHeader("Content-Type", "application/json")
          .withBody(
            mapper.writeValueAsString(
              CartResponse(
                cart = CartResponseCart(id = cartId),
              ),
            ),
          )
          .withStatus(200),
      ),
  )

  fun stubCompleteCart(cartId: String = "test-cart-id"): StubMapping = stubFor(
    post("/internal/pin-phone/carts/$cartId/complete")
      .willReturn(
        aResponse()
          .withHeader("Content-Type", "application/json")
          .withBody(
            mapper.writeValueAsString(
              CompleteCartResponse(
                paymentSuccessful = true,
                orderStatusRecorded = true,
                orderId = "test-order-id",
                cartId = "test-cart-id",
              ),
            ),
          )
          .withStatus(200),
      ),
  )
}

class MedusaApiExtension :
  BeforeAllCallback,
  AfterAllCallback,
  BeforeEachCallback {
  companion object {
    @JvmField
    val medusaApi = MedusaMockServer()
  }

  override fun beforeAll(context: ExtensionContext): Unit = medusaApi.start()
  override fun beforeEach(context: ExtensionContext) {
    medusaApi.resetAll()
    medusaApi.stubGetAdminToken()
    medusaApi.stubCreateCart()
    medusaApi.stubAddLineItem()
    medusaApi.stubCompleteCart()
  }

  override fun afterAll(context: ExtensionContext): Unit = medusaApi.stop()
}
