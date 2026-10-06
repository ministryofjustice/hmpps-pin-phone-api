package domains.rules

import rego.v1

import data.domains.product.credit_limit_exceeded
import data.domains.rules.quantity_limit_exceeded

#
# Default decision
#
default final_decision := "ALLOW"


#
# BT Credit limit exceeded
#
final_decision := "DENY" if {
    credit_limit_exceeded
}
