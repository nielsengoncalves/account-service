package com.nielsen.model

import java.math.BigDecimal
import java.util.*

data class Account(val id: Id, val balance: Balance = Balance(BigDecimal.ZERO)) {
    data class Id(val value: UUID)
    data class Balance(val value: BigDecimal)
}