package com.nielsen.api

import com.nielsen.service.AccountService
import com.nielsen.model.Account
import com.nielsen.model.Amount
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.route
import org.kodein.di.Kodein
import org.kodein.di.generic.instance
import java.math.BigDecimal
import java.util.*

fun Route.accountApi(kodein: Kodein) {
    val accountService by kodein.instance<AccountService>()
    route("/accounts") {

        post("/") {
            val createAccountRequest = call.receive(CreateAccountRequest::class)
            val newAccount = Account(Account.Id(UUID.fromString(createAccountRequest.id)))
            accountService.insert(newAccount)
            call.respond(HttpStatusCode.Created, AccountResponse.from(newAccount))
        }

        get("/{accountId}") {
            val accountId = Account.Id(UUID.fromString(call.parameters["accountId"]))
            val foundAccount = accountService.getAccountById(accountId)

            call.respond(HttpStatusCode.OK, AccountResponse.from(foundAccount))
        }

        post("/{accountId}/deposit") {
            val accountId = Account.Id(UUID.fromString(call.parameters["accountId"]))
            val depositRequest = call.receive(DepositRequest::class)
            accountService.deposit(accountId, Amount(depositRequest.amount.toBigDecimal()))

            call.respond(HttpStatusCode.OK)
        }

        post("/{accountId}/withdraw") {
            val accountId = Account.Id(UUID.fromString(call.parameters["accountId"]))
            val withdrawRequest = call.receive(WithdrawRequest::class)
            accountService.withdraw(accountId, Amount(withdrawRequest.amount.toBigDecimal()))

            call.respond(HttpStatusCode.OK)
        }

        post("/{accountId}/transfer") {
            val accountId = Account.Id(UUID.fromString(call.parameters["accountId"]))
            val transferRequest = call.receive(TransferRequest::class)
            accountService.transfer(
                sourceAccountId = accountId,
                destinationAccountId = Account.Id(transferRequest.destinationAccountId),
                amount = Amount(transferRequest.amount.toBigDecimal())
            )

            call.respond(HttpStatusCode.OK)
        }
    }
}

data class CreateAccountRequest(val id: String)

data class DepositRequest(val amount: Double)

data class WithdrawRequest(val amount: Double)

data class TransferRequest(val destinationAccountId: UUID, val amount: Double)

data class AccountResponse(val id: UUID, val balance: BigDecimal) {
    companion object {
        fun from(account: Account): AccountResponse {
            return AccountResponse(
                id = account.id.value,
                balance = account.balance.value
            )
        }
    }
}