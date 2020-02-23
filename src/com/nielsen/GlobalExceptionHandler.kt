package com.nielsen

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.exc.InvalidFormatException
import com.nielsen.service.AccountNotFoundException
import com.nielsen.service.DuplicateAccountException
import com.nielsen.service.InsufficientFundsException
import com.nielsen.service.TransferNotAllowedException
import io.ktor.application.call
import io.ktor.features.StatusPages
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import org.slf4j.LoggerFactory

fun StatusPages.Configuration.globalExceptionHandler() {
    val log = LoggerFactory.getLogger(this.javaClass)

    exception<TransferNotAllowedException> { e ->
        call.respond(HttpStatusCode.BadRequest, e.localizedMessage)
    }
    exception<DuplicateAccountException> { e ->
        call.respond(HttpStatusCode.Conflict, e.localizedMessage)
    }
    exception<InsufficientFundsException> { e ->
        call.respond(HttpStatusCode.BadRequest, e.localizedMessage)
    }
    exception<InsufficientFundsException> { e ->
        call.respond(HttpStatusCode.BadRequest, e.localizedMessage)
    }
    exception<InvalidFormatException> { e ->
        call.respond(HttpStatusCode.BadRequest, e.localizedMessage)
    }
    exception<JsonParseException> { e ->
        call.respond(HttpStatusCode.BadRequest, e.localizedMessage)
    }
    exception<AccountNotFoundException> {
        call.respond(HttpStatusCode.NotFound)
    }
    exception<Throwable> { e ->
        log.error("Internal Server Error:", e)
        call.respond(HttpStatusCode.InternalServerError)
    }
}