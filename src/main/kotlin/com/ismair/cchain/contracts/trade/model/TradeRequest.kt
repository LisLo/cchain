package com.ismair.cchain.contracts.trade.model

import com.ismair.cchain.contracts.cash.model.DepotRequest
import com.ismair.cchain.contracts.ContractRequest
import kotlinx.serialization.Serializable

@Serializable
data class TradeRequest(
        val mode: Mode,
        val user: String,
        val isin: String,
        val shareCount: Int,
        val priceLimit: Int,
        val dateLimit: String
) : ContractRequest {
    enum class Mode { BUY, SELL }

    constructor(request: DepotRequest) : this(
            if (request.mode == DepotRequest.Mode.BUY) Mode.BUY else Mode.SELL,
            request.user,
            request.isin,
            request.shareCount,
            request.priceLimit,
            request.dateLimit
    )
}