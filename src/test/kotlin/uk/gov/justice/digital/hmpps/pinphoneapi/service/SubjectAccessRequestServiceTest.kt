package uk.gov.justice.digital.hmpps.pinphoneapi.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import org.junit.jupiter.api.assertNull
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.pinphoneapi.client.medusaapiclient.generated.SarRequestContent
import uk.gov.justice.digital.hmpps.pinphoneapi.client.medusaapiclient.generated.SarRequestResponse
import uk.gov.justice.digital.hmpps.pinphoneapi.client.medusaclient.MedusaInternalClient
import uk.gov.justice.digital.hmpps.pinphoneapi.model.sar.SubjectAccessResponseData
import uk.gov.justice.digital.hmpps.pinphoneapi.service.sar.SubjectAccessRequestService
import java.time.LocalDate
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
class SubjectAccessRequestServiceTest {
  private val medusaStoreClient: MedusaInternalClient = mock()

  private lateinit var service: SubjectAccessRequestService

  @BeforeEach
  fun setUp() {
    service = SubjectAccessRequestService(
      medusaStoreClient,
    )
  }

  @Test
  fun `Get SAR report for the prisoner`() {
    whenever(
      medusaStoreClient.getSarReport(
        "MDI",
        LocalDate.of(2026, 1, 1),
        LocalDate.of(2026, 1, 15),
      ),
    ).thenReturn(
      SarRequestResponse(
        listOf(
          SarRequestContent(
            prisonCode = "MDI",
            orderStatus = SarRequestContent.OrderStatus.completed,
            orderCreatedAt = LocalDateTime.now(),
            orderUpdatedAt = LocalDateTime.now(),
            amountInPounds = "5",
            rawAmount = "value : 0.01 and precison : 20",
            paymentStatus = SarRequestContent.PaymentStatus.AUTHORIZED,
            paymentAuthorizedAt = LocalDateTime.now(),
            paymentCreatedAt = LocalDateTime.now(),
            paymentUpdatedAt = LocalDateTime.now(),
            amountInPence = "500",
            paymentOutcome = SarRequestContent.PaymentOutcome.AUTHORIZED,
            btCreditLimit = "500",
            btBalanceBeforeTransaction = "1000",
            btBalanceAfterTransaction = "1500",
            errorCode = "",
            errorDescription = "",
          ),
        ),
      ),
    )
    val result = service.getPrisonContentFor(
      "MDI",
      LocalDate.of(2026, 1, 1),
      LocalDate.of(2026, 1, 15),
    )

    assertNotNull(result)
    val sarContent = assertInstanceOf(SubjectAccessResponseData::class.java, result!!.content)

    assertEquals("MDI", sarContent.prisonerNumber)
  }

  @Test
  fun `Get empty SAR report for the prisoner`() {
    whenever(
      medusaStoreClient.getSarReport(
        "CKI",
        LocalDate.of(2026, 1, 1),
        LocalDate.of(2026, 1, 15),
      ),
    ).thenReturn(
      SarRequestResponse(
        emptyList(),
      ),
    )
    val result = service.getPrisonContentFor(
      "CKI",
      LocalDate.of(2026, 1, 1),
      LocalDate.of(2026, 1, 15),
    )

    assertNull(result)
  }
}
