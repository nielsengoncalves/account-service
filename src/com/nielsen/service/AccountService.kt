package com.nielsen.service

import com.nielsen.application.AccountDatabase
import com.nielsen.application.AccountNotFoundException
import com.nielsen.application.DuplicateAccountException
import com.nielsen.application.InsufficientFundsException
import com.nielsen.application.TransferNotAllowedException
import com.nielsen.application.compute
import com.nielsen.model.Account
import com.nielsen.model.Amount
import java.math.BigDecimal

class AccountService(private val accountDatabase: AccountDatabase) {

    fun insert(account: Account): Account =
        accountDatabase.compute(account.id,
            ifAbsent = { account },
            ifPresent = { existingAccountId, _ -> throw DuplicateAccountException(existingAccountId) }
        )

    fun getAccountById(accountId: Account.Id): Account =
        accountDatabase[accountId] ?: throw AccountNotFoundException(accountId)

    fun deposit(accountId: Account.Id, amount: Amount): Account =
        accountDatabase.compute(accountId,
            ifAbsent = { throw AccountNotFoundException(it) },
            ifPresent = { _, existingAccount -> existingAccount.deposit(amount) }
        )

    fun withdraw(accountId: Account.Id, amount: Amount): Account =
        accountDatabase.compute(accountId,
            ifAbsent = { throw AccountNotFoundException(it) },
            ifPresent = { _, existingAccount ->
                checkFundsForWithdraw(existingAccount, amount)
                existingAccount.withdraw(amount)
            })


    fun transfer(sourceAccountId: Account.Id, destinationAccountId: Account.Id, amount: Amount): Account {
        if (sourceAccountId == destinationAccountId) {
            throw TransferNotAllowedException(sourceAccountId)
        }

        return accountDatabase.compute(
            sourceAccountId,
            ifAbsent = { throw AccountNotFoundException(it) },
            ifPresent = { _, sourceAccount ->
                checkFundsForWithdraw(sourceAccount, amount)
                accountDatabase.compute(destinationAccountId,
                    ifAbsent = { throw AccountNotFoundException(it) },
                    ifPresent = { _, destinyAccount -> destinyAccount.deposit(amount) }
                )
                sourceAccount.withdraw(amount)
            }
        )
    }

    private fun checkFundsForWithdraw(account: Account, amountToWithdraw: Amount) {
        val sourceAccountNewBalance = account.balance.value - amountToWithdraw.value
        if (sourceAccountNewBalance.compareTo(BigDecimal.ZERO) == -1) {
            throw InsufficientFundsException(account.id)
        }
    }

    private fun Account.deposit(amount: Amount): Account =
        this.copy(balance = Account.Balance(this.balance.value + amount.value))

    private fun Account.withdraw(amount: Amount): Account =
        this.copy(balance = Account.Balance(this.balance.value - amount.value))
}