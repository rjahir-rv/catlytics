package com.catlytics.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.catlytics.core.domain.usecase.library.ObserveLibraryChangesUseCase
import com.catlytics.core.domain.usecase.library.RefreshLibraryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

internal sealed interface AppStartupUiState {
    data object WaitingForPermission : AppStartupUiState
    data object Loading : AppStartupUiState
    data object Ready : AppStartupUiState
    data class Error(val message: String) : AppStartupUiState
}

@HiltViewModel
class AppStartupViewModel @Inject constructor(
    private val refreshLibraryUseCase: RefreshLibraryUseCase,
    private val observeLibraryChangesUseCase: ObserveLibraryChangesUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow<AppStartupUiState>(
        AppStartupUiState.WaitingForPermission,
    )
    internal val uiState: StateFlow<AppStartupUiState> = _uiState.asStateFlow()

    private var hasStartedLibraryRefresh = false
    private var hasStartedLibraryObservation = false

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
            observeLibraryChanges()
        }
    }

    @OptIn(FlowPreview::class)
    private fun observeLibraryChanges() {
        if (hasStartedLibraryObservation) return

        hasStartedLibraryObservation = true
        viewModelScope.launch {
            observeLibraryChangesUseCase()
                .debounce(LIBRARY_CHANGE_DEBOUNCE_MILLIS.milliseconds)
                .collect {
                    runCatching { refreshLibraryUseCase() }
                }
        }
    }

    private companion object {
        const val LIBRARY_CHANGE_DEBOUNCE_MILLIS = 2_000L
    }
}
