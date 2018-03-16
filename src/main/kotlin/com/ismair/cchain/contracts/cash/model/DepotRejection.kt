package com.ismair.cchain.contracts.cash.model

import com.ismair.cchain.contracts.ContractResponse
import kotlinx.serialization.Serializable

@Serializable
data class DepotRejection(
        override val requestId: Int,
        val request: DepotRequest,
        val message: String
) : ContractResponse