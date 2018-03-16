package com.ismair.cchain.services

import com.ismair.cchain.model.trade.TradeConfirmation
import com.ismair.cchain.model.trade.TradeMode

class DepotService(list: List<TradeConfirmation>) {
    private val requestIds = mutableSetOf<Int>()
    private val shares = mutableMapOf<Pair<String, String>, Int>()

    init {
        list.forEach { add(it) }
    }

    fun add(confirmation: TradeConfirmation) {
        if (requestIds.contains(confirmation.requestId)) {
            val key = Pair(confirmation.user, confirmation.isin)
            val shareCount = confirmation.shareCount
            val signedShareCount = if (confirmation.mode == TradeMode.BUY) shareCount else -shareCount
            shares[key] = shares.getOrDefault(key, 0) + signedShareCount
        }
    }

    fun hasEnoughShares(user: String, isin: String, shareCount: Int): Boolean {
        val share = shares[Pair(user, isin)]
        return share != null && share >= shareCount
    }
}