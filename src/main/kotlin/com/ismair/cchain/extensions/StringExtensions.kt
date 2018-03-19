package com.ismair.cchain.extensions

fun String.shrink() =
        this.toLowerCase()
                .replace(" ", "")
                .replace("\t", "")
                .replace("\n", "")
                .replace("\r", "")