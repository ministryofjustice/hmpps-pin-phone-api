package uk.gov.justice.digital.hmpps.pinphoneapi.mapping

import uk.gov.justice.digital.hmpps.pinphoneapi.client.btPinPhoneClient.generated.ControlledNumber
import uk.gov.justice.digital.hmpps.pinphoneapi.service.pinphonecontacts.BtRelationshipType

fun ControlledNumber.toContactDto(prisonerId: String): BtPinPhoneContactDto {
  val relationship = BtRelationshipType.fromId(relationshipId)
  return BtPinPhoneContactDto(
    prisonerId = prisonerId,
    id = id,
    name = name,
    phoneNumber = phoneNumber,
    controlStatus = controlStatus,
    callAllowed = callAllowed,
    legal = legal,
    allowMonitor = allowMonitor,
    alert = alert,
    override = override,
    contactType = relationship.category.name,
    contactTypeDescription = relationship.description,
  )
}

data class BtPinPhoneContactDto(
  val prisonerId: String,
  val id: Int,
  val name: String,
  val phoneNumber: String,
  val controlStatus: Boolean,
  val callAllowed: Boolean,
  val legal: Boolean,
  val allowMonitor: Boolean,
  val alert: Boolean,
  val override: Boolean,
  val contactType: String,
  val contactTypeDescription: String,
)
