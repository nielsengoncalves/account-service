package com.nielsen.service

import com.nielsen.model.Account
import com.nielsen.model.Amount

interface AccountService {
    fun insert(account: Account)
    fun findById(accountId: Account.Id): Account?
    fun getAccountById(accountId: Account.Id): Account
    fun deposit(accountId: Account.Id, amount: Amount)
    fun withdraw(accountId: Account.Id, amount: Amount)
    fun transfer(sourceAccountId: Account.Id, destinationAccountId: Account.Id, amount: Amount)
}

class TransferNotAllowedException(accountId: Account.Id):
    RuntimeException("Cannot transfer money within same account ${accountId.value}.")

class DuplicateAccountException(accountId: Account.Id):
    RuntimeException("Account ${accountId.value} already exists.")

class InsufficientFundsException(accountId: Account.Id):
    RuntimeException("Account ${accountId.value} doesn't have sufficient funds for this operation.")

class AccountNotFoundException(accountId: Account.Id):
    RuntimeException("Account ${accountId.value} not found.")