package domains.product

import rego.v1

import data.domains.rules.final_decision

#
# Default - visible
#
default hidden := false

#
# Hide when final decision is DENY
#
hidden := true if {
    final_decision == "DENY"
}