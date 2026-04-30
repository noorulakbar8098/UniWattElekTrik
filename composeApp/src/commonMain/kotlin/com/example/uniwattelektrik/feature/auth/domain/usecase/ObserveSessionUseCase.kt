package com.example.uniwattelektrik.feature.auth.domain.usecase

import com.example.uniwattelektrik.feature.auth.domain.model.AuthSession
import com.example.uniwattelektrik.feature.auth.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow

class ObserveSessionUseCase(private val repository: AuthRepository) {
    operator fun invoke(): Flow<AuthSession?> = repository.observeSession()
}

