package com.techmania.conductorapp.util

/**
 * A generic sealed class that holds a value with its loading status.
 */
sealed class Resource<out T> {
    data class Success<out T>(val data: T) : Resource<T>()
    data class Error(val message: String) : Resource<Nothing>()
    object Loading : Resource<Nothing>() // Use 'object' for a data-less state
}