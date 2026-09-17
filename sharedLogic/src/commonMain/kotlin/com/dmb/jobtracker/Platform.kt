package com.dmb.jobtracker

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform