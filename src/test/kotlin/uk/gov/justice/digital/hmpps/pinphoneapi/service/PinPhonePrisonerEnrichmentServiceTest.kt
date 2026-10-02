package uk.gov.justice.digital.hmpps.pinphoneapi.service

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import uk.gov.justice.digital.hmpps.pinphoneapi.client.btPinPhoneClient.BtPinPhoneClient
import uk.gov.justice.digital.hmpps.pinphoneapi.client.prisonersearchclient.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.pinphoneapi.client.prisonfinance.PrisonFinanceClient
import uk.gov.justice.digital.hmpps.pinphoneapi.integration.PinPhoneTestFixture
import uk.gov.justice.digital.hmpps.pinphoneapi.service.pinphoneenrichment.PinPhonePrisonerEnrichmentService

@ExtendWith(MockitoExtension::class)
class PinPhonePrisonerEnrichmentServiceTest {

  @Mock
  lateinit var prisonerSearchClient: PrisonerSearchClient

  @Mock
  lateinit var btPinPhoneClient: BtPinPhoneClient

  @Mock
  lateinit var prisonFinanceClient: PrisonFinanceClient

  private lateinit var service: PinPhonePrisonerEnrichmentService

  @BeforeEach
  fun beforeEach() {
    service = PinPhonePrisonerEnrichmentService(
      prisonerSearchClient,
      btPinPhoneClient,
      prisonFinanceClient,
    )
  }

  @Test
  fun `getEnrichedPrisoner - returns fully enriched prisoner and has adjudications`() {
    val prisoner = PinPhoneTestFixture.Prisoner()
    val prisonerResponse = PinPhoneTestFixture.prisonerSearchResponseDto()
    val prisonerBalance = PinPhoneTestFixture.balanceDto()
    val prisonerBalanceResponse = PinPhoneTestFixture.balanceResponseDto()
    val btPinPhoneBalance = PinPhoneTestFixture.btPinPhoneDto()
    val btPinPhoneResponse = PinPhoneTestFixture.btPinPhoneResponseDto()

    whenever(prisonerSearchClient.getPrisoner(PinPhoneTestFixture.PRISONER_NUMBER))
      .thenReturn(Mono.just(prisoner))
    whenever(btPinPhoneClient.getPrisonerBalance(any()))
      .thenReturn(Mono.just(btPinPhoneBalance))
    whenever(prisonFinanceClient.getPrisonerBalance(PinPhoneTestFixture.BOOKING_ID))
      .thenReturn(Mono.just(prisonerBalance))

    val result = service.getEnrichedPrisoner(PinPhoneTestFixture.PRISONER_NUMBER)

    StepVerifier.create(result)
      .assertNext { enriched ->
        // prisoner search
        Assertions.assertThat(enriched.prisoner).isEqualTo(prisonerResponse)
        // bt pin phone balance
        Assertions.assertThat(enriched.prisonerBtBalance).isEqualTo(btPinPhoneResponse)
        // prisoner finance balance
        Assertions.assertThat(enriched.prisonerBalance).isEqualTo(prisonerBalanceResponse)
      }
      .verifyComplete()
  }

  @ParameterizedTest
  @CsvSource(
    "prisoner-search",
    "bt-failure",
    "finance-failure",
  )
  fun `getEnrichedPrisoner - handles service errors`(failingService: String) {
    val prisoner = PinPhoneTestFixture.Prisoner()
    val prisonerBalance = PinPhoneTestFixture.balanceDto()
    val btPinPhoneBalance = PinPhoneTestFixture.btPinPhoneDto()

    when (failingService) {
      "prisoner-search" -> {
        whenever(prisonerSearchClient.getPrisoner(PinPhoneTestFixture.PRISONER_NUMBER))
          .thenReturn(Mono.error(RuntimeException("Service unavailable")))
        whenever(btPinPhoneClient.getPrisonerBalance(any()))
          .thenReturn(Mono.just(btPinPhoneBalance))
      }
      "bt-failure" -> {
        whenever(prisonerSearchClient.getPrisoner(PinPhoneTestFixture.PRISONER_NUMBER))
          .thenReturn(Mono.just(prisoner))
        whenever(prisonFinanceClient.getPrisonerBalance(PinPhoneTestFixture.BOOKING_ID))
          .thenReturn(Mono.just(prisonerBalance))
        whenever(btPinPhoneClient.getPrisonerBalance(any()))
          .thenReturn(Mono.error(RuntimeException("Service unavailable")))
      }
      "finance-failure" -> {
        whenever(prisonerSearchClient.getPrisoner(PinPhoneTestFixture.PRISONER_NUMBER))
          .thenReturn(Mono.just(prisoner))
        whenever(prisonFinanceClient.getPrisonerBalance(PinPhoneTestFixture.BOOKING_ID))
          .thenReturn(Mono.error(RuntimeException("Service unavailable")))
        whenever(btPinPhoneClient.getPrisonerBalance(any()))
          .thenReturn(Mono.just(btPinPhoneBalance))
      }
    }

    val result = service.getEnrichedPrisoner(PinPhoneTestFixture.PRISONER_NUMBER)

    StepVerifier.create(result)
      .verifyErrorMessage("Service unavailable")
  }
}
