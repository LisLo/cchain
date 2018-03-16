package com.ismair.cchain.contracts.trade.model

import com.ismair.cchain.contracts.ContractResponse
import kotlinx.serialization.Serializable

@Serializable
data class TradeRejection(
        override val requestId: Int,
        val request: TradeRequest,
        val message: String
) : ContractResponse