package com.ismair.cchain.cash.model

import com.ismair.cchain.contract.ContractResponse
import kotlinx.serialization.Serializable

@Serializable
data class DepotRejection(
        override val requestId: Int,
        val request: DepotRequest,
        val message: String
) : ContractResponse