package com.ismair.cchain.model.right

import com.ismair.cchain.model.ContractRequest
import kotlinx.serialization.Serializable

@Serializable
data class RightRequest(
        val user: String
) : ContractRequest()