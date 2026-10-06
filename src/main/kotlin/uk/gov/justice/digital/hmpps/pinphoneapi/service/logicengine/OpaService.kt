package uk.gov.justice.digital.hmpps.pinphoneapi.service.logicengine

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.pinphoneapi.client.logicengine.OpaClient
import uk.gov.justice.digital.hmpps.pinphoneapi.model.logicengine.request.OpaRequest
import uk.gov.justice.digital.hmpps.pinphoneapi.model.logicengine.response.OpaResponse

@Service
class OpaService(
  private val opaClient: OpaClient,
) {

  fun evaluatePolicy(request: OpaRequest): OpaResponse = opaClient.evaluate(request)
}
