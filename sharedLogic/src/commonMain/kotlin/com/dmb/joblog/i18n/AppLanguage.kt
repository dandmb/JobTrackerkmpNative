package com.dmb.joblog.i18n

enum class AppLanguage {
    EN, FR;

    companion object {
        val DEFAULT: AppLanguage = EN

        fun fromTag(tag: String?): AppLanguage {
            val language = tag?.trim()?.lowercase()?.split('-', '_')?.firstOrNull().orEmpty()
            return if (language == "fr") FR else DEFAULT
        }
    }
}

internal fun AppLanguage.pick(en: String, fr: String): String = if (this == AppLanguage.FR) fr else en
