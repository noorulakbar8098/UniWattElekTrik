package com.example.uniwattelektrik.feature.auth.data.remote

private class StubEmployeeLocator : EmployeeLocator {
    override suspend fun findByUid(uid: String): EmployeeAccount? = null
}

actual object EmployeeLocatorFactory {
    actual fun create(): EmployeeLocator = StubEmployeeLocator()
}

