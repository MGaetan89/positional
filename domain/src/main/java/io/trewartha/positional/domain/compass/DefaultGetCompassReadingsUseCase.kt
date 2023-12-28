package io.trewartha.positional.domain.compass

import io.trewartha.positional.data.compass.Compass
import io.trewartha.positional.data.location.LocationRepository
import io.trewartha.positional.data.measurement.Angle
import io.trewartha.positional.domain.utils.flow.throttleFirst
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject
import kotlin.time.Duration.Companion.minutes

class DefaultGetCompassReadingsUseCase @Inject constructor(
    private val compass: Compass,
    locationRepository: LocationRepository,
) : GetCompassReadingsUseCase {

    private val magneticDeclination: Flow<Angle?> = locationRepository.location
        .throttleFirst(LOCATION_THROTTLE_PERIOD)
        .map { it.magneticDeclination }
        .onStart { emit(null) }

    override operator fun invoke(): Flow<CompassReadings> =
        combine(
            compass.azimuth,
            magneticDeclination,
        ) { compassAzimuth, magneticDeclination ->
            CompassReadings(compassAzimuth, magneticDeclination)
        }
}

private val LOCATION_THROTTLE_PERIOD = 5.minutes
