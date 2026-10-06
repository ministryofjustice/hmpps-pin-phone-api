package uk.gov.justice.digital.hmpps.pinphoneapi.controller

import io.swagger.v3.oas.annotations.Operation
import org.springframework.context.annotation.Profile
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.pinphoneapi.client.btPinPhoneClient.BtPinPhoneTestSupportClient
import uk.gov.justice.digital.hmpps.pinphoneapi.client.btPinPhoneClient.BtRelationshipsResponse
import uk.gov.justice.digital.hmpps.pinphoneapi.client.btPinPhoneClient.CreateAccountRequest
import uk.gov.justice.digital.hmpps.pinphoneapi.client.btPinPhoneClient.CreateAccountResponse
import uk.gov.justice.digital.hmpps.pinphoneapi.client.btPinPhoneClient.CreateControlledNumberRequest
import uk.gov.justice.digital.hmpps.pinphoneapi.client.btPinPhoneClient.CreateControlledNumberResponse
import uk.gov.justice.digital.hmpps.pinphoneapi.client.btPinPhoneClient.CreditAccountRequest
import uk.gov.justice.digital.hmpps.pinphoneapi.client.btPinPhoneClient.CreditAccountResponse
import uk.gov.justice.digital.hmpps.pinphoneapi.client.btPinPhoneClient.generated.BtPinPhoneBalanceRequest
import uk.gov.justice.digital.hmpps.pinphoneapi.client.btPinPhoneClient.generated.BtPinPhoneBalanceResponse
import uk.gov.justice.digital.hmpps.pinphoneapi.client.prisonfinance.generated.AddHoldTransaction
import uk.gov.justice.digital.hmpps.pinphoneapi.client.prisonfinance.generated.HoldDetails
import uk.gov.justice.digital.hmpps.pinphoneapi.client.prisonfinance.generated.ReleaseHoldAndCreateTransaction
import uk.gov.justice.digital.hmpps.pinphoneapi.client.prisonfinance.generated.Transaction
import uk.gov.justice.digital.hmpps.pinphoneapi.client.productenrichment.dto.ProductDetailsResponse
import uk.gov.justice.digital.hmpps.pinphoneapi.service.PrisonFinanceService
import uk.gov.justice.digital.hmpps.pinphoneapi.service.ProductEnrichmentInfoService

@RestController
@RequestMapping("/api")
@PreAuthorize("hasRole('ROLE_PIN_PHONE_CREDIT_API')")
@Profile("dev", "test")
class TestController(
  private val productEnrichmentInfoService: ProductEnrichmentInfoService,
  private val btPinPhoneTestSupportClient: BtPinPhoneTestSupportClient,
  private val prisonFinanceService: PrisonFinanceService,
) {

  // Product endpoints
  @GetMapping("/product/{ean}")
  fun getProduct(
    @PathVariable ean: String,
  ): Mono<ProductDetailsResponse> = productEnrichmentInfoService.getProductEnrichmentDetails(ean)

  // BT endpoints
  @Operation(summary = "Get BT auth token")
  @GetMapping("/bt-auth-test")
  fun testBtAuth() = btPinPhoneTestSupportClient.getBtToken()

  @Operation(summary = "Get balances for BT account")
  @GetMapping("/get-balances-test/{prisonerId}/{reference}")
  fun testBt(
    @PathVariable prisonerId: String,
    @PathVariable reference: String,
  ): Mono<BtPinPhoneBalanceResponse> = btPinPhoneTestSupportClient.getPrisonerBalance(
    BtPinPhoneBalanceRequest(
      reference = reference,
      prisonerId = prisonerId,
    ),
  )

  @Operation(summary = "Create BT account")
  @PostMapping("/bt-test/account-test")
  fun createBtAccount(
    @RequestBody request: CreateAccountRequest,
  ): Mono<CreateAccountResponse> = btPinPhoneTestSupportClient.createAccount(request)

  @Operation(summary = "Add controlled number to BT account")
  @PostMapping("/bt-test/controlled-number-test")
  fun createBtControlledNumber(
    @RequestBody request: CreateControlledNumberRequest,
  ): Mono<CreateControlledNumberResponse> = btPinPhoneTestSupportClient.createControlledNumber(request)

  @Operation(summary = "Get relationship types for BT")
  @PostMapping("/bt-test/relationships-test")
  fun getBtRelationships(): Mono<BtRelationshipsResponse> = btPinPhoneTestSupportClient.getRelationships()

  @Operation(summary = "Add account credit to BT account")
  @PostMapping("/bt-test/account-credit-test")
  fun creditBtAccount(
    @RequestBody request: CreditAccountRequest,
  ): Mono<CreditAccountResponse> = btPinPhoneTestSupportClient.accountCredit(request)

  // prison finance endpoints
  @PostMapping("/finance/prisons/{prisonId}/offenders/{offenderNo}/addHold")
  fun addHold(
    @PathVariable prisonId: String,
    @PathVariable offenderNo: String,
    @RequestBody request: AddHoldTransaction,
  ): HoldDetails = prisonFinanceService.addHold(prisonId, offenderNo, request.amount)

  @PostMapping("/finance/prisons/{prisonId}/offenders/{offenderNo}/releaseHold/{holdNumber}")
  fun releaseHold(
    @PathVariable prisonId: String,
    @PathVariable offenderNo: String,
    @PathVariable holdNumber: Number,
  ): ResponseEntity<Void> = prisonFinanceService.releaseHold(prisonId, offenderNo, holdNumber)

  @PostMapping("/finance/prisons/{prisonId}/offenders/{offenderNo}/releaseHoldCreateTransaction/{holdNumber}")
  fun releaseHoldAndCreateTransaction(
    @PathVariable prisonId: String,
    @PathVariable offenderNo: String,
    @PathVariable holdNumber: Number,
    @RequestBody request: ReleaseHoldAndCreateTransaction,
  ): Transaction = prisonFinanceService.releaseHoldAndCreateTransaction(prisonId, offenderNo, holdNumber, request.type)
}
