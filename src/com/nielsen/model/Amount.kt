package com.nielsen.model

import java.math.BigDecimal
import java.math.RoundingMode

data class Amount private constructor(val value: BigDecimal) {
    companion object {
        fun from(value: BigDecimal): Amount {
            val fixedValue = value.setScale(2, RoundingMode.HALF_UP)

            if (fixedValue.compareTo(BigDecimal.ZERO) != 1) {
                throw IllegalArgumentException("Amount must be greater than 0.")
            }

            return Amount(fixedValue)
        }
    }
}