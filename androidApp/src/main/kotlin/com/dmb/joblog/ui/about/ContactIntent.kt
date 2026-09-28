package com.dmb.joblog.ui.about

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import com.dmb.joblog.presentation.about.AboutContent

fun contactIntent(): Intent = Intent(Intent.ACTION_SENDTO, AboutContent.contactMailtoUri().toUri())

fun openMailApp(context: Context): Boolean =
    try {
        context.startActivity(contactIntent().addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (_: ActivityNotFoundException) {
        false
    }
