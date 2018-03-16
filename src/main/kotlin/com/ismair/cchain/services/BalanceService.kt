package com.ismair.cchain.services

import com.ismair.cchain.contracts.cash.model.TransferConfirmation

class BalanceService(responses: List<TransferConfirmation>) {
    private val balances = responses
            .groupBy { it.payee }
            .toList()
            .associate { it.first to it.second.sumBy { it.amount } }
            .toMutableMap()

    fun add(confirmation: TransferConfirmation) {
        balances[confirmation.payer] = balances.getOrDefault(confirmation.payer, 0) - confirmation.amount
        balances[confirmation.payee] = balances.getOrDefault(confirmation.payee, 0) + confirmation.amount
    }

    fun hasEnoughMoney(user: String, money: Int): Boolean {
        val balance = balances[user]
        return balance != null && balance >= money
    }
}