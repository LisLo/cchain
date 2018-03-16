package com.ismair.cchain.contracts.trade.model

import com.ismair.cchain.contracts.ContractRequest
import com.ismair.cchain.contracts.cash.model.DepotRequest
import kotlinx.serialization.Serializable

@Serializable
data class TradeRequest(
        val mode: TradeMode,
        val user: String,
        val isin: String,
        val shareCount: Int,
        val priceLimit: Int,
        val dateLimit: String
) : ContractRequest {
    constructor(user: String, request: DepotRequest) : this(
            if (request.mode == DepotRequest.Mode.BUY) TradeMode.BUY else TradeMode.SELL,
            user,
            request.isin,
            request.shareCount,
            request.priceLimit,
            request.dateLimit
    )
}