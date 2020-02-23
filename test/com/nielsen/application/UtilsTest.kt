package com.nielsen.application

import com.nielsen.application.compute
import io.kotlintest.shouldBe
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.concurrent.ConcurrentHashMap

class UtilsTest {

    @Nested
    inner class Compute {

        @Test
        fun `should compute when key is absent`() {
            val concurrentHashMap = ConcurrentHashMap<Int, String>()

            concurrentHashMap.compute(1,
                ifAbsent = { "absent" },
                ifPresent = { _, _ -> "present" }
            )

            concurrentHashMap[1] shouldBe "absent"
        }

        @Test
        fun `should compute when key is present`() {
            val concurrentHashMap = ConcurrentHashMap<Int, String>()
            concurrentHashMap[1] = "existing-value"

            concurrentHashMap.compute(1,
                ifAbsent = { "absent" },
                ifPresent = { _, _ -> "present" }
            )

            concurrentHashMap[1] shouldBe "present"
        }
    }
}