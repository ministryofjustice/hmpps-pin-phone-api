package uk.gov.justice.digital.hmpps.pinphoneapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.pinphoneapi.client.logicengine.OpaClient
import uk.gov.justice.digital.hmpps.pinphoneapi.model.logicengine.request.Input
import uk.gov.justice.digital.hmpps.pinphoneapi.model.logicengine.request.OpaRequest
import uk.gov.justice.digital.hmpps.pinphoneapi.model.logicengine.response.OpaResponse
import uk.gov.justice.digital.hmpps.pinphoneapi.model.logicengine.response.Result
import uk.gov.justice.digital.hmpps.pinphoneapi.service.logicengine.OpaService

@ExtendWith(MockitoExtension::class)
class OpaServiceTest {

  @Mock
  private lateinit var opaClient: OpaClient

  private lateinit var opaService: OpaService

  @BeforeEach
  fun beforeEach() {
    opaService = OpaService(opaClient)
  }

  @Test
  fun `should return OPA response`() {
    val request = OpaRequest(
      input = Input(
        productId = "BT_PIN_Phone",
        currentBalance = 1200,
        creditLimit = 2500,
      ),
    )

    val expectedResponse = OpaResponse(
      result = Result(
        hidden = false,
        decision = "ALLOW",
        creditLimit = 2500,
        maxAvailableCredit = 1300,
      ),
    )

    whenever<OpaResponse>(
      opaClient.evaluate(request),
    ).thenReturn(expectedResponse)

    val response = opaService.evaluatePolicy(request)

    assertThat(response).isEqualTo(expectedResponse)

    verify(opaClient).evaluate(request)
  }
}
