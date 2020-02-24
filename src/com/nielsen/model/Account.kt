package com.nielsen.model

import java.math.BigDecimal
import java.util.UUID

data class Account(
    val id: Id,
    val balance: Balance = Balance(BigDecimal.ZERO.setScale(2))
) {
    data class Id(val value: UUID)
    data class Balance(val value: BigDecimal)
}

data class AccountData(
    val account: Account,
    val deduplicationIds: Set<DeduplicationId>
)

data class DeduplicationId(val value: UUID)
