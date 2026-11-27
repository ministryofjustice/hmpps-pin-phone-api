package uk.gov.justice.digital.hmpps.pinphoneapi.service

import io.flipt.client.FliptClient
import io.flipt.client.models.BooleanEvaluationResponse
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever

class FliptFeatureToggleServiceTest {

  private val fliptClient: FliptClient = mock()
  private val service = FliptFeatureToggleService(fliptClient)

  @Nested
  inner class UseUpdatedPaymentLogic {
    @Test
    fun `returns true when flipt evaluates to enabled`() {
      val result = mock<BooleanEvaluationResponse>()
      whenever(result.isEnabled).thenReturn(true)
      whenever(fliptClient.evaluateBoolean(eq("use-updated-payment-logic"), eq("A1234BC"), any()))
        .thenReturn(result)

      assertTrue(service.isUpdatedPaymentsEnabled("A1234BC"))
    }

    @Test
    fun `returns false when flipt evaluates to disabled`() {
      val result = mock<BooleanEvaluationResponse>()
      whenever(result.isEnabled).thenReturn(false)
      whenever(fliptClient.evaluateBoolean(eq("use-updated-payment-logic"), eq("A1234BC"), any()))
        .thenReturn(result)

      assertFalse(service.isUpdatedPaymentsEnabled("A1234BC"))
    }
  }
}
