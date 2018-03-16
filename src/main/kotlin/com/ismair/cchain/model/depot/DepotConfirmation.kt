package com.ismair.cchain.model.depot

import com.ismair.cchain.abstracts.ContractResponse
import kotlinx.serialization.Serializable

@Serializable
data class DepotConfirmation(
        override val requestId: Int,
        val mode: DepotMode,
        val isin: String,
        val shareCount: Int,
        val priceLimit: Int,
        val dateLimit: String
) : ContractResponse()