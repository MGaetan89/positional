package io.trewartha.positional.ui.location

import kotlinx.datetime.Instant

sealed interface LocationEvent {

    data class NavigateToGeoActivity(
        val latitude: Double,
        val longitude: Double,
        val instant: Instant
    ) : LocationEvent

    object NavigateToLocationHelp : LocationEvent

    object NavigateToSettings : LocationEvent

    object ShowScreenLockedSnackbar : LocationEvent

    object ShowScreenUnlockedSnackbar : LocationEvent

    object ShowCoordinatesCopyErrorSnackbar : LocationEvent

    object ShowCoordinatesCopySuccessBothSnackbar : LocationEvent

    object ShowCoordinatesCopySuccessLatitudeSnackbar : LocationEvent

    object ShowCoordinatesCopySuccessLongitudeSnackbar : LocationEvent

    object ShowCoordinatesShareErrorSnackbar : LocationEvent

    data class ShowCoordinatesShareSheet(val coordinates: String) : LocationEvent
}
