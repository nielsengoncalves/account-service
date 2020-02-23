package com.nielsen.api

import com.nielsen.application.main
import io.kotlintest.shouldBe
import io.ktor.application.Application
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.TestApplicationEngine
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.setBody
import io.ktor.server.testing.withTestApplication
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.*

class AccountApiIntegrationTest {

    @Nested
    inner class CreateAccount {

        @Test
        fun `should return created when account is created `() = withTestApplication(Application::main) {
            val accountId = UUID.fromString("9e35e732-3849-4aa8-9073-8626da5d18c6")
            with(handleRequest(HttpMethod.Post, "/accounts") {
                setBody("{\"id\": \"$accountId\"}")
                addHeader(HttpHeaders.ContentType, "application/json")
            }) {
                response.status() shouldBe HttpStatusCode.Created
                response.content shouldBe "{\"id\":\"$accountId\",\"balance\":0.00}"
            }
        }

        @Test
        fun `should return conflict when account already exists`() = withTestApplication(Application::main) {
            val accountId = UUID.fromString("3f4d9696-f2f6-4ea9-b211-b77c58cef0e3")
            saveAccount(accountId)

            with(handleRequest(HttpMethod.Post, "/accounts") {
                setBody("{\"id\": \"$accountId\"}")
                addHeader(HttpHeaders.ContentType, "application/json")
            }) {
                response.status() shouldBe HttpStatusCode.Conflict
            }
        }
    }

    @Nested
    inner class FindAccount {

        @Test
        fun `should return ok when account is found`() = withTestApplication(Application::main) {
            val accountId = UUID.fromString("3f4d9696-f2f6-4ea9-b211-b77c58cef0e3")
            saveAccount(accountId)

            with(handleRequest(HttpMethod.Get, "/accounts/$accountId")) {
                response.status() shouldBe HttpStatusCode.OK
                response.content shouldBe "{\"id\":\"$accountId\",\"balance\":0.00}"
            }
        }

        @Test
        fun `should return not-found when account doesn't exist`() = withTestApplication(Application::main) {
            val accountId = UUID.fromString("3f4d9696-f2f6-4ea9-b211-b77c58cef0e3")

            with(handleRequest(HttpMethod.Get, "/accounts/$accountId")) {
                response.status() shouldBe HttpStatusCode.NotFound
            }
        }
    }

    @Nested
    inner class Deposit {
        @Test
        fun `should return ok when deposit succeeds`() = withTestApplication(Application::main) {
            val accountId = UUID.fromString("3f4d9696-f2f6-4ea9-b211-b77c58cef0e3")
            saveAccount(accountId)

            with(handleRequest(HttpMethod.Post, "/accounts/$accountId/deposit") {
                setBody("{\"amount\": \"99.90\"}")
                addHeader(HttpHeaders.ContentType, "application/json")
            }) {
                response.status() shouldBe HttpStatusCode.OK
                response.content shouldBe "{\"id\":\"$accountId\",\"balance\":99.90}"
            }
        }

        @Test
        fun `should return bad request when amount is invalid`() = withTestApplication(Application::main) {
            val accountId = UUID.fromString("3f4d9696-f2f6-4ea9-b211-b77c58cef0e3")
            saveAccount(accountId)

            with(handleRequest(HttpMethod.Post, "/accounts/$accountId/deposit") {
                setBody("{\"amount\": \"-99.90\"}")
                addHeader(HttpHeaders.ContentType, "application/json")
            }) {
                response.status() shouldBe HttpStatusCode.BadRequest
            }
        }
    }

    @Nested
    inner class Withdraw {
        @Test
        fun `should return ok when withdraw succeeds`() = withTestApplication(Application::main) {
            val accountId = UUID.fromString("3f4d9696-f2f6-4ea9-b211-b77c58cef0e3")
            saveAccount(accountId)
            deposit(accountId, "1000.00")

            with(handleRequest(HttpMethod.Post, "/accounts/$accountId/withdraw") {
                setBody("{\"amount\": \"500.00\"}")
                addHeader(HttpHeaders.ContentType, "application/json")
            }) {
                response.status() shouldBe HttpStatusCode.OK
                response.content shouldBe "{\"id\":\"$accountId\",\"balance\":500.00}"
            }
        }

        @Test
        fun `should return bad request when amount is invalid`() = withTestApplication(Application::main) {
            val accountId = UUID.fromString("3f4d9696-f2f6-4ea9-b211-b77c58cef0e3")
            saveAccount(accountId)
            deposit(accountId, "1000.00")

            with(handleRequest(HttpMethod.Post, "/accounts/$accountId/withdraw") {
                setBody("{\"amount\": \"-500.00\"}")
                addHeader(HttpHeaders.ContentType, "application/json")
            }) {
                response.status() shouldBe HttpStatusCode.BadRequest
            }
        }
    }

    @Nested
    inner class Transfer {
        @Test
        fun `should return ok when transfer succeeds`() = withTestApplication(Application::main) {
            val sourceAccountId = UUID.fromString("3f4d9696-f2f6-4ea9-b211-b77c58cef0e3")
            saveAccount(sourceAccountId)
            deposit(sourceAccountId, "1000.00")

            val destinationAccountId = UUID.fromString("9e35e732-3849-4aa8-9073-8626da5d18c6")
            saveAccount(destinationAccountId)

            with(handleRequest(HttpMethod.Post, "/accounts/$sourceAccountId/transfer") {
                setBody("{\"destinationAccountId\": \"$destinationAccountId\", \"amount\": \"100.00\"}")
                addHeader(HttpHeaders.ContentType, "application/json")
            }) {
                response.status() shouldBe HttpStatusCode.OK
                response.content shouldBe "{\"id\":\"$sourceAccountId\",\"balance\":900.00}"
            }

            with(handleRequest(HttpMethod.Get, "/accounts/$destinationAccountId")) {
                response.status() shouldBe HttpStatusCode.OK
                response.content shouldBe "{\"id\":\"$destinationAccountId\",\"balance\":100.00}"
            }
        }

        @Test
        fun `should return bad request when user doens't have sufficient funds`() = withTestApplication(Application::main) {
            val sourceAccountId = UUID.fromString("3f4d9696-f2f6-4ea9-b211-b77c58cef0e3")
            saveAccount(sourceAccountId)
            deposit(sourceAccountId, "1000.00")

            val destinationAccountId = UUID.fromString("9e35e732-3849-4aa8-9073-8626da5d18c6")
            saveAccount(destinationAccountId)

            with(handleRequest(HttpMethod.Post, "/accounts/$sourceAccountId/transfer") {
                setBody("{\"destinationAccountId\": \"$destinationAccountId\", \"amount\": \"1001.00\"}")
                addHeader(HttpHeaders.ContentType, "application/json")
            }) {
                response.status() shouldBe HttpStatusCode.BadRequest
            }
        }
    }

    private fun TestApplicationEngine.saveAccount(accountId: UUID) {
        with(handleRequest(HttpMethod.Post, "/accounts") {
            setBody("{\"id\": \"$accountId\"}")
            addHeader(HttpHeaders.ContentType, "application/json")
        }) {
            response.status() shouldBe HttpStatusCode.Created
        }
    }

    private fun TestApplicationEngine.deposit(accountId: UUID, amount: String) {
        with(handleRequest(HttpMethod.Post, "/accounts/$accountId/deposit") {
            setBody("{\"amount\": \"$amount\"}")
            addHeader(HttpHeaders.ContentType, "application/json")
        }) {
            response.status() shouldBe HttpStatusCode.OK
            response.content shouldBe "{\"id\":\"$accountId\",\"balance\":$amount}"
        }
    }
}