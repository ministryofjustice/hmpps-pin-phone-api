package uk.gov.justice.digital.hmpps.pinphoneapi.client

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClientResponseException
import tools.jackson.core.JacksonException
import tools.jackson.databind.ObjectMapper
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

@Component
class WebClientErrorHandler(private val objectMapper: ObjectMapper) {

  fun handleError(ex: WebClientResponseException): ErrorResponse = try {
    objectMapper.readValue(ex.responseBodyAsString, ErrorResponse::class.java)
  } catch (parseException: JacksonException) {
    logger.error("Failed to parse error response body for status: ${ex.statusCode}", parseException)
    ErrorResponse(
      status = ex.statusCode.value(),
      errorCode = "UNKNOWN",
      userMessage = "Unable to parse error response from server.",
      developerMessage = parseException.message ?: "Error parsing response body.",
      moreInfo = "No additional information available.",
    )
  }

  companion object {
    private val logger = LoggerFactory.getLogger(WebClientErrorHandler::class.java)
  }
}
