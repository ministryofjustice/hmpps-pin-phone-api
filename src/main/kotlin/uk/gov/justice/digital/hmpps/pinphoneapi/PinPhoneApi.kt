package uk.gov.justice.digital.hmpps.pinphoneapi

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class PinPhoneApi

fun main(args: Array<String>) {
  runApplication<PinPhoneApi>(*args)
}
