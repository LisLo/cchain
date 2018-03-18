package com.ismair.cchain.model.transfer

import com.ismair.cchain.model.ContractResponse
import kotlinx.serialization.Serializable

@Serializable
data class TransferRejection(
        override val requestId: Int,
        val request: TransferRequest,
        val message: String
) : ContractResponse()