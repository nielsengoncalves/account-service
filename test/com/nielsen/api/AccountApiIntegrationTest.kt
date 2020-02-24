package com.nielsen.api

import com.nielsen.application.main
import com.nielsen.shouldHaveResponse
import io.kotlintest.assertions.ktor.shouldHaveStatus
import io.ktor.application.Application
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.TestApplicationEngine
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.setBody
import io.ktor.server.testing.withTestApplication
import java.util.UUID
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class AccountApiIntegrationTest {

    companion object {
        const val APPLICATION_JSON = "application/json"
    }

    @Nested
    inner class CreateAccount {

        @Test
        fun `should return created when account is created`() {
            withTestApplication(Application::main) {
                val accountId = UUID.fromString("9e35e732-3849-4aa8-9073-8626da5d18c6")

                handleRequest(HttpMethod.Post, "/accounts") {
                    addHeader(HttpHeaders.ContentType, APPLICATION_JSON)
                    setBody(
                        """
                        {
                            "id": "$accountId"
                        }
                        """.trimIndent()
                    )
                }.apply {
                    response.shouldHaveStatus(HttpStatusCode.Created)
                    response.shouldHaveResponse(
                        """
                        {
                            "id":"$accountId",
                            "balance":0.00
                        }
                        """.trimIndent()
                    )
                }
            }
        }

        @Test
        fun `should return conflict when account already exists`() {
            withTestApplication(Application::main) {
                val accountId = UUID.fromString("3f4d9696-f2f6-4ea9-b211-b77c58cef0e3")
                saveAccount(accountId)

                handleRequest(HttpMethod.Post, "/accounts") {
                    addHeader(HttpHeaders.ContentType, APPLICATION_JSON)
                    setBody(
                        """
                        {
                            "id": "$accountId"
                        }
                        """.trimIndent()
                    )
                }.apply {
                    response.shouldHaveStatus(HttpStatusCode.Conflict)
                }
            }
        }
    }

    @Nested
    inner class FindAccount {

        @Test
        fun `should return ok when account is found`() {
            withTestApplication(Application::main) {
                val accountId = UUID.fromString("3f4d9696-f2f6-4ea9-b211-b77c58cef0e3")
                saveAccount(accountId)

                handleRequest(HttpMethod.Get, "/accounts/$accountId").apply {
                    response.shouldHaveStatus(HttpStatusCode.OK)
                    response.shouldHaveResponse(
                        """
                        {
                            "id": "$accountId",
                            "balance": 0.00
                        }    
                        """.trimIndent()
                    )
                }
            }
        }

        @Test
        fun `should return not-found when account doesn't exist`() {
            withTestApplication(Application::main) {
                val accountId = UUID.fromString("3f4d9696-f2f6-4ea9-b211-b77c58cef0e3")

                handleRequest(HttpMethod.Get, "/accounts/$accountId").apply {
                    response.shouldHaveStatus(HttpStatusCode.NotFound)
                }
            }
        }
    }

    @Nested
    inner class Deposit {
        @Test
        fun `should return ok when deposit succeeds`() {
            withTestApplication(Application::main) {
                val accountId = UUID.fromString("3f4d9696-f2f6-4ea9-b211-b77c58cef0e3")
                val deduplicationId = UUID.fromString("b8a99111-75a7-424c-84ff-8acbd0748160")
                saveAccount(accountId)

                handleRequest(HttpMethod.Post, "/accounts/$accountId/deposit") {
                    addHeader(HttpHeaders.ContentType, APPLICATION_JSON)
                    setBody(
                        """
                        {
                            "amount": "99.90", 
                            "deduplicationId": "$deduplicationId"
                        }
                        """.trimIndent()
                    )
                }.apply {
                    response.shouldHaveStatus(HttpStatusCode.OK)
                    response.shouldHaveResponse(
                        """
                        {
                            "id": "$accountId",
                            "balance": 99.90
                        }    
                        """.trimIndent()
                    )
                }
            }
        }

        @Test
        fun `should return bad request when amount is invalid`() {
            withTestApplication(Application::main) {
                val accountId = UUID.fromString("3f4d9696-f2f6-4ea9-b211-b77c58cef0e3")
                val deduplicationId = UUID.fromString("b8a99111-75a7-424c-84ff-8acbd0748160")
                saveAccount(accountId)

                handleRequest(HttpMethod.Post, "/accounts/$accountId/deposit") {
                    addHeader(HttpHeaders.ContentType, APPLICATION_JSON)
                    setBody(
                        """
                        {
                            "amount": "-99.90", 
                            "deduplicationId": "$deduplicationId"
                        }
                        """.trimIndent()
                    )
                }.apply {
                    response.shouldHaveStatus(HttpStatusCode.BadRequest)
                }
            }
        }
    }

    @Nested
    inner class Withdraw {
        @Test
        fun `should return ok when withdraw succeeds`() {
            withTestApplication(Application::main) {
                val accountId = UUID.fromString("3f4d9696-f2f6-4ea9-b211-b77c58cef0e3")
                val deduplicationId = UUID.fromString("b8a99111-75a7-424c-84ff-8acbd0748160")
                saveAccount(accountId)
                deposit(accountId, "1000.00")

                handleRequest(HttpMethod.Post, "/accounts/$accountId/withdraw") {
                    addHeader(HttpHeaders.ContentType, APPLICATION_JSON)
                    setBody(
                        """
                        {
                            "amount": "500.00", 
                            "deduplicationId": "$deduplicationId"
                        }
                        """.trimIndent()
                    )
                }.apply {
                    response.shouldHaveStatus(HttpStatusCode.OK)
                    response.shouldHaveResponse(
                        """
                        {
                            "id": "$accountId",
                            "balance": 500.00
                        }    
                        """.trimIndent()
                    )
                }
            }
        }

        @Test
        fun `should return bad request when amount is invalid`() {
            withTestApplication(Application::main) {
                val accountId = UUID.fromString("3f4d9696-f2f6-4ea9-b211-b77c58cef0e3")
                val deduplicationId = UUID.fromString("b8a99111-75a7-424c-84ff-8acbd0748160")
                saveAccount(accountId)
                deposit(accountId, "1000.00")

                handleRequest(HttpMethod.Post, "/accounts/$accountId/withdraw") {
                    addHeader(HttpHeaders.ContentType, APPLICATION_JSON)
                    setBody(
                        """
                        {
                            "amount": "-500.00", 
                            "deduplicationId": "$deduplicationId"
                        }
                        """.trimIndent()
                    )
                }.apply {
                    response.shouldHaveStatus(HttpStatusCode.BadRequest)
                }
            }
        }
    }

    @Nested
    inner class Transfer {
        @Test
        fun `should return ok when transfer succeeds`() {
            withTestApplication(Application::main) {
                val sourceAccountId = UUID.fromString("3f4d9696-f2f6-4ea9-b211-b77c58cef0e3")
                val deduplicationId = UUID.fromString("b8a99111-75a7-424c-84ff-8acbd0748160")
                saveAccount(sourceAccountId)
                deposit(sourceAccountId, "1000.00")

                val destinationAccountId = UUID.fromString("9e35e732-3849-4aa8-9073-8626da5d18c6")
                saveAccount(destinationAccountId)

                handleRequest(HttpMethod.Post, "/accounts/$sourceAccountId/transfer") {
                    addHeader(HttpHeaders.ContentType, APPLICATION_JSON)
                    setBody(
                        """
                        {
                            "destinationAccountId": "$destinationAccountId", 
                            "amount": "100.00", 
                            "deduplicationId": "$deduplicationId"
                        }
                        """.trimIndent()
                    )
                }.apply {
                    response.shouldHaveStatus(HttpStatusCode.OK)
                    response.shouldHaveResponse(
                        """
                        {
                            "id": "$sourceAccountId",
                            "balance": 900.00
                        }    
                        """.trimIndent()
                    )
                }

                handleRequest(HttpMethod.Get, "/accounts/$destinationAccountId").apply {
                    response.shouldHaveStatus(HttpStatusCode.OK)
                    response.shouldHaveResponse(
                        """
                        {
                            "id": "$destinationAccountId",
                            "balance": 100.00
                        }    
                        """.trimIndent()
                    )
                }
            }
        }

        @Test
        fun `should return bad request when user doens't have sufficient funds`() {
            val deduplicationId = UUID.fromString("b8a99111-75a7-424c-84ff-8acbd0748160")
            withTestApplication(Application::main) {
                val sourceAccountId = UUID.fromString("3f4d9696-f2f6-4ea9-b211-b77c58cef0e3")
                saveAccount(sourceAccountId)
                deposit(sourceAccountId, "1000.00")

                val destinationAccountId = UUID.fromString("9e35e732-3849-4aa8-9073-8626da5d18c6")
                saveAccount(destinationAccountId)

                handleRequest(HttpMethod.Post, "/accounts/$sourceAccountId/transfer") {
                    addHeader(HttpHeaders.ContentType, APPLICATION_JSON)
                    setBody(
                        """
                        {
                            "destinationAccountId": "$destinationAccountId", 
                            "amount": "1001.00", 
                            "deduplicationId": "$deduplicationId"
                        }
                        """.trimIndent()
                    )
                }.apply {
                    response.shouldHaveStatus(HttpStatusCode.BadRequest)
                }
            }
        }
    }

    private fun TestApplicationEngine.saveAccount(accountId: UUID) {
        handleRequest(HttpMethod.Post, "/accounts") {
            addHeader(HttpHeaders.ContentType, APPLICATION_JSON)
            setBody(
                """
                {
                    "id": "$accountId"
                }
                """.trimIndent()
            )
        }.apply {
            response.shouldHaveStatus(HttpStatusCode.Created)
            response.shouldHaveResponse(
                """
                {
                    "id":"$accountId",
                    "balance":0.00
                }
                """.trimIndent()
            )
        }
    }

    private fun TestApplicationEngine.deposit(accountId: UUID, amount: String) {
        val deduplicationId = UUID.randomUUID()

        handleRequest(HttpMethod.Post, "/accounts/$accountId/deposit") {
            addHeader(HttpHeaders.ContentType, APPLICATION_JSON)
            setBody(
                """
                {
                    "amount": "$amount", 
                    "deduplicationId": "$deduplicationId"
                }
                """.trimIndent()
            )
        }.apply {
            response.shouldHaveStatus(HttpStatusCode.OK)
            response.shouldHaveResponse(
                """
                {
                    "id": "$accountId",
                    "balance": $amount
                }    
                """.trimIndent()
            )
        }
    }
}
