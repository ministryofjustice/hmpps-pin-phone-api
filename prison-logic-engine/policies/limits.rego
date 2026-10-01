package domains.product

import rego.v1

default effective_limit := 0

default maximum_credit := 0
default credit_limit_enabled := false

default credit_limit_exceeded := false

credit_limit_max := 5000

prison_limit := limit if {
    prison := input.prisoner.prisonId

    iep := input.incentives.iepCode

    limit := data.prisons[prison].products["BT_PIN_Phone"][iep].limit
}

#
# Credit limits only apply to BT PIN Phone
#
credit_limit_enabled := true if {
    input.productId == "BT_PIN_Phone"

    not input.prisoner.isFN

    not input.prisoner.isLG
}

effective_limit := limit if {
    input_credit_limit

    limit := input_credit_limit
}

effective_limit := limit if {
    not input_credit_limit

    credit_limit_enabled

    limit := min([
        prison_limit,
        credit_limit_max
    ])
}
proposed_balance := balance if {

    current := object.get(
        input,
        "currentBalance",
        0
    )

    requested := object.get(
        input,
        "creditRequested",
        0
    )

    balance := current + requested
}


credit_limit_exceeded if {

    credit_limit_enabled

    requested := object.get(
        input,
        "creditRequested",
        0
    )

    requested > 0

    proposed_balance > effective_limit
}

input_credit_limit := limit if {
    limit := object.get(input, "creditLimit", 0)
    limit > 0
}

maximum_credit := value if {

    effective_limit > 0

    current := object.get(
        input,
        "currentBalance",
        0
    )

    value := max([
        effective_limit - current,
        0
    ])
}