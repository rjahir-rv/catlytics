package com.catlytics.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.catlytics.core.domain.usecase.library.RefreshLibraryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal sealed interface AppStartupUiState {
    data object WaitingForPermission : AppStartupUiState
    data object Loading : AppStartupUiState
    data object Ready : AppStartupUiState
    data class Error(val message: String) : AppStartupUiState
}

@HiltViewModel
class AppStartupViewModel @Inject constructor(
    private val refreshLibraryUseCase: RefreshLibraryUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow<AppStartupUiState>(
        AppStartupUiState.WaitingForPermission,
    )
    internal val uiState: StateFlow<AppStartupUiState> = _uiState.asStateFlow()

    private var hasStartedLibraryRefresh = false

    fun onAudioPermissionState(hasAudioPermission: Boolean) {
        if (!hasAudioPermission || hasStartedLibraryRefresh) return

        hasStartedLibraryRefresh = true
        _uiState.value = AppStartupUiState.Loading
        viewModelScope.launch {
            _uiState.value = try {
                refreshLibraryUseCase()
                AppStartupUiState.Ready
            } catch (cancellationException: CancellationException) {
                throw cancellationException
            } catch (throwable: Throwable) {
                AppStartupUiState.Error(
                    throwable.message ?: "No se pudo cargar la biblioteca musical.",
                )
            }
        }
    }
}
