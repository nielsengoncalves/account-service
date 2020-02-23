package com.nielsen

import com.nielsen.model.Account
import java.util.concurrent.ConcurrentHashMap

typealias AccountsDatabase = ConcurrentHashMap<Account.Id, Account>

fun <K, V> ConcurrentHashMap<K, V>.compute(key: K, ifAbsent: (K) -> V, ifPresent: (K, V) -> V) {
    this.compute(key) { _, value ->
        if (value == null) {
            ifAbsent(key)
        } else {
            ifPresent(key, value)
        }
    }
}