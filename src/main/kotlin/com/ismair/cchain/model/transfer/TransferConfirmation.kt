package com.ismair.cchain.model.transfer

import com.ismair.cchain.abstracts.ContractResponse
import kotlinx.serialization.Serializable

@Serializable
data class TransferConfirmation(
        override val requestId: Int,
        val payer: String,
        val payee: String,
        val amount: Int,
        val purpose: String
) : ContractResponse()