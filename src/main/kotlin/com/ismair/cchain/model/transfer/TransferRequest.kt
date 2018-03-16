package com.ismair.cchain.model.transfer

import com.ismair.cchain.abstracts.ContractRequest
import kotlinx.serialization.Serializable

@Serializable
data class TransferRequest(
        val payee: String,
        val amount: Int,
        val purpose: String
) : ContractRequest()