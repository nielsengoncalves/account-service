package com.nielsen.model

import io.kotlintest.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class AmountTest {

    @ParameterizedTest
    @ValueSource(strings = ["-1", "0"])
    fun `should not create object with negative numbers and zero`(value: String) {
        assertThrows<IllegalArgumentException> {
            Amount.from(value.toBigDecimal())
        }
    }

    @Test
    fun `should create object for positive numbers and fix the scale`() {
        val actualAmount = Amount.from("1".toBigDecimal())

        actualAmount shouldBe Amount.from("1.00".toBigDecimal())
    }
}