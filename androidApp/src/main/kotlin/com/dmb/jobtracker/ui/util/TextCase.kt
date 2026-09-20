package com.dmb.jobtracker.ui.util


fun String.toTitleCase(): String =
    split(" ").joinToString(" ") { word ->
        word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }

fun String.capitalizeFirst(): String =
    replaceFirstChar { it.titlecase() }