package com.ismair.cchain.cash.model

import com.ismair.cchain.contract.ContractResponse
import kotlinx.serialization.Serializable

@Serializable
data class TransferRejection(
        override val requestId: Int,
        val request: TransferRequest,
        val message: String
) : ContractResponse