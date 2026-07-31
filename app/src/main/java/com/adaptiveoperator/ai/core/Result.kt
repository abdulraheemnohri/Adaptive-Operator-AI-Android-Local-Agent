package com.adaptiveoperator.ai.core

/**
 * Generic result wrapper for suspending operations.
 * Used throughout the app for error handling without exceptions.
 */
sealed class Result<out T> {
    data class Success<out T>(val data: T) : Result<T>()
    data class Error(val message: String, val exception: Throwable? = null) : Result<Nothing>()
}
