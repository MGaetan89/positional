package io.trewartha.positional.ui.location.format

import io.trewartha.positional.model.core.measurement.Coordinates
import io.trewartha.positional.model.core.measurement.Distance
import io.trewartha.positional.model.settings.CoordinatesFormat
import kotlin.math.roundToInt

/**
 * Formats coordinates in the Military Grid Reference System (MGRS)
 */
class MgrsFormatter : CoordinatesFormatter {

    override val format = CoordinatesFormat.MGRS

    override fun formatForDisplay(coordinates: Coordinates?): List<String?> =
        coordinates?.asMgrsCoordinates()?.let { mgrsCoordinates ->
            with(mgrsCoordinates) {
                listOf(
                    "$zone$band $hundredKMSquareID",
                    easting.format(),
                    northing.format(),
                )
            }
        } ?: List(FORMAT_DISPLAY_LINE_COUNT) { null }

    override fun formatForCopy(coordinates: Coordinates): String =
        coordinates.asMgrsCoordinates().toString()

    private fun Distance.format() =
        this.inMeters().value.roundToInt().toString().padStart(NUMERICAL_LOCATION_FORMAT, '0')
}

private const val FORMAT_DISPLAY_LINE_COUNT = 3
private const val NUMERICAL_LOCATION_FORMAT = 5
