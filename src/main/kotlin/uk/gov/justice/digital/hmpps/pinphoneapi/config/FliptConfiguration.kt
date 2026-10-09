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
) {

  private lateinit var client: FliptClient

  @Bean
  fun fliptClient() = FliptClient
    .builder()
    .namespace(fliptNamespace)
    .url(fliptUrl)
    .updateInterval(Duration.ofSeconds(120))
    .build()

  @PreDestroy
  fun cleanup() {
    client.close()
  }
}
