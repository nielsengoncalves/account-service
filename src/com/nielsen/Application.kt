package com.nielsen

import com.nielsen.api.accountApi
import com.nielsen.service.AccountService
import com.nielsen.service.AccountServiceImpl
import io.ktor.application.Application
import io.ktor.application.install
import io.ktor.features.ContentNegotiation
import io.ktor.features.StatusPages
import io.ktor.jackson.jackson
import io.ktor.routing.routing
import org.kodein.di.Kodein
import org.kodein.di.generic.bind
import org.kodein.di.generic.instance
import org.kodein.di.generic.singleton

fun Application.main() {
    install(StatusPages) {
        globalExceptionHandler()
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
    bind<AccountsDatabase>() with singleton { AccountsDatabase() }
    bind<AccountService>() with singleton { AccountServiceImpl(instance()) }
}