package uk.gov.justice.digital.hmpps.pinphoneapi.service.pinphoneenrichment

import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.pinphoneapi.client.btPinPhoneClient.BtPinPhoneClient
import uk.gov.justice.digital.hmpps.pinphoneapi.client.btPinPhoneClient.generated.BtPinPhoneBalanceRequest
import uk.gov.justice.digital.hmpps.pinphoneapi.client.prisonersearchclient.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.pinphoneapi.client.prisonfinance.PrisonFinanceClient
import uk.gov.justice.digital.hmpps.pinphoneapi.mapping.prisonerenrichment.BalanceResponseDto
import uk.gov.justice.digital.hmpps.pinphoneapi.mapping.prisonerenrichment.BtPinPhoneResponseDto
import uk.gov.justice.digital.hmpps.pinphoneapi.mapping.prisonerenrichment.PrisonerSearchResponseDto
import uk.gov.justice.digital.hmpps.pinphoneapi.mapping.prisonerenrichment.toBalanceResponseDto
import uk.gov.justice.digital.hmpps.pinphoneapi.mapping.prisonerenrichment.toBtPinPhoneResponseDto
import uk.gov.justice.digital.hmpps.pinphoneapi.mapping.prisonerenrichment.toPrisonerSearchResponseDto
import java.util.UUID

/**
 * Service responsible for enriching prisoner data with additional information from multiple sources.
 *
 * Aggregates data from various HMPPS APIs and BT,
 * Returns the aggregated balance and credit information
 *
 * @property prisonerSearchClient Client for retrieving basic prisoner information and incentives
 * @property btPinPhoneClient Client for retrieving prisoner BT PinPhone balance information
 * @property prisonFinanceClient Client for retrieving prisoner finance information
 */
@Service
class PinPhonePrisonerEnrichmentService(
  private val prisonerSearchClient: PrisonerSearchClient,
  private val btPinPhoneClient: BtPinPhoneClient,
  private val prisonFinanceClient: PrisonFinanceClient,
) {

  /**
   * @param prisonerNumber The unique identifier for the prisoner
   * @return A Mono emitting an [EnrichedPinPhonePrisonerDto] containing aggregated prisoner data
   */
  fun getEnrichedPrisoner(prisonerNumber: String): Mono<EnrichedPinPhonePrisonerDto> {
    val prisonerMono = prisonerSearchClient.getPrisoner(prisonerNumber).cache()

    val bookingIdMono: Mono<String> = prisonerMono.mapNotNull { it.bookingId }

    val balanceMono = bookingIdMono
      .flatMap { bookingId -> prisonFinanceClient.getPrisonerBalance(bookingId) }

    val reference = UUID.randomUUID().toString().replace("-", "").take(20)
    val btBalanceMono =
      btPinPhoneClient.getPrisonerBalance(BtPinPhoneBalanceRequest(reference, prisonerNumber))

    return Mono.zip(prisonerMono, balanceMono, btBalanceMono)
      .map { tuple ->
        EnrichedPinPhonePrisonerDto(
          prisoner = tuple.t1.toPrisonerSearchResponseDto(),
          prisonerBalance = tuple.t2.toBalanceResponseDto(),
          prisonerBtBalance = tuple.t3.toBtPinPhoneResponseDto(),
        )
      }
  }

  data class EnrichedPinPhonePrisonerDto(
    val prisoner: PrisonerSearchResponseDto,
    val prisonerBalance: BalanceResponseDto?,
    val prisonerBtBalance: BtPinPhoneResponseDto?,
  )
}
