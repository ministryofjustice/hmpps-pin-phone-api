package uk.gov.justice.digital.hmpps.pinphoneapi.config

import io.flipt.client.FliptClient
import jakarta.annotation.PreDestroy
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Duration

@Configuration
@EnableConfigurationProperties
class FliptConfiguration(
  @param:Value("\${flipt.url}") val fliptUrl: String,
  @param:Value("\${flipt.namespace}") val fliptNamespace: String,
  @param:Value("\${flipt.enabled:false}") val fliptEnabled: Boolean,
) {

  private lateinit var client: FliptClient

  @Bean
  fun fliptClient(): FliptClient {
    client = if (fliptEnabled) {
      FliptClient.builder()
        .namespace(fliptNamespace)
        .url(fliptUrl)
        .updateInterval(Duration.ofSeconds(120))
        .build()
    } else {
      FliptClient.builder()
        .url("http://localhost")
        .namespace("default")
        .build()
    }
    return client
  }

  @PreDestroy
  fun cleanup() {
    if (::client.isInitialized) {
      client.close()
    }
  }
}
