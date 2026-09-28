package com.dmb.joblog.i18n

import kotlin.test.Test
import kotlin.test.assertEquals

class NoHardCodedTextTest {

    private val accented = Regex(""""[^"\n]*[àâçéèêëîïôûùüÿœÀÂÇÉÈÊËÎÏÔÛÙÜŸŒ«»][^"\n]*"""")

    @Test
    fun androidSources_haveNoFrenchLiteral() {
        val root = StringResources.androidFile("kotlin")
        val offenders = root.walkTopDown().filter { it.extension == "kt" }.flatMap { file ->
            file.readLines().mapIndexedNotNull { i, line ->
                val code = line.trim()
                if (code.startsWith("//") || code.startsWith("*") || code.startsWith("/*")) null
                else if (accented.containsMatchIn(code.substringBefore("//"))) "${file.name}:${i + 1}: ${code.take(90)}" else null
            }
        }.toList()

        assertEquals(emptyList(), offenders, "texte français codé en dur (utiliser strings.xml ou le contenu partagé)")
    }
}
