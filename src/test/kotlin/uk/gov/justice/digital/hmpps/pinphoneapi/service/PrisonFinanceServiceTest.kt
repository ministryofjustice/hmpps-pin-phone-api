package uk.gov.justice.digital.hmpps.pinphoneapi.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.http.ResponseEntity
import uk.gov.justice.digital.hmpps.pinphoneapi.client.prisonfinance.PrisonFinanceClient
import uk.gov.justice.digital.hmpps.pinphoneapi.client.prisonfinance.generated.AddHoldTransaction
import uk.gov.justice.digital.hmpps.pinphoneapi.client.prisonfinance.generated.HoldDetails
import uk.gov.justice.digital.hmpps.pinphoneapi.client.prisonfinance.generated.ReleaseHoldAndCreateTransaction
import uk.gov.justice.digital.hmpps.pinphoneapi.client.prisonfinance.generated.ReleaseHoldTransaction
import uk.gov.justice.digital.hmpps.pinphoneapi.client.prisonfinance.generated.Transaction
import uk.gov.justice.digital.hmpps.pinphoneapi.integration.HOLD_NUMBER
import uk.gov.justice.digital.hmpps.pinphoneapi.integration.PRISONER_ID
import uk.gov.justice.digital.hmpps.pinphoneapi.integration.PRISONER_NUMBER

class PrisonFinanceServiceTest {

  private val prisonFinanceClient: PrisonFinanceClient = mock()
  private val service = PrisonFinanceService(prisonFinanceClient)

  @Test
  fun `addHold builds request correctly and calls client`() {
    // Given
    val prisonId = PRISONER_ID
    val offenderNo = PRISONER_NUMBER
    // val clientRequest = AddHoldClientRequest(amount = 10.5)

    val expectedResponse = HoldDetails(holdNumber = 123)
    whenever(prisonFinanceClient.addHold(any(), any(), any()))
      .thenReturn(expectedResponse)

    // When
    val result = service.addHold(prisonId, offenderNo, 1050)

    // Then
    val captor = argumentCaptor<AddHoldTransaction>()

    verify(prisonFinanceClient).addHold(
      eq(prisonId),
      eq(offenderNo),
      captor.capture(),
    )

    val request = captor.firstValue

    // Static fields
    assertEquals("HOLD", request.description)
    assertEquals(1050, request.amount)
    assertEquals(offenderNo, request.clientName)

    // Generated fields
    assertTrue(request.clientTransactionId.length > "CLIENT-".length)
    assertTrue(request.clientUniqueReference.length > "CLIENT-".length)

    // Response passthrough
    assertEquals(expectedResponse, result)
  }

  @Test
  fun `releaseHold builds request correctly and calls client`() {
    // Given
    val prisonId = PRISONER_ID
    val offenderNo = PRISONER_NUMBER
    val holdNumber = HOLD_NUMBER

    val expectedResponse = ResponseEntity.noContent().build<Void>()
    whenever(prisonFinanceClient.releaseHold(any(), any(), any(), any()))
      .thenReturn(expectedResponse)

    // When
    val result = service.releaseHold(prisonId, offenderNo, holdNumber)

    // Then
    val captor = argumentCaptor<ReleaseHoldTransaction>()

    verify(prisonFinanceClient).releaseHold(
      eq(prisonId),
      eq(offenderNo),
      eq(holdNumber),
      captor.capture(),
    )

    val request = captor.firstValue

    // Static fields
    assertEquals("Remove HOLD", request.description)
    assertEquals(offenderNo, request.clientName)

    // Response passthrough
    assertEquals(expectedResponse, result)
  }

  @Test
  fun `releaseHoldCreateTransaction builds request correctly and calls client`() {
    // Given
    val prisonId = PRISONER_ID
    val offenderNo = PRISONER_NUMBER
    val holdNumber = HOLD_NUMBER
    val clientRequest = ReleaseHoldAndCreateTransaction(
      type = ReleaseHoldAndCreateTransaction.Type.PHONE,
      createDescription = "Pin phone credit",
      clientTransactionId = "test-transaction-id",
      removeClientUniqueReference = "test-remove-ref",
      createClientUniqueReference = "test-create-ref",
    )

    val expectedResponse = Transaction(id = "111-1")
    whenever(prisonFinanceClient.releaseHoldCreateTransaction(any(), any(), any(), any()))
      .thenReturn(expectedResponse)

    // When
    val result = service.releaseHoldAndCreateTransaction(prisonId, offenderNo, holdNumber, clientRequest.type)

    // Then
    val captor = argumentCaptor<ReleaseHoldAndCreateTransaction>()

    verify(prisonFinanceClient).releaseHoldCreateTransaction(
      eq(prisonId),
      eq(offenderNo),
      eq(holdNumber),
      captor.capture(),
    )

    val request = captor.firstValue

    // Static fields
    assertEquals("Remove HOLD", request.removeDescription)
    assertEquals(request.type, request.type)
    assertEquals("HOLD for PHONE", request.createDescription)
    assertEquals(offenderNo, request.clientName)

    // Response passthrough
    assertEquals(expectedResponse, result)
  }
}
