package com.nielsen.service

import com.nielsen.application.AccountDatabase
import com.nielsen.application.AccountNotFoundException
import com.nielsen.application.DuplicateAccountException
import com.nielsen.application.InsufficientFundsException
import com.nielsen.application.TransferNotAllowedException
import com.nielsen.model.Account
import com.nielsen.model.Amount
import io.kotlintest.shouldBe
import io.kotlintest.shouldThrow
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.lang.IllegalArgumentException
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
            insertAccount(account)

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
            insertAccount(account)

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
            val account = Account(
                id = Account.Id(UUID.fromString("806daf1a-d72b-4edb-845e-7c87497ecffb")),
                balance = Account.Balance("0.00".toBigDecimal())
            )
            insertAccount(account)

            val actualAccount = accountService.deposit(account.id, Amount.from("50".toBigDecimal()))
            actualAccount shouldBe Account(
                id = Account.Id(UUID.fromString("806daf1a-d72b-4edb-845e-7c87497ecffb")),
                balance = Account.Balance("50.00".toBigDecimal())
            )
        }

        @Test
        fun `should not deposit when account is not found`() {
            val invalidAccountId = Account.Id(UUID.randomUUID())

            shouldThrow<AccountNotFoundException> {
                accountService.deposit(invalidAccountId, Amount.from("50.00".toBigDecimal()))
            }
        }

        @ParameterizedTest
        @ValueSource(strings = ["0.0", "-50.5"])
        fun `should not deposit zero and negative amounts`(amount: String) {
            val account = Account(
                id = Account.Id(UUID.fromString("806daf1a-d72b-4edb-845e-7c87497ecffb")),
                balance = Account.Balance("0.00".toBigDecimal())
            )
            insertAccount(account)

            shouldThrow<IllegalArgumentException> {
                accountService.deposit(account.id, Amount.from(amount.toBigDecimal()))
            }

            val actualBalance = getAccountBalance(account.id)
            actualBalance shouldBe Account.Balance("0.00".toBigDecimal())
        }
    }

    @Nested
    inner class Withdraw {

        @ParameterizedTest
        @ValueSource(strings = ["0.1", "100.0", "1000.00"])
        fun `should withdraw some amount`(amount: String) {
            val account = Account(
                id = Account.Id(UUID.fromString("806daf1a-d72b-4edb-845e-7c87497ecffb")),
                balance = Account.Balance("1000.00".toBigDecimal())
            )
            insertAccount(account)

            val actualAccount = accountService.withdraw(account.id, Amount.from(amount.toBigDecimal()))

            actualAccount shouldBe Account(
                id = Account.Id(UUID.fromString("806daf1a-d72b-4edb-845e-7c87497ecffb")),
                balance = Account.Balance("1000.00".toBigDecimal() - amount.toBigDecimal())
            )
        }

        @Test
        fun `should not withdraw when account is not found`() {
            val invalidAccountId = Account.Id(UUID.randomUUID())

            shouldThrow<AccountNotFoundException> {
                accountService.withdraw(invalidAccountId, Amount.from("50.00".toBigDecimal()))
            }
        }

        @Test
        fun `should not withdraw if account doesn't have sufficient funds`() {
            val account = Account(
                id = Account.Id(UUID.fromString("806daf1a-d72b-4edb-845e-7c87497ecffb")),
                balance = Account.Balance("1000.00".toBigDecimal())
            )
            insertAccount(account)

            shouldThrow<InsufficientFundsException> {
                accountService.withdraw(account.id, Amount.from("1000.10".toBigDecimal()))
            }

            val actualBalance = getAccountBalance(account.id)
            actualBalance shouldBe Account.Balance("1000.00".toBigDecimal())
        }
    }

    @Nested
    inner class Transfer {

        @ParameterizedTest
        @ValueSource(strings = ["0.1", "50.0", "1000.00"])
        fun `should transfer some amount between two valid accounts`(amount: String) {
            val sourceAccount = Account(
                id = Account.Id(UUID.fromString("806daf1a-d72b-4edb-845e-7c87497ecffb")),
                balance = Account.Balance("1000.00".toBigDecimal())
            )
            val destinationAccount = Account(
                id = Account.Id(UUID.fromString("8e65b612-959b-45db-99d4-a7b6acf39435")),
                balance = Account.Balance("0.00".toBigDecimal())
            )
            insertAccount(sourceAccount)
            insertAccount(destinationAccount)

            val actualSourceAccount = accountService.transfer(
                sourceAccountId = sourceAccount.id,
                destinationAccountId = destinationAccount.id,
                amount = Amount.from(amount.toBigDecimal())
            )
            val actualDestinationAccountBalance = getAccountBalance(destinationAccount.id)

            actualSourceAccount shouldBe Account(
                id = Account.Id(UUID.fromString("806daf1a-d72b-4edb-845e-7c87497ecffb")),
                balance = Account.Balance("1000.00".toBigDecimal() - amount.toBigDecimal())
            )
            actualDestinationAccountBalance shouldBe Account.Balance("0.00".toBigDecimal() + amount.toBigDecimal())
        }

        @Test
        fun `should not transfer money for the same account`() {
            val sourceAccount = Account(
                id = Account.Id(UUID.fromString("806daf1a-d72b-4edb-845e-7c87497ecffb")),
                balance = Account.Balance("1000.00".toBigDecimal())
            )
            insertAccount(sourceAccount)
            val destinationAccountId = sourceAccount.id

            shouldThrow<TransferNotAllowedException> {
                accountService.transfer(sourceAccount.id, destinationAccountId, Amount.from("100.00".toBigDecimal()))
            }

            val actualSourceAccountBalance = getAccountBalance(sourceAccount.id)
            actualSourceAccountBalance shouldBe Account.Balance("1000.00".toBigDecimal())
        }

        @Test
        fun `should not transfer money if source account is not found`() {
            val sourceAccountId = Account.Id(UUID.randomUUID())
            val destinationAccount = Account(
                id = Account.Id(UUID.fromString("8e65b612-959b-45db-99d4-a7b6acf39435")),
                balance = Account.Balance("10000.00".toBigDecimal())
            )
            insertAccount(destinationAccount)

            shouldThrow<AccountNotFoundException> {
                accountService.transfer(sourceAccountId, destinationAccount.id, Amount.from("50.00".toBigDecimal()))
            }

            val actualDestinationAccountBalance = getAccountBalance(destinationAccount.id)
            actualDestinationAccountBalance shouldBe Account.Balance("10000.00".toBigDecimal())
        }

        @Test
        fun `should not transfer money if destination account is not found`() {
            val sourceAccount = Account(
                id = Account.Id(UUID.fromString("806daf1a-d72b-4edb-845e-7c87497ecffb")),
                balance = Account.Balance("1000.00".toBigDecimal())
            )
            insertAccount(sourceAccount)
            val destinationAccountId = Account.Id(UUID.randomUUID())

            shouldThrow<AccountNotFoundException> {
                accountService.transfer(sourceAccount.id, destinationAccountId, Amount.from("50.00".toBigDecimal()))
            }

            val actualSourceAccountBalance = getAccountBalance(sourceAccount.id)
            actualSourceAccountBalance shouldBe Account.Balance("1000.00".toBigDecimal())
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
            insertAccount(sourceAccount)
            insertAccount(destinationAccount)

            shouldThrow<InsufficientFundsException> {
                accountService.transfer(sourceAccount.id, destinationAccount.id, Amount.from(1000.10.toBigDecimal()))
            }

            val actualSourceAccountBalance = getAccountBalance(sourceAccount.id)
            val actualDestinationAccountBalance = getAccountBalance(destinationAccount.id)

            actualSourceAccountBalance shouldBe Account.Balance("1000.00".toBigDecimal())
            actualDestinationAccountBalance shouldBe Account.Balance("0.00".toBigDecimal())
        }
    }

    private fun insertAccount(account: Account) {
        accountDatabase[account.id] = account
    }

    private fun getAccount(accountId: Account.Id): Account = accountDatabase[accountId]!!

    private fun getAccountBalance(accountId: Account.Id): Account.Balance = getAccount(accountId).balance
}