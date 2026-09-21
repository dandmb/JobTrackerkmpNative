package com.dmb.joblog

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform