package com.ismair.cchain.trade.model

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
}