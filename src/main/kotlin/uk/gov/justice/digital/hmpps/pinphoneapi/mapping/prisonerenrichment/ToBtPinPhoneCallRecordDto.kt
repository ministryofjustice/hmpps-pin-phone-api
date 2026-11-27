package uk.gov.justice.digital.hmpps.pinphoneapi.mapping.prisonerenrichment

import uk.gov.justice.digital.hmpps.pinphoneapi.client.btPinPhoneClient.generated.CallRecord
import java.time.LocalDateTime

fun CallRecord.toDto() =
  BtPinPhoneCallRecordDto(
    phoneNumber = phoneNumber,
    transactionStartTime = transactionStartTime,
    duration = duration,
    costPence = costPence,
    balancePence = balancePence,
    type = type,
  )

data class BtPinPhoneCallRecordDto(
  val phoneNumber: String,
  val transactionStartTime: LocalDateTime,
  val duration: Int,
  val costPence: Int,
  val balancePence: Int,
  val type: Int,
)