package com.example.uniwattelektrik.core

/**
 * Lightweight success/failure wrapper used across the data ↔ domain boundary.
 * We use our own type (instead of [kotlin.Result]) to make failures a first-class
 * domain concept ([AppError]) and keep the code framework-agnostic.
 */
sealed class Resource<out T> {
    data class Success<T>(val data: T) : Resource<T>()
    data class Failure(val error: AppError) : Resource<Nothing>()

    inline fun <R> map(transform: (T) -> R): Resource<R> = when (this) {
        is Success -> Success(transform(data))
        is Failure -> this
    }

    companion object {
        fun <T> success(data: T): Resource<T> = Success(data)
        fun failure(error: AppError): Resource<Nothing> = Failure(error)
    }
}

