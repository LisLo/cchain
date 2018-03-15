package com.ismair.cchain.trade.model

import com.ismair.cchain.contract.ContractResponse
import kotlinx.serialization.Serializable

@Serializable
data class TradeRejection(
        override val requestId: Int,
        val request: TradeRequest,
        val message: String
) : ContractResponse