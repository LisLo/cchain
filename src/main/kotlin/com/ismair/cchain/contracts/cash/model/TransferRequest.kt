package com.ismair.cchain.contracts.cash.model

import com.ismair.cchain.contracts.ContractRequest
import kotlinx.serialization.Serializable

@Serializable
data class TransferRequest(
        val payee: String,
        val amount: Int,
        val purpose: String
) : ContractRequest