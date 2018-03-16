package com.ismair.cchain.model.trade

import com.ismair.cchain.ContractResponse
import kotlinx.serialization.Serializable

@Serializable
data class TradeRejection(
        override val requestId: Int,
        val request: TradeRequest,
        val message: String
) : ContractResponse