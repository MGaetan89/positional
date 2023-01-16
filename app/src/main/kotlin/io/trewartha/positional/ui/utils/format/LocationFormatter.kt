package io.trewartha.positional.ui.utils.format

import io.trewartha.positional.data.location.CoordinatesFormat
import io.trewartha.positional.data.location.Location
import io.trewartha.positional.data.units.Units

interface LocationFormatter {

    fun getAltitude(location: Location, units: Units): String
    fun getAltitudeAccuracy(location: Location, units: Units): String?
    fun getBearing(location: Location): String
    fun getBearingAccuracy(location: Location): String?
    fun getCoordinates(location: Location, format: CoordinatesFormat): Pair<String, Int>
    fun getCoordinatesForCopy(location: Location, format: CoordinatesFormat): String
    fun getCoordinatesAccuracy(location: Location, units: Units): String
    fun getSpeed(location: Location, units: Units): String
    fun getSpeedAccuracy(location: Location, units: Units): String?
    fun getTimestamp(location: Location): String
}
