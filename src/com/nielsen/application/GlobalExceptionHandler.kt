package com.nielsen

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.exc.InvalidFormatException
import io.ktor.application.call
import io.ktor.features.StatusPages
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import org.slf4j.LoggerFactory

fun StatusPages.Configuration.globalExceptionHandler() {
    val log = LoggerFactory.getLogger(this.javaClass)

    exception<BadRequestException> { e ->
        call.respond(HttpStatusCode.BadRequest, ErrorResponse(e.localizedMessage))
    }
    exception<DuplicateAccountException> { e ->
        call.respond(HttpStatusCode.Conflict, ErrorResponse(e.localizedMessage))
    }
    exception<BadRequestException> { e ->
        call.respond(HttpStatusCode.BadRequest, ErrorResponse(e.localizedMessage))
    }
    exception<InvalidFormatException> { e ->
        call.respond(HttpStatusCode.BadRequest, ErrorResponse(e.localizedMessage))
    }
    exception<JsonParseException> { e ->
        call.respond(HttpStatusCode.BadRequest, ErrorResponse(e.localizedMessage))
    }
    exception<IllegalArgumentException> { e ->
        call.respond(HttpStatusCode.BadRequest, ErrorResponse(e.localizedMessage))
    }
    exception<AccountNotFoundException> {
        call.respond(HttpStatusCode.NotFound)
    }
    exception<Throwable> { e ->
        log.error("Internal Server Error:", e)
        call.respond(HttpStatusCode.InternalServerError)
    }
}

data class ErrorResponse(val message: String)