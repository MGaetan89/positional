package io.trewartha.positional.ui.compass

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.trewartha.positional.data.compass.CompassHardwareException
import io.trewartha.positional.data.compass.CompassMode
import io.trewartha.positional.data.settings.SettingsRepository
import io.trewartha.positional.domain.compass.CompassReading
import io.trewartha.positional.domain.compass.GetCompassReadingsUseCase
import io.trewartha.positional.ui.utils.flow.ForViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class CompassViewModel @Inject constructor(
    getCompassReadingsUseCase: GetCompassReadingsUseCase,
    settingsRepository: SettingsRepository
) : ViewModel() {

    val state: StateFlow<State> =
        combine<CompassReading, CompassMode, State>(
            getCompassReadingsUseCase(),
            settingsRepository.compassMode
        ) { readings, mode ->
            State.SensorsPresent.Loaded(readings, mode)
        }.catch { throwable ->
            when (throwable) {
                is CompassHardwareException -> {
                    emit(State.SensorsMissing)
                }
                else -> {
                    throw throwable
                }
            }
        }.stateIn(
            viewModelScope,
            SharingStarted.ForViewModel,
            initialValue = State.SensorsPresent.Loading
        )

    sealed interface State {

        data object SensorsMissing : State

        sealed interface SensorsPresent : State {

            data object Loading : SensorsPresent

            data class Loaded(
                val compassReading: CompassReading,
                val compassMode: CompassMode
            ) : SensorsPresent
        }
    }
}
