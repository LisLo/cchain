package com.ismair.cchain.contracts.cash.model

import com.ismair.cchain.contracts.ContractResponse
import kotlinx.serialization.Serializable

@Serializable
data class TransferConfirmation(
        override val requestId: Int,
        val payer: String,
        val payee: String,
        val amount: Int,
        val purpose: String
) : ContractResponse