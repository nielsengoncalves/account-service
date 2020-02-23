package com.nielsen.service

import com.nielsen.AccountsDatabase
import com.nielsen.compute
import com.nielsen.model.Account
import com.nielsen.model.Amount
import java.math.BigDecimal

class AccountServiceImpl(private val accountsDatabase: AccountsDatabase): AccountService {

    override fun insert(account: Account) {
        accountsDatabase.compute(account.id,
            ifAbsent = { account },
            ifPresent = { existingAccountId, _ -> throw DuplicateAccountException(existingAccountId) }
        )
    }

    override fun findById(accountId: Account.Id): Account? = accountsDatabase[accountId]

    override fun getAccountById(accountId: Account.Id): Account =
        findById(accountId) ?: throw AccountNotFoundException(accountId)

    override fun deposit(accountId: Account.Id, amount: Amount) {
        accountsDatabase.compute(accountId,
            ifAbsent = { throw AccountNotFoundException(it) },
            ifPresent = { _, existingAccount -> existingAccount.deposit(amount) }
        )
    }

    override fun withdraw(accountId: Account.Id, amount: Amount) {
        accountsDatabase.compute(accountId,
            ifAbsent = { throw AccountNotFoundException(it) },
            ifPresent = { _, existingAccount ->
                checkFundsForWithdraw(existingAccount, amount)
                existingAccount.withdraw(amount)
            })
    }

    override fun transfer(sourceAccountId: Account.Id, destinationAccountId: Account.Id, amount: Amount) {
        if (sourceAccountId == destinationAccountId) {
            throw TransferNotAllowedException(sourceAccountId)
        }

        accountsDatabase.compute(
            sourceAccountId,
            ifAbsent = { throw AccountNotFoundException(it) },
            ifPresent = { _, sourceAccount ->
                checkFundsForWithdraw(sourceAccount, amount)
                accountsDatabase.compute(destinationAccountId,
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