package com.ismair.cchain.model.cheat

import com.ismair.cchain.abstracts.ContractRequest
import kotlinx.serialization.Serializable

@Serializable
data class CheatRequest(
        val mode: CheatMode
) : ContractRequest()