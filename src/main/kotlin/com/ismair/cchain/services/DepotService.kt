package com.ismair.cchain.services

import com.ismair.cchain.extensions.shrink
import com.ismair.cchain.model.trade.TradeConfirmation
import com.ismair.cchain.model.trade.TradeMode

class DepotService(list: List<TradeConfirmation>) {
    private val requestIds = mutableSetOf<Int>()
    private val shares = mutableMapOf<Pair<String, String>, Int>()

    init {
        list.forEach { add(it) }
    }

    fun add(confirmation: TradeConfirmation) {
        val (requestId, mode, user, isin, shareCount, _, _) = confirmation
        if (!requestIds.contains(requestId)) {
            val key = Pair(user.shrink(), isin)
            val signedShareCount = if (mode == TradeMode.BUY) shareCount else -shareCount
            shares[key] = shares.getOrDefault(key, 0) + signedShareCount
            requestIds.add(requestId)
        }
    }

    fun hasEnoughShares(user: String, isin: String, shareCount: Int): Boolean {
        val share = shares[Pair(user.shrink(), isin)]
        return share != null && share >= shareCount
    }
}