package com.nielsen

import io.kotlintest.assertions.json.shouldMatchJson
import io.kotlintest.matchers.types.shouldNotBeNull
import io.ktor.server.testing.TestApplicationResponse

fun TestApplicationResponse.shouldHaveResponse(json: String) {
    content.shouldNotBeNull()
    content!!.shouldMatchJson(json)
}
