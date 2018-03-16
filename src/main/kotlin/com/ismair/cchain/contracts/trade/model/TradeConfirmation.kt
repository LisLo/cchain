package com.ismair.cchain.contracts.trade.model

import com.ismair.cchain.contracts.ContractResponse
import kotlinx.serialization.Serializable

@Serializable
data class TradeConfirmation(
        override val requestId: Int,
        val mode: TradeMode,
        val user: String,
        val isin: String,
        val shareCount: Int,
        val price: Int
) : ContractResponse