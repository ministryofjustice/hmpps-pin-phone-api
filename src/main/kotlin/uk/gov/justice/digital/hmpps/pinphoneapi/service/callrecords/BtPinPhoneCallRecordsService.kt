package uk.gov.justice.digital.hmpps.pinphoneapi.service.callrecords

import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.pinphoneapi.client.btPinPhoneClient.BtPinPhoneClient
import uk.gov.justice.digital.hmpps.pinphoneapi.client.btPinPhoneClient.generated.BtCallRecordsRequest
import uk.gov.justice.digital.hmpps.pinphoneapi.mapping.prisonerenrichment.BtPinPhoneCallRecordDto
import uk.gov.justice.digital.hmpps.pinphoneapi.mapping.prisonerenrichment.toDto
import java.time.LocalDate
import java.util.UUID

@Service
class BtPinPhoneCallRecordsService(
  private val btPinPhoneClient: BtPinPhoneClient,
) {

  /**
   * @param prisonerNumber The unique identifier for the prisoner
   * @param startDate Start of the search period
   * @param endDate End of the search period
   * @return A Mono emitting a list of prisoner call records
   */
  fun getCallRecords(
    prisonerNumber: String,
    startDate: LocalDate,
    endDate: LocalDate,
    pageSize: Int,
    pageNumber: Int,
  ): Mono<List<BtPinPhoneCallRecordDto>> {
    val reference = UUID.randomUUID()
      .toString()
      .replace("-", "")
      .take(20)

    return btPinPhoneClient.getCallRecords(
      BtCallRecordsRequest(
        reference = reference,
        prisonerId = prisonerNumber,
        startDate = startDate,
        endDate = endDate,
        pageSize = pageSize,
        pageNumber = pageNumber,
      ),
    ).map { records ->
      records.map { it.toDto() }
    }
  }
}
