package uk.gov.justice.digital.hmpps.pinphoneapi.service.sar

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.pinphoneapi.client.medusaclient.MedusaInternalClient
import uk.gov.justice.digital.hmpps.pinphoneapi.mapping.sar.toSarPinPhoneList
import uk.gov.justice.digital.hmpps.pinphoneapi.model.sar.SubjectAccessResponseData
import uk.gov.justice.hmpps.kotlin.sar.HmppsPrisonSubjectAccessRequestService
import uk.gov.justice.hmpps.kotlin.sar.HmppsSubjectAccessRequestContent
import java.time.LocalDate
/**
 * Prisoners have the right to access and receive a copy of their personal data and other supplementary information.
 * This is commonly referred to as a subject access request or ‘SAR’.
 * The purpose of this service is to surface all relevant prisoner-specific information for a subject access request.
 * By extending HmppsPrisonSubjectAccessRequestService the endpoint needed for this is automatically included, no
 * additional controller (endpoint) is required.
 */
@Service
class SubjectAccessRequestService(
  private val medusaStoreClient: MedusaInternalClient,
) : HmppsPrisonSubjectAccessRequestService {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  override fun getPrisonContentFor(prn: String, fromDate: LocalDate?, toDate: LocalDate?): HmppsSubjectAccessRequestContent? = run {
    log.info("SAR: processing subject access request for prisoner $prn, from $fromDate to $toDate.")

    val from = fromDate ?: LocalDate.EPOCH
    val to = toDate ?: LocalDate.now()

    val response = medusaStoreClient.getSarReport(prn, from, to)

    log.info("SAR: Returning SAR response for prisoner $prn, from $from to $to.")

    if (response.sarContent!!.isEmpty()) {
      log.info("SAR: no matches found for prisoner $prn, from $from to $to.")
      return null
    }
    HmppsSubjectAccessRequestContent(
      content = SubjectAccessResponseData(
        prisonerNumber = prn,
        fromDate = from,
        toDate = to,
        pinPhones = response.sarContent?.toSarPinPhoneList(),
      ),
    )
  }
}
