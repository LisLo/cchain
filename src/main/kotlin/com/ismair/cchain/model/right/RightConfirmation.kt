package com.ismair.cchain.model.right

import com.ismair.cchain.model.ContractResponse
import kotlinx.serialization.Serializable

@Serializable
data class RightConfirmation(
        override val requestId: Int,
        val user: String
) : ContractResponse()