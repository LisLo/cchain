package com.ismair.cchain.cash.model

import com.ismair.cchain.ContractResponse
import kotlinx.serialization.Serializable

@Serializable
data class DepotExecution(
        override val requestId: Int,
        val request: DepotRequest
) : ContractResponse