package com.ismair.cchain.cash.model

import com.ismair.cchain.contract.ContractRequest
import kotlinx.serialization.Serializable

@Serializable
data class TransferRequest(
        val user: String,
        val amount: Int,
        val purpose: String
) : ContractRequest