package com.ismair.cchain.model.depot

import com.ismair.cchain.ContractResponse
import kotlinx.serialization.Serializable

@Serializable
data class DepotConfirmation(
        override val requestId: Int,
        val request: DepotRequest
) : ContractResponse