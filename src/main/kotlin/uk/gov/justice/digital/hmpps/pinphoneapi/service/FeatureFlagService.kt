package uk.gov.justice.digital.hmpps.pinphoneapi.service

import io.flipt.client.FliptClient
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class FeatureFlagService(
  private val client: FliptClient,
) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun isEnabled(
    flagKey: String,
    entityId: String,
    context: Map<String, String> = emptyMap(),
    defaultValue: Boolean = false,
  ): Boolean = try {
    client.evaluateBoolean(flagKey, entityId, context).isEnabled
  } catch (e: Exception) {
    log.error("Flipt evaluation failed for flag {}", flagKey, e)
    defaultValue
  }
}
