package com.nielsen.service

import com.nielsen.application.AccountDatabase
import com.nielsen.application.AccountNotFoundException
import com.nielsen.application.DuplicateAccountException
import com.nielsen.application.InsufficientFundsException
import com.nielsen.application.TransferNotAllowedException
import com.nielsen.model.Account
import com.nielsen.model.AccountData
import com.nielsen.model.Amount
import com.nielsen.model.DeduplicationId
import io.kotlintest.shouldBe
import io.kotlintest.shouldThrow
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.util.*

class AccountServiceTest {

    private val accountDatabase = AccountDatabase()
    private val accountService = AccountService(accountDatabase)

    @BeforeEach
    fun setup() {
        accountDatabase.clear()
    }

    @Nested
    inner class Insert {

        @Test
        fun `should insert account`() {
            val account = Account(
                id = Account.Id(UUID.fromString("806daf1a-d72b-4edb-845e-7c87497ecffb")),
                balance = Account.Balance("0.00".toBigDecimal())
            )

            val actualAccount = accountService.insert(account)

            actualAccount shouldBe account
        }

        @Test
        fun `should not insert duplicate account`() {
            val account = Account(
                id = Account.Id(UUID.fromString("806daf1a-d72b-4edb-845e-7c87497ecffb")),
                balance = Account.Balance("0.00".toBigDecimal())
            )
            insertAccountData(account)

            shouldThrow<DuplicateAccountException> {
                accountService.insert(account)
            }
        }
    }

    @Nested
    inner class GetAccountById {

        @Test
        fun `should get account by id`() {
            val account = Account(
                id = Account.Id(UUID.fromString("806daf1a-d72b-4edb-845e-7c87497ecffb")),
                balance = Account.Balance("0.00".toBigDecimal())
            )
            insertAccountData(account)

            val actualAccount = accountService.getAccountById(account.id)

            actualAccount shouldBe account
        }

        @Test
        fun `should throw exception when account is not found`() {
            val invalidAccountId = Account.Id(UUID.randomUUID())

            shouldThrow<AccountNotFoundException> {
                accountService.getAccountById(invalidAccountId)
            }
        }
    }

    @Nested
    inner class Deposit {

        @Test
        fun `should deposit some amount`() {
            val deduplicationId = DeduplicationId(UUID.randomUUID())
            val account = Account(
                id = Account.Id(UUID.fromString("806daf1a-d72b-4edb-845e-7c87497ecffb")),
                balance = Account.Balance("0.00".toBigDecimal())
            )
            insertAccountData(account)

            val actualAccountData = accountService.deposit(
                accountId = account.id,
                amount = Amount.from("50".toBigDecimal()),
                deduplicationId = deduplicationId
            )

            actualAccountData shouldBe AccountData(
                account = Account(
                    id = Account.Id(UUID.fromString("806daf1a-d72b-4edb-845e-7c87497ecffb")),
                    balance = Account.Balance("50.00".toBigDecimal())
                ),
                deduplicationIds = setOf(deduplicationId)
            )
        }

        @Test
        fun `should be idempotent and not process duplicate deposit operation`() {
            val deduplicationId = DeduplicationId(UUID.randomUUID())
            val account = Account(
                id = Account.Id(UUID.fromString("806daf1a-d72b-4edb-845e-7c87497ecffb")),
                balance = Account.Balance("50.00".toBigDecimal())
            )
            insertAccountData(account, setOf(deduplicationId))

            val actualAccountData = accountService.deposit(
                accountId = account.id,
                amount = Amount.from("50".toBigDecimal()),
                deduplicationId = deduplicationId
            )

            actualAccountData shouldBe AccountData(
                account = Account(
                    id = Account.Id(UUID.fromString("806daf1a-d72b-4edb-845e-7c87497ecffb")),
                    balance = Account.Balance("50.00".toBigDecimal())
                ),
                deduplicationIds = setOf(deduplicationId)
            )
        }

        @Test
        fun `should not deposit when account is not found`() {
            val deduplicationId = DeduplicationId(UUID.fromString("fdc48190-3254-4d16-ae7b-80c3abafede1"))
            val invalidAccountId = Account.Id(UUID.randomUUID())

            shouldThrow<AccountNotFoundException> {
                accountService.deposit(invalidAccountId, Amount.from("50.00".toBigDecimal()), deduplicationId)
            }
        }

        @ParameterizedTest
        @ValueSource(strings = ["0.0", "-50.5"])
        fun `should not deposit zero and negative amounts`(amount: String) {
            val deduplicationId = DeduplicationId(UUID.randomUUID())
            val account = Account(
                id = Account.Id(UUID.fromString("806daf1a-d72b-4edb-845e-7c87497ecffb")),
                balance = Account.Balance("0.00".toBigDecimal())
            )
            insertAccountData(account)

            shouldThrow<IllegalArgumentException> {
                accountService.deposit(account.id, Amount.from(amount.toBigDecimal()), deduplicationId)
            }

            val actualAccountData = getAccountData(account.id)
            actualAccountData shouldBe AccountData(
                account = Account(
                    id = Account.Id(UUID.fromString("806daf1a-d72b-4edb-845e-7c87497ecffb")),
                    balance = Account.Balance("0.00".toBigDecimal())
                ),
                deduplicationIds = emptySet()
            )
        }
    }

    @Nested
    inner class Withdraw {

        @ParameterizedTest
        @ValueSource(strings = ["0.1", "100.0", "1000.00"])
        fun `should withdraw some amount`(amount: String) {
            val deduplicationId = DeduplicationId(UUID.randomUUID())
            val account = Account(
                id = Account.Id(UUID.fromString("806daf1a-d72b-4edb-845e-7c87497ecffb")),
                balance = Account.Balance("1000.00".toBigDecimal())
            )
            insertAccountData(account)

            val actualAccountData = accountService.withdraw(
                accountId = account.id,
                amount = Amount.from(amount.toBigDecimal()),
                deduplicationId = deduplicationId
            )

            actualAccountData shouldBe AccountData(
                account = Account(
                    id = Account.Id(UUID.fromString("806daf1a-d72b-4edb-845e-7c87497ecffb")),
                    balance = Account.Balance("1000.00".toBigDecimal() - amount.toBigDecimal())
                ),
                deduplicationIds = setOf(deduplicationId)
            )
        }

        @Test
        fun `should be idempotent and not process duplicate withdraw operation`() {
            val deduplicationId = DeduplicationId(UUID.randomUUID())
            val account = Account(
                id = Account.Id(UUID.fromString("806daf1a-d72b-4edb-845e-7c87497ecffb")),
                balance = Account.Balance("1000.00".toBigDecimal())
            )
            insertAccountData(account, setOf(deduplicationId))

            val actualAccountData = accountService.withdraw(
                accountId = account.id,
                amount = Amount.from("250.00".toBigDecimal()),
                deduplicationId = deduplicationId
            )

            actualAccountData shouldBe AccountData(
                account = Account(
                    id = Account.Id(UUID.fromString("806daf1a-d72b-4edb-845e-7c87497ecffb")),
                    balance = Account.Balance("1000.00".toBigDecimal())
                ),
                deduplicationIds = setOf(deduplicationId)
            )
        }

        @Test
        fun `should not withdraw when account is not found`() {
            val deduplicationId = DeduplicationId(UUID.randomUUID())
            val invalidAccountId = Account.Id(UUID.randomUUID())

            shouldThrow<AccountNotFoundException> {
                accountService.withdraw(invalidAccountId, Amount.from("50.00".toBigDecimal()), deduplicationId)
            }
        }

        @Test
        fun `should not withdraw if account doesn't have sufficient funds`() {
            val deduplicationId = DeduplicationId(UUID.randomUUID())
            val account = Account(
                id = Account.Id(UUID.fromString("806daf1a-d72b-4edb-845e-7c87497ecffb")),
                balance = Account.Balance("1000.00".toBigDecimal())
            )
            insertAccountData(account)

            shouldThrow<InsufficientFundsException> {
                accountService.withdraw(account.id, Amount.from("1000.10".toBigDecimal()), deduplicationId)
            }

            val actualAccountData = getAccountData(account.id)
            actualAccountData shouldBe AccountData(
                account = Account(
                    id = Account.Id(UUID.fromString("806daf1a-d72b-4edb-845e-7c87497ecffb")),
                    balance = Account.Balance("1000.00".toBigDecimal())
                ),
                deduplicationIds = emptySet()
            )
        }
    }

    @Nested
    inner class Transfer {

        @ParameterizedTest
        @ValueSource(strings = ["0.1", "50.0", "1000.00"])
        fun `should transfer some amount between two valid accounts`(amount: String) {
            val deduplicationId = DeduplicationId(UUID.randomUUID())
            val sourceAccount = Account(
                id = Account.Id(UUID.fromString("806daf1a-d72b-4edb-845e-7c87497ecffb")),
                balance = Account.Balance("1000.00".toBigDecimal())
            )
            val destinationAccount = Account(
                id = Account.Id(UUID.fromString("8e65b612-959b-45db-99d4-a7b6acf39435")),
                balance = Account.Balance("0.00".toBigDecimal())
            )
            insertAccountData(sourceAccount)
            insertAccountData(destinationAccount)

            val actualSourceAccountData = accountService.transfer(
                sourceAccountId = sourceAccount.id,
                destinationAccountId = destinationAccount.id,
                amount = Amount.from(amount.toBigDecimal()),
                deduplicationId = deduplicationId
            )

            val actualDestinationAccountData = getAccountData(destinationAccount.id)
            actualSourceAccountData shouldBe AccountData(
                account = Account(
                    id = Account.Id(UUID.fromString("806daf1a-d72b-4edb-845e-7c87497ecffb")),
                    balance = Account.Balance("1000.00".toBigDecimal() - amount.toBigDecimal())
                ),
                deduplicationIds = setOf(deduplicationId)
            )
            actualDestinationAccountData shouldBe AccountData(
                account = Account(
                    id = Account.Id(UUID.fromString("8e65b612-959b-45db-99d4-a7b6acf39435")),
                    balance = Account.Balance("0.00".toBigDecimal() + amount.toBigDecimal())
                ),
                deduplicationIds = setOf(deduplicationId)
            )
        }

        @Test
        fun `should be idempotent and not process duplicate transfer operation`() {
            val deduplicationId = DeduplicationId(UUID.randomUUID())
            val sourceAccount = Account(
                id = Account.Id(UUID.fromString("806daf1a-d72b-4edb-845e-7c87497ecffb")),
                balance = Account.Balance("750.00".toBigDecimal())
            )
            val destinationAccount = Account(
                id = Account.Id(UUID.fromString("8e65b612-959b-45db-99d4-a7b6acf39435")),
                balance = Account.Balance("250.00".toBigDecimal())
            )
            insertAccountData(sourceAccount, setOf(deduplicationId))
            insertAccountData(destinationAccount, setOf(deduplicationId))

            val actualSourceAccountData = accountService.transfer(
                sourceAccountId = sourceAccount.id,
                destinationAccountId = destinationAccount.id,
                amount = Amount.from("250.00".toBigDecimal()),
                deduplicationId = deduplicationId
            )

            val actualDestinationAccountData = getAccountData(destinationAccount.id)
            actualSourceAccountData shouldBe AccountData(
                account = Account(
                    id = Account.Id(UUID.fromString("806daf1a-d72b-4edb-845e-7c87497ecffb")),
                    balance = Account.Balance("750.00".toBigDecimal())
                ),
                deduplicationIds = setOf(deduplicationId)
            )
            actualDestinationAccountData shouldBe AccountData(
                account = Account(
                    id = Account.Id(UUID.fromString("8e65b612-959b-45db-99d4-a7b6acf39435")),
                    balance = Account.Balance("250.00".toBigDecimal())
                ),
                deduplicationIds = setOf(deduplicationId)
            )
        }

        @Test
        fun `should not transfer money for the same account`() {
            val sourceAccount = Account(
                id = Account.Id(UUID.fromString("806daf1a-d72b-4edb-845e-7c87497ecffb")),
                balance = Account.Balance("1000.00".toBigDecimal())
            )
            insertAccountData(sourceAccount)
            val destinationAccountId = sourceAccount.id

            shouldThrow<TransferNotAllowedException> {
                accountService.transfer(
                    sourceAccount.id,
                    destinationAccountId,
                    Amount.from("100.00".toBigDecimal()),
                    DeduplicationId(UUID.randomUUID())
                )
            }

            val actualSourceAccountData = getAccountData(sourceAccount.id)
            actualSourceAccountData shouldBe AccountData(
                account = Account(
                    id = Account.Id(UUID.fromString("806daf1a-d72b-4edb-845e-7c87497ecffb")),
                    balance = Account.Balance("1000.00".toBigDecimal())
                ),
                deduplicationIds = emptySet()
            )
        }

        @Test
        fun `should not transfer money if source account is not found`() {
            val sourceAccountId = Account.Id(UUID.randomUUID())
            val destinationAccount = Account(
                id = Account.Id(UUID.fromString("8e65b612-959b-45db-99d4-a7b6acf39435")),
                balance = Account.Balance("10000.00".toBigDecimal())
            )
            insertAccountData(destinationAccount)

            shouldThrow<AccountNotFoundException> {
                accountService.transfer(
                    sourceAccountId,
                    destinationAccount.id,
                    Amount.from("50.00".toBigDecimal()),
                    DeduplicationId(UUID.randomUUID())
                )
            }

            val actualDestinationAccountData = getAccountData(destinationAccount.id)
            actualDestinationAccountData shouldBe AccountData(
                account = Account(
                    id = Account.Id(UUID.fromString("8e65b612-959b-45db-99d4-a7b6acf39435")),
                    balance = Account.Balance("10000.00".toBigDecimal())
                ),
                deduplicationIds = emptySet()
            )
        }

        @Test
        fun `should not transfer money if destination account is not found`() {
            val sourceAccount = Account(
                id = Account.Id(UUID.fromString("8e65b612-959b-45db-99d4-a7b6acf39435")),
                balance = Account.Balance("1000.00".toBigDecimal())
            )
            insertAccountData(sourceAccount)
            val destinationAccountId = Account.Id(UUID.randomUUID())

            shouldThrow<AccountNotFoundException> {
                accountService.transfer(
                    sourceAccount.id,
                    destinationAccountId,
                    Amount.from("50.00".toBigDecimal()),
                    DeduplicationId(UUID.randomUUID())
                )
            }

            val actualSourceAccountData = getAccountData(sourceAccount.id)
            actualSourceAccountData shouldBe AccountData(
                account = Account(
                    id = Account.Id(UUID.fromString("8e65b612-959b-45db-99d4-a7b6acf39435")),
                    balance = Account.Balance("1000.00".toBigDecimal())
                ),
                deduplicationIds = emptySet()
            )
        }

        @Test
        fun `should not transfer money if source account doesn't have sufficient funds`() {
            val sourceAccount = Account(
                id = Account.Id(UUID.fromString("806daf1a-d72b-4edb-845e-7c87497ecffb")),
                balance = Account.Balance("1000.00".toBigDecimal())
            )
            val destinationAccount = Account(
                id = Account.Id(UUID.fromString("8e65b612-959b-45db-99d4-a7b6acf39435")),
                balance = Account.Balance("0.00".toBigDecimal())
            )
            insertAccountData(sourceAccount)
            insertAccountData(destinationAccount)

            shouldThrow<InsufficientFundsException> {
                accountService.transfer(
                    sourceAccount.id,
                    destinationAccount.id,
                    Amount.from("1000.10".toBigDecimal()),
                    DeduplicationId(UUID.randomUUID())
                )
            }

            val actualSourceAccountData = getAccountData(sourceAccount.id)
            val actualDestinationAccountData = getAccountData(destinationAccount.id)

            actualSourceAccountData shouldBe AccountData(
                account = Account(
                    id = Account.Id(UUID.fromString("806daf1a-d72b-4edb-845e-7c87497ecffb")),
                    balance = Account.Balance("1000.00".toBigDecimal())
                ),
                deduplicationIds = emptySet()
            )
            actualDestinationAccountData shouldBe AccountData(
                account = Account(
                    id = Account.Id(UUID.fromString("8e65b612-959b-45db-99d4-a7b6acf39435")),
                    balance = Account.Balance("0.00".toBigDecimal())
                ),
                deduplicationIds = emptySet()
            )
        }
    }

    private fun insertAccountData(account: Account, deduplicationIds: Set<DeduplicationId> = emptySet()) {
        accountDatabase[account.id] = AccountData(account, deduplicationIds)
    }

    private fun getAccountData(accountId: Account.Id): AccountData = accountDatabase[accountId]!!

}
