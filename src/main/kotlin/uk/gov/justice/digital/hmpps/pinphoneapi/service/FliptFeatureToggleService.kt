package uk.gov.justice.digital.hmpps.pinphoneapi.service

import io.flipt.client.FliptClient
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service

@Service
@ConditionalOnProperty(name = ["FLIPT.ENABLED"], havingValue = "true", matchIfMissing = true)
class FliptFeatureToggleService(
  private val fliptClient: FliptClient,
) {
  companion object {
    private val log: org.slf4j.Logger = LoggerFactory.getLogger("FliptFeatureToggleService")
  }

  fun isUpdatedPaymentsEnabled(offenderNo: String): Boolean = try {
    fliptClient.evaluateBoolean("use-updated-payment-logic", offenderNo, emptyMap()).isEnabled
  } catch (e: Exception) {
    log.warn("Flipt evaluation failed, defaulting to false: ${e.message}")
    false
  }

  // more toggles as added
}
