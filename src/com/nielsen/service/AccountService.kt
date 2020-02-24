package com.nielsen.service

import com.nielsen.application.AccountDatabase
import com.nielsen.application.AccountNotFoundException
import com.nielsen.application.DuplicateAccountException
import com.nielsen.application.InsufficientFundsException
import com.nielsen.application.TransferNotAllowedException
import com.nielsen.application.compute
import com.nielsen.model.Account
import com.nielsen.model.Account.Balance
import com.nielsen.model.Account.Id
import com.nielsen.model.AccountData
import com.nielsen.model.Amount
import com.nielsen.model.DeduplicationId
import java.math.BigDecimal

class AccountService(private val accountDatabase: AccountDatabase) {

    fun insert(account: Account): Account =
        accountDatabase.compute(account.id,
            ifAbsent = { AccountData(account, emptySet()) },
            ifPresent = { existingAccountId, _ -> throw DuplicateAccountException(existingAccountId) }
        ).account

    fun getAccountById(accountId: Id): Account =
        accountDatabase[accountId]?.account ?: throw AccountNotFoundException(accountId)

    fun deposit(accountId: Id, amount: Amount, deduplicationId: DeduplicationId): AccountData =
        accountDatabase.compute(accountId,
            ifAbsent = { throw AccountNotFoundException(it) },
            ifPresent = { _, accountData ->
                when {
                    operationIsAlreadyProcessed(accountData, deduplicationId) -> accountData
                    else -> accountData.deposit(amount, deduplicationId)
                }
            }
        )

    fun withdraw(accountId: Id, amount: Amount, deduplicationId: DeduplicationId): AccountData =
        accountDatabase.compute(accountId,
            ifAbsent = { throw AccountNotFoundException(it) },
            ifPresent = { _, accountData ->
                when {
                    operationIsAlreadyProcessed(accountData, deduplicationId) -> accountData
                    else -> {
                        checkFundsForWithdraw(accountData.account, amount)
                        accountData.withdraw(amount, deduplicationId)
                    }
                }
            }
        )

    fun transfer(
        sourceAccountId: Id,
        destinationAccountId: Id,
        amount: Amount,
        deduplicationId: DeduplicationId
    ): AccountData {
        if (sourceAccountId == destinationAccountId) {
            throw TransferNotAllowedException(sourceAccountId)
        }

        return accountDatabase.compute(
            sourceAccountId,
            ifAbsent = { throw AccountNotFoundException(it) },
            ifPresent = { _, sourceAccountData ->
                when {
                    operationIsAlreadyProcessed(sourceAccountData, deduplicationId) -> sourceAccountData
                    else -> {
                        checkFundsForWithdraw(sourceAccountData.account, amount)
                        deposit(destinationAccountId, amount, deduplicationId)
                        sourceAccountData.withdraw(amount, deduplicationId)
                    }
                }
            }
        )
    }

    private fun operationIsAlreadyProcessed(accountData: AccountData, deduplicationId: DeduplicationId) =
        accountData.deduplicationIds.contains(deduplicationId)

    private fun checkFundsForWithdraw(account: Account, amountToWithdraw: Amount) {
        val sourceAccountNewBalance = account.balance.value - amountToWithdraw.value
        if (sourceAccountNewBalance.compareTo(BigDecimal.ZERO) == -1) {
            throw InsufficientFundsException(account.id)
        }
    }

    private fun AccountData.deposit(amount: Amount, deduplicationId: DeduplicationId): AccountData =
        AccountData(
            account = this.account.deposit(amount),
            deduplicationIds = this.deduplicationIds.plus(deduplicationId)
        )

    private fun AccountData.withdraw(amount: Amount, deduplicationId: DeduplicationId): AccountData =
        AccountData(
            account = this.account.withdraw(amount),
            deduplicationIds = this.deduplicationIds.plus(deduplicationId)
        )

    private fun Account.deposit(amount: Amount): Account =
        this.copy(balance = Balance(this.balance.value + amount.value))

    private fun Account.withdraw(amount: Amount): Account =
        this.copy(balance = Balance(this.balance.value - amount.value))
}
