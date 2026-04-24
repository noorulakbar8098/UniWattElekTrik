package com.example.uniwattelektrik

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform