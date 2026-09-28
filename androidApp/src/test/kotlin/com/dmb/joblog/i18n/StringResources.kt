package com.dmb.joblog.i18n

import java.io.File

object StringResources {

    private fun root(): File = generateSequence(File("").absoluteFile) { it.parentFile }.first { File(it, "settings.gradle.kts").exists() }

    val androidEn: Map<String, String> by lazy { readAndroid("androidApp/src/main/res/values/strings.xml") }
    val androidFr: Map<String, String> by lazy { readAndroid("androidApp/src/main/res/values-fr/strings.xml") }
    val iosEn: Map<String, String> by lazy { readIos("iosApp/iosApp/en.lproj/Localizable.strings") }
    val iosFr: Map<String, String> by lazy { readIos("iosApp/iosApp/fr.lproj/Localizable.strings") }

    fun androidFile(relative: String): File = File(root(), "androidApp/src/main/$relative")
    fun repoFile(relative: String): File = File(root(), relative)

    private fun readAndroid(relative: String): Map<String, String> {
        val text = File(root(), relative).readText()
        return Regex("""<string name="([^"]+)"[^>]*>(.*?)</string>""", RegexOption.DOT_MATCHES_ALL).findAll(text).associate {
            it.groupValues[1] to unescapeAndroid(it.groupValues[2])
        }
    }

    private fun unescapeAndroid(raw: String): String = raw
        .replace("\\'", "'").replace("\\\"", "\"").replace("\\n", "\n")
        .replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">")

    private fun readIos(relative: String): Map<String, String> {
        val text = File(root(), relative).readText()
        return Regex("""^"([^"]+)"\s*=\s*"((?:[^"\\]|\\.)*)"\s*;""", RegexOption.MULTILINE).findAll(text).associate {
            it.groupValues[1] to it.groupValues[2].replace("\\\"", "\"").replace("\\n", "\n").replace("\\\\", "\\")
        }
    }

    fun normalizePlaceholders(value: String): String {
        var counter = 0
        return Regex("""%(\d+\$)?[sd@]""").replace(value) { m ->
            val explicit = m.groupValues[1].removeSuffix("$")
            "%" + (explicit.ifEmpty { (++counter).toString() })
        }
    }
}
