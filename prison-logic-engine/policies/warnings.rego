package domains.product
import data.domains.validation.global_product


import rego.v1

warning_messages contains
"Proposed balance exceeds phone credit limit" if {
       credit_limit_exceeded
       data.prisons[input.prisoner.prisonId]
}

