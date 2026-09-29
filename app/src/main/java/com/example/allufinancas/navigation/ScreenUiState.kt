package com.example.allufinancas.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.allufinancas.data.DemoDataSource
import kotlinx.coroutines.flow.*

internal sealed interface ScreenUiState<out T> {
    data object Loading : ScreenUiState<Nothing>
    data class Empty<T>(val value: T) : ScreenUiState<T>
    data class Content<T>(val value: T) : ScreenUiState<T>
    data class Error(val message: String) : ScreenUiState<Nothing>
}

internal fun <T> observeScreenUiState(
    ready: StateFlow<Boolean>,
    startupError: StateFlow<String?>,
    values: Flow<T>,
    isEmpty: (T) -> Boolean,
): Flow<ScreenUiState<T>> = combine(ready, startupError, values) { isReady, error, value ->
    when {
        error != null -> ScreenUiState.Error(error)
        !isReady -> ScreenUiState.Loading
        isEmpty(value) -> ScreenUiState.Empty(value)
        else -> ScreenUiState.Content(value)
    }
}.catch { exception ->
    emit(ScreenUiState.Error(exception.message ?: "Não foi possível carregar esta tela."))
}

internal abstract class DataScreenViewModel(private val dataSource: DemoDataSource) : ViewModel() {
    protected fun <T> stateFor(values: Flow<T>, isEmpty: (T) -> Boolean): StateFlow<ScreenUiState<T>> =
        observeScreenUiState(dataSource.ready, dataSource.error, values, isEmpty)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ScreenUiState.Loading)
}
