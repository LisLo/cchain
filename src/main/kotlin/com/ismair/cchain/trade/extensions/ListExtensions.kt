package com.ismair.cchain.trade.extensions

fun <T> List<T>.forEachNonEqualPair(func: (T, T) -> Unit) {
    this.forEach {
        val first = it
        this.forEach {
            val second = it
            if (first != second) {
                func(first, second)
            }
        }
    }
}