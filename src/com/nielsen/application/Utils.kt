package com.nielsen

import com.nielsen.model.Account
import java.util.concurrent.ConcurrentHashMap

typealias AccountDatabase = ConcurrentHashMap<Account.Id, Account>

fun <K, V: Any> ConcurrentHashMap<K, V>.compute(key: K, ifAbsent: (K) -> V, ifPresent: (K, V) -> V): V {
    return this.compute(key) { _, value ->
        if (value == null) {
            ifAbsent(key)
        } else {
            ifPresent(key, value)
        }
    }!!
}