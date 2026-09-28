package com.dmb.joblog.ui.i18n

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import com.dmb.joblog.i18n.AppLanguage
import java.util.Locale

@Composable
fun rememberAppLanguage(): AppLanguage {
    val locale: Locale? = LocalConfiguration.current.locales[0]
    return remember(locale) { languageOf(locale ?: Locale.ROOT) }
}

fun currentAppLanguage(): AppLanguage = languageOf(Locale.getDefault())

internal fun languageOf(locale: Locale): AppLanguage = AppLanguage.fromTag(locale.toLanguageTag())
