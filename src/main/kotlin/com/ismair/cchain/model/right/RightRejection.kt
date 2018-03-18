package com.ismair.cchain.model.right

import com.ismair.cchain.model.ContractResponse
import kotlinx.serialization.Serializable

@Serializable
data class RightRejection(
        override val requestId: Int,
        val request: RightRequest,
        val message: String
) : ContractResponse()