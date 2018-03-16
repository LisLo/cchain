package com.ismair.cchain.contracts.cash.model

import com.ismair.cchain.contracts.ContractRequest
import kotlinx.serialization.Serializable

@Serializable
data class DepotRequest(
        val mode: Mode,
        val user: String,
        val isin: String,
        val shareCount: Int,
        val priceLimit: Int,
        val dateLimit: String
) : ContractRequest {
    enum class Mode { BUY, SELL }
}