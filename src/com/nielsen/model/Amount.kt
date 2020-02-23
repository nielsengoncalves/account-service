package com.nielsen.model

import java.math.BigDecimal

data class Amount(val value: BigDecimal) {
    init {
        if (value.compareTo(BigDecimal.ZERO) != 1) {
            throw IllegalArgumentException("Amount must be greater than 0.")
        }
    }
}