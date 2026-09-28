package com.dmb.joblog.ui.about

import android.content.Context
import androidx.core.content.pm.PackageInfoCompat
import com.dmb.joblog.BuildConfig
import com.dmb.joblog.presentation.about.AboutContent

fun appVersionLabel(context: Context, content: AboutContent): String {
    val info = try {
        context.packageManager.getPackageInfo(context.packageName, 0)
    } catch (_: Exception) {
        return withStagingMarker(content.versionLabel("", ""))
    }
    return withStagingMarker(content.versionLabel(info.versionName.orEmpty(), PackageInfoCompat.getLongVersionCode(info).toString()))
}

private fun withStagingMarker(label: String): String = if (BuildConfig.IS_STAGING) "$label · STG" else label
