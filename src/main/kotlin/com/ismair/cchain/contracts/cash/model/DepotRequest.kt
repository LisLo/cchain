package com.ismair.cchain.contracts.cash.model

import com.ismair.cchain.contracts.ContractRequest
import kotlinx.serialization.Serializable

@Serializable
data class DepotRequest(
        val mode: DepotMode,
        val isin: String,
        val shareCount: Int,
        val priceLimit: Int,
        val dateLimit: String
) : ContractRequest