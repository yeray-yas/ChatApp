package com.yerayyas.chatappkotlinproject.domain.model

/**
 * Representa los diferentes estados de la UI para operaciones asíncronas
 */
sealed interface UiState<out T> {
    data object Idle : UiState<Nothing>
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(
        val exception: Exception? = null,
        val message: String = "Ha ocurrido un error"
    ) : UiState<Nothing>
}

/**
 * Estados específicos para listas
 */
sealed interface ListUiState<out T> : UiState<T> {
    data object Empty : ListUiState<Nothing>
}

/**
 * Extension functions para facilitar el manejo de estados
 */
fun <T> UiState<T>.isLoading(): Boolean = this is UiState.Loading
fun <T> UiState<T>.isSuccess(): Boolean = this is UiState.Success
fun <T> UiState<T>.isError(): Boolean = this is UiState.Error
fun <T> UiState<T>.isIdle(): Boolean = this is UiState.Idle

fun <T> UiState<T>.getDataOrNull(): T? = when (this) {
    is UiState.Success -> data
    else -> null
}

fun <T> UiState<T>.getErrorOrNull(): UiState.Error? = when (this) {
    is UiState.Error -> this
    else -> null
}