package com.nielsen.application

import com.nielsen.model.Account

class AccountNotFoundException(accountId: Account.Id) :
    RuntimeException("Account ${accountId.value} not found.")

class DuplicateAccountException(accountId: Account.Id) :
    RuntimeException("Account ${accountId.value} already exists.")

class TransferNotAllowedException(accountId: Account.Id) :
    BadRequestException("Cannot transfer money within same account ${accountId.value}.")

class InsufficientFundsException(accountId: Account.Id) :
    BadRequestException("Account ${accountId.value} doesn't have sufficient funds for this operation.")

open class BadRequestException(message: String) : RuntimeException(message)
