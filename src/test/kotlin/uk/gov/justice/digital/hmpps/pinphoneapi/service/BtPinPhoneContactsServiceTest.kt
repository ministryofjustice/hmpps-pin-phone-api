package uk.gov.justice.digital.hmpps.pinphoneapi.service

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.pinphoneapi.client.btPinPhoneClient.BtPinPhoneClient
import uk.gov.justice.digital.hmpps.pinphoneapi.client.btPinPhoneClient.generated.BtPinPhoneControlledNumbersResponse
import uk.gov.justice.digital.hmpps.pinphoneapi.client.btPinPhoneClient.generated.ControlledNumber
import uk.gov.justice.digital.hmpps.pinphoneapi.config.UpstreamException
import uk.gov.justice.digital.hmpps.pinphoneapi.integration.PinPhoneTestFixture
import uk.gov.justice.digital.hmpps.pinphoneapi.integration.PinPhoneTestFixture.PRISONER_NUMBER
import uk.gov.justice.digital.hmpps.pinphoneapi.integration.PinPhoneTestFixture.REFERENCE
import uk.gov.justice.digital.hmpps.pinphoneapi.service.pinphonecontacts.BtPinPhoneContactsService

@ExtendWith(MockitoExtension::class)
class BtPinPhoneContactsServiceTest {

  @Mock
  lateinit var btPinPhoneClient: BtPinPhoneClient

  private lateinit var service: BtPinPhoneContactsService

  @BeforeEach
  fun beforeEach() {
    service = BtPinPhoneContactsService(btPinPhoneClient)
  }

  @Test
  fun `returns list of contacts mapped from BT response`() {
    val contactResponse = BtPinPhoneControlledNumbersResponse(
      reference = REFERENCE,
      prisonerId = PRISONER_NUMBER,
      controlledNumbers = PinPhoneTestFixture.contactList,
    )

    whenever(btPinPhoneClient.getPrisonerContacts(any()))
      .thenReturn(Mono.just(contactResponse))

    val result = service.getPrisonerContacts(PRISONER_NUMBER).block()

    assertThat(result).isNotNull
    assertThat(result).hasSize(PinPhoneTestFixture.contactList.size)
    assertThat(result!![0].prisonerId).isEqualTo(PRISONER_NUMBER)
    assertThat(result[0].name).isEqualTo("John Doe")
    assertThat(result[0].phoneNumber).isEqualTo("07700900351")
  }

  @Test
  fun `maps social relationship type correctly, 1 = Mother = SOCIAL`() {
    val contactResponse = BtPinPhoneControlledNumbersResponse(
      reference = REFERENCE,
      prisonerId = PRISONER_NUMBER,
      controlledNumbers = listOf(
        ControlledNumber(
          id = 1,
          name = "Test Contact",
          phoneNumber = "07700900351",
          controlStatus = true,
          callAllowed = true,
          legal = false,
          allowMonitor = false,
          alert = false,
          override = false,
          relationshipId = 1,
        ),
      ),
    )

    whenever(btPinPhoneClient.getPrisonerContacts(any()))
      .thenReturn(Mono.just(contactResponse))

    val result = service.getPrisonerContacts(PRISONER_NUMBER).block()

    assertThat(result!![0].contactType).isEqualTo("SOCIAL")
    assertThat(result[0].contactTypeDescription).isEqualTo("Mother")
  }

  @Test
  fun `maps official relationship type correctly, 28 = Solicitor = PROFESSIONAL`() {
    val contactResponse = BtPinPhoneControlledNumbersResponse(
      reference = REFERENCE,
      prisonerId = PRISONER_NUMBER,
      controlledNumbers = listOf(
        ControlledNumber(
          id = 1,
          name = "Legal Contact",
          phoneNumber = "07700900351",
          controlStatus = true,
          callAllowed = true,
          legal = true,
          allowMonitor = false,
          alert = false,
          override = false,
          relationshipId = 28,
        ),
      ),
    )

    whenever(btPinPhoneClient.getPrisonerContacts(any()))
      .thenReturn(Mono.just(contactResponse))

    val result = service.getPrisonerContacts(PRISONER_NUMBER).block()

    assertThat(result!![0].contactType).isEqualTo("OFFICIAL")
    assertThat(result[0].contactTypeDescription).isEqualTo("Solicitor")
  }

  @Test
  fun `maps unknown relationship id to OTHER`() {
    val contactResponse = BtPinPhoneControlledNumbersResponse(
      reference = REFERENCE,
      prisonerId = PRISONER_NUMBER,
      controlledNumbers = listOf(
        ControlledNumber(
          id = 1,
          name = "Unknown Contact",
          phoneNumber = "07700900351",
          controlStatus = true,
          callAllowed = true,
          legal = false,
          allowMonitor = false,
          alert = false,
          override = false,
          relationshipId = 999,
        ),
      ),
    )

    whenever(btPinPhoneClient.getPrisonerContacts(any()))
      .thenReturn(Mono.just(contactResponse))

    val result = service.getPrisonerContacts(PRISONER_NUMBER).block()

    assertThat(result!![0].contactType).isEqualTo("OTHER")
    assertThat(result[0].contactTypeDescription).isEqualTo("Other")
  }

  @Test
  fun `returns empty list when prisoner has no contacts`() {
    val contactResponse = BtPinPhoneControlledNumbersResponse(
      reference = REFERENCE,
      prisonerId = PRISONER_NUMBER,
      controlledNumbers = emptyList(),
    )

    whenever(btPinPhoneClient.getPrisonerContacts(any()))
      .thenReturn(Mono.just(contactResponse))

    val result = service.getPrisonerContacts(PRISONER_NUMBER).block()

    assertThat(result).isNotNull
    assertThat(result).isEmpty()
  }

  @Test
  fun `Exception when BT client fails`() {
    whenever(btPinPhoneClient.getPrisonerContacts(any()))
      .thenReturn(Mono.error(UpstreamException("BT service unavailable")))

    assertThatThrownBy { service.getPrisonerContacts(PRISONER_NUMBER).block() }
      .isInstanceOf(UpstreamException::class.java)
      .hasMessageContaining("BT service unavailable")
  }
}
