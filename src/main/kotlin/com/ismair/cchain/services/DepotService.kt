package com.ismair.cchain.services

import com.ismair.cchain.contracts.trade.model.TradeConfirmation
import com.ismair.cchain.contracts.trade.model.TradeRequest

class DepotService(list: List<TradeConfirmation>) {
    private val shares = list
            .groupBy { Pair(it.request.user, it.request.isin) }
            .toList()
            .associate { it.first to it.second.sumBy { getSignedShareCount(it) } }
            .toMutableMap()

    private fun getSignedShareCount(confirmation: TradeConfirmation): Int {
        val shareCount = confirmation.request.shareCount
        return if (confirmation.request.mode == TradeRequest.Mode.BUY) shareCount else -shareCount
    }

    fun add(user: String, confirmation: TradeConfirmation) {
        val isin = confirmation.request.isin
        val key = Pair(user, isin)
        shares[key] = shares.getOrDefault(key, 0) + getSignedShareCount(confirmation)
    }

    fun hasEnoughShares(user: String, isin: String, shareCount: Int): Boolean {
        val share = shares[Pair(user, isin)]
        return share != null && share >= shareCount
    }
}