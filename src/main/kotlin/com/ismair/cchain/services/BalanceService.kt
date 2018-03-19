package com.ismair.cchain.services

import com.ismair.cchain.extensions.shrink
import com.ismair.cchain.model.transfer.TransferConfirmation

class BalanceService(list: List<TransferConfirmation>) {
    private val requestIds = mutableSetOf<Int>()
    private val balances = mutableMapOf<String, Int>()

    init {
        list.forEach { add(it) }
    }

    private fun add(user: String, amount: Int) {
        val userShrinked = user.shrink()
        balances[userShrinked] = balances.getOrDefault(userShrinked, 0) + amount
    }

    fun add(confirmation: TransferConfirmation) {
        if (requestIds.contains(confirmation.requestId)) {
            add(confirmation.payer.shrink(), -confirmation.amount)
            add(confirmation.payee.shrink(), +confirmation.amount)
        }
    }

    fun hasEnoughMoney(user: String, amount: Int): Boolean {
        val balance = balances[user.shrink()]
        return balance != null && balance >= amount
    }
}