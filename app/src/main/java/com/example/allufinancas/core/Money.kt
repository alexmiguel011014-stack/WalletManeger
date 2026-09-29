package com.example.allufinancas.core

import java.text.NumberFormat
import java.util.Locale

internal fun brl(cents: Long, showCents: Boolean = true): String =
    NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR")).apply {
        minimumFractionDigits = if (showCents) 2 else 0
        maximumFractionDigits = if (showCents) 2 else 0
    }.format(cents / 100.0)
