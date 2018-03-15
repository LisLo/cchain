package com.ismair.cchain.trade.model

import com.ismair.cchain.contract.ContractResponse
import kotlinx.serialization.Serializable

@Serializable
data class TradeConfirmation(
        override val requestId: Int,
        val request: TradeRequest,
        val price: Int
) : ContractResponse