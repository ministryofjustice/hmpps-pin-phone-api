package uk.gov.justice.digital.hmpps.pinphoneapi.service.pinphonecontacts

import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.pinphoneapi.client.btPinPhoneClient.BtPinPhoneClient
import uk.gov.justice.digital.hmpps.pinphoneapi.client.btPinPhoneClient.generated.BtPinPhoneControlledNumbersRequest
import uk.gov.justice.digital.hmpps.pinphoneapi.mapping.BtPinPhoneContactDto
import uk.gov.justice.digital.hmpps.pinphoneapi.mapping.toContactDto
import java.util.UUID

/**
 * Service responsible for retrieving prisoner contacts from BT PinPhone
 *
 * @property btPinPhoneClient Client for retrieving prisoner contact information
 */
@Service
class BtPinPhoneContactsService(
  private val btPinPhoneClient: BtPinPhoneClient,
) {

  /**
   * @param prisonerNumber The unique identifier for the prisoner
   * @return A Mono emitting an [BtPinPhoneContactDto] containing list of prisoner contacts
   */
  fun getPrisonerContacts(prisonerNumber: String): Mono<List<BtPinPhoneContactDto>> {
    val reference = UUID.randomUUID().toString().replace("-", "").take(20)
    return btPinPhoneClient.getPrisonerContacts(
      BtPinPhoneControlledNumbersRequest(reference, prisonerNumber),
    )
      .map { response ->
        response.controlledNumbers.map { it.toContactDto(response.prisonerId) }
      }
  }
}
