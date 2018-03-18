package com.ismair.cchain.model.trade

import com.ismair.cchain.model.ContractRequest
import kotlinx.serialization.Serializable

@Serializable
data class TradeRequest(
        val mode: TradeMode,
        val user: String,
        val isin: String,
        val shareCount: Int,
        val priceLimit: Int,
        val dateLimit: String
) : ContractRequest()