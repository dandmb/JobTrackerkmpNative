package com.dmb.jobtracker.ui.about

import android.content.Context
import androidx.core.content.pm.PackageInfoCompat
import com.dmb.jobtracker.presentation.about.AboutContent

/**
 * Libellé de version lu dans la configuration de build réelle de l'APK installé (`versionName` / `versionCode` de
 * `androidApp/build.gradle.kts`, via le `PackageManager`) : rien n'est codé en dur, il reste juste à chaque changement de version.
 */
fun appVersionLabel(context: Context): String {
    val info = try {
        context.packageManager.getPackageInfo(context.packageName, 0)
    } catch (_: Exception) {
        return AboutContent.versionLabel("", "")
    }
    return AboutContent.versionLabel(info.versionName.orEmpty(), PackageInfoCompat.getLongVersionCode(info).toString())
}
