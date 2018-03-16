package com.ismair.cchain.model.trade

import com.ismair.cchain.abstracts.ContractRequest
import com.ismair.cchain.model.depot.DepotMode
import com.ismair.cchain.model.depot.DepotRequest
import kotlinx.serialization.Serializable

@Serializable
data class TradeRequest(
        val mode: TradeMode,
        val user: String,
        val isin: String,
        val shareCount: Int,
        val priceLimit: Int,
        val dateLimit: String
) : ContractRequest() {
    constructor(user: String, request: DepotRequest) : this(
            if (request.mode == DepotMode.BUY) TradeMode.BUY else TradeMode.SELL,
            user,
            request.isin,
            request.shareCount,
            request.priceLimit,
            request.dateLimit
    )
}