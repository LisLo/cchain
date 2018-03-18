package com.ismair.cchain.model.depot

import com.ismair.cchain.model.ContractRequest
import kotlinx.serialization.Serializable

@Serializable
data class DepotRequest(
        val mode: DepotMode,
        val isin: String,
        val shareCount: Int,
        val priceLimit: Int,
        val dateLimit: String
) : ContractRequest()