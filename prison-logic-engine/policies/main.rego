package app.main

import rego.v1


import data.domains.validation.errors
import data.domains.rules.final_decision

import data.domains.product.maximum_credit
import data.domains.product.effective_limit
import data.domains.product.credit_limit_enabled
import data.domains.product.allowed_fund_sources
import data.domains.product.hidden
import data.domains.product.warning_messages




response := {
    "decision": final_decision,
    "creditLimit": effective_limit,
    "hidden": hidden,
    "warnings": warning_messages,
    "maxAvailableCredit": maximum_credit,
}
