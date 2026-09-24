package com.playdice.pickone.data

internal inline fun <reified T : Enum<T>> String?.enumOrDefault(default: T): T =
    this?.trim()?.let { value -> enumValues<T>().firstOrNull { it.name.equals(value, ignoreCase = true) } } ?: default
