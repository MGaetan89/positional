package io.trewartha.positional.ui.sun

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.trewartha.positional.data.location.Location
import io.trewartha.positional.data.sun.SolarTimes
import io.trewartha.positional.domain.location.GetLocationUseCase
import io.trewartha.positional.domain.sun.GetSolarTimesUseCase
import io.trewartha.positional.ui.utils.flow.ForViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.retry
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import timber.log.Timber
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

/**
 * View model for the Sun view
 */
@HiltViewModel
class SunViewModel @Inject constructor(
    getLocationUseCase: GetLocationUseCase,
    private val getDailyTwilightTimes: GetSolarTimesUseCase,
) : ViewModel() {

    private val location: Flow<Location> =
        getLocationUseCase()
            .retry { cause ->
                if (cause is SecurityException) {
                    Timber.w("Waiting for location permissions to be granted")
                    delay(1.seconds)
                    true
                } else {
                    throw cause
                }
            }
            .shareIn(viewModelScope, SharingStarted.ForViewModel, replay = 1)

    private val today: LocalDate
        get() = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

    /**
     * The currently selected date
     */
    val selectedDate: StateFlow<LocalDate>
        get() = _date
    private val _date = MutableStateFlow(today)

    /**
     * Twilight times on the selected date at the device's current location
     */
    val selectedDateTwilights: StateFlow<SolarTimes?> =
        combine(selectedDate, location) { date, location ->
            getDailyTwilightTimes(location.coordinates, date)
        }.stateIn(viewModelScope, SharingStarted.ForViewModel, initialValue = null)

    /**
     * Today's date
     */
    val todaysDate: StateFlow<LocalDate> =
        flow {
            while (viewModelScope.isActive) {
                emit(today)
                delay(1.seconds)
            }
        }.distinctUntilChanged().stateIn(viewModelScope, SharingStarted.ForViewModel, today)

    /**
     * Callback to tell the view model that the user has selected a date
     */
    fun onSelectedDateChange(date: LocalDate) {
        _date.update { date }
    }

    /**
     * Callback to tell the view model that the user wants to change the selected date to today
     */
    fun onSelectedDateChangedToToday() {
        _date.update { today }
    }

    /**
     * Callback to tell the view model that the user wants to decrement the selected date by one day
     */
    fun onSelectedDateDecrement() {
        _date.update { it - DatePeriod(days = 1) }
    }

    /**
     * Callback to tell the view model that the user wants to increment the selected date by one day
     */
    fun onSelectedDateIncrement() {
        _date.update { it + DatePeriod(days = 1) }
    }
}
