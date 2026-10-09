package uk.gov.justice.digital.hmpps.pinphoneapi.service

import io.flipt.client.FliptClient
import io.flipt.client.FliptException
import io.flipt.client.models.BooleanEvaluationResponse
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
class FeatureFlagServiceTest {
  private lateinit var featureFlagService: FeatureFlagService

  @Mock
  private lateinit var fliptClient: FliptClient
  private val response: BooleanEvaluationResponse = mock()

  @BeforeEach
  fun setup() {
    featureFlagService = FeatureFlagService(fliptClient)
  }

  @Test
  fun `Return true when the feature flag is enabled`() {
    whenever(fliptClient.evaluateBoolean("random-flag", "entityId", emptyMap()))
      .thenReturn(response)
    whenever(response.isEnabled).thenReturn(true)

    assertTrue(featureFlagService.isEnabled("random-flag", "entityId", emptyMap()))
  }

  @Test
  fun `Return false when the feature flag is disabled`() {
    whenever(fliptClient.evaluateBoolean("random-flag", "entityId", emptyMap()))
      .thenReturn(response)
    whenever(response.isEnabled).thenReturn(false)

    assertFalse(featureFlagService.isEnabled("random-flag", "entityId", emptyMap()))
  }

  @Test
  fun `Return default false when the flag can't be retrieved`() {
    whenever(fliptClient.evaluateBoolean("random-flag", "entityId", emptyMap()))
      .thenThrow(FliptException::class.java)

    assertFalse(featureFlagService.isEnabled("random-flag", "entityId", emptyMap()))
  }

  @Test
  fun `Return default true when the flag can't be retrieved, but default true set`() {
    whenever(fliptClient.evaluateBoolean("random-flag", "entityId", emptyMap()))
      .thenThrow(FliptException::class.java)

    assertTrue(featureFlagService.isEnabled("random-flag", "entityId", emptyMap(), true))
  }
}
