package com.dmb.joblog.ui.util

// Sigles (SQL, QA, UX) et marques en casse mixte (iOS, eBay) : casse volontaire, jamais retouchée.
private fun String.hasInnerUppercase(): Boolean = drop(1).any { it.isUpperCase() }

fun String.toTitleCase(): String =
    split(" ").joinToString(" ") { word ->
        if (word.hasInnerUppercase()) word
        else word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }

fun String.capitalizeFirst(): String =
    if (substringBefore(' ').hasInnerUppercase()) this
    else replaceFirstChar { it.titlecase() }
