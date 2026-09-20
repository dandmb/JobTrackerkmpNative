package com.dmb.jobtracker.ui.util

/**
 * Vrai si le mot porte une majuscule après sa première lettre : sigle (SQL, QA, UX) ou nom de marque
 * en casse mixte (iOS, iPhone, eBay). Ces mots sont saisis volontairement ainsi : on ne les retouche pas.
 */
private fun String.hasInnerUppercase(): Boolean = drop(1).any { it.isUpperCase() }

fun String.toTitleCase(): String =
    split(" ").joinToString(" ") { word ->
        if (word.hasInnerUppercase()) word
        else word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }

fun String.capitalizeFirst(): String =
    if (substringBefore(' ').hasInnerUppercase()) this
    else replaceFirstChar { it.titlecase() }
