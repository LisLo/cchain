package com.ismair.cchain.trade.model

import com.ismair.cchain.cash.model.DepotRequest
import com.ismair.cchain.contract.ContractRequest
import kotlinx.serialization.Serializable

@Serializable
data class TradeRequest(
        val mode: Mode,
        val name: String,
        val isin: String,
        val shareCount: Int,
        val priceLimit: Int,
        val dateLimit: String
) : ContractRequest {
    enum class Mode { BUY, SELL }

    constructor(request: DepotRequest) : this(
            if (request.mode == DepotRequest.Mode.BUY) Mode.BUY else Mode.SELL,
            request.name,
            request.isin,
            request.shareCount,
            request.priceLimit,
            request.dateLimit
    )
}