package domains.rules

import rego.v1
import data.domains.rules.scope.rule_applies

applicable_rules contains rule.ruleId if {

    rule := data.rules[_]

}