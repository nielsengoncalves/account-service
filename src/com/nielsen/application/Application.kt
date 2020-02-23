package com.nielsen.application

import com.nielsen.api.accountApi
import com.nielsen.service.AccountService
import io.ktor.application.Application
import io.ktor.application.install
import io.ktor.features.CallLogging
import io.ktor.features.ContentNegotiation
import io.ktor.features.StatusPages
import io.ktor.jackson.jackson
import io.ktor.request.path
import io.ktor.routing.routing
import org.kodein.di.Kodein
import org.kodein.di.generic.bind
import org.kodein.di.generic.instance
import org.kodein.di.generic.singleton
import org.slf4j.event.Level

fun Application.main() {
    install(StatusPages) {
        globalExceptionHandler()
    }
    install(CallLogging) {
        level = Level.INFO
        filter { call -> call.request.path().startsWith("/") }
    }
    install(ContentNegotiation) {
        jackson()
    }
    val kodein = buildDIContainer()
    routing {
        accountApi(kodein)
    }
}

private fun buildDIContainer(): Kodein = Kodein {
    bind<AccountDatabase>() with singleton { AccountDatabase() }
    bind<AccountService>() with singleton { AccountService(instance()) }
}