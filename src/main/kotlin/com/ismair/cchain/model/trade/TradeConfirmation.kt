package com.ismair.cchain.model.trade

import com.ismair.cchain.abstracts.ContractResponse
import kotlinx.serialization.Serializable

@Serializable
data class TradeConfirmation(
        override val requestId: Int,
        val mode: TradeMode,
        val user: String,
        val isin: String,
        val shareCount: Int,
        val priceLimit: Int,
        val price: Int
) : ContractResponse()