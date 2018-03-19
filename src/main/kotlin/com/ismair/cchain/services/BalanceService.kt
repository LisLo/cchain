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
        val (requestId, payer, payee, amount, _) = confirmation
        if (!requestIds.contains(requestId)) {
            add(payer.shrink(), -amount)
            add(payee.shrink(), +amount)
            requestIds.add(requestId)
        }
    }

    fun hasEnoughMoney(user: String, amount: Int): Boolean {
        val balance = balances[user.shrink()]
        return balance != null && balance >= amount
    }
}