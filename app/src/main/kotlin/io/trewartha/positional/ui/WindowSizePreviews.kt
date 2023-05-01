package io.trewartha.positional.ui

import androidx.compose.ui.tooling.preview.Preview

private const val PHONE_HEIGHT_DP = 891
private const val PHONE_WIDTH_DP = 411
private const val TABLET_HEIGHT_DP = 1280
private const val TABLET_WIDTH_DP = 800

@Preview(
    name = "Phone Portrait",
    group = "Phone Size",
    widthDp = PHONE_WIDTH_DP,
    heightDp = PHONE_HEIGHT_DP,
    showBackground = true
)
@Preview(
    name = "Phone Landscape",
    group = "Phone Size",
    widthDp = PHONE_HEIGHT_DP,
    heightDp = PHONE_WIDTH_DP,
    showBackground = true
)
@Preview(
    name = "Tablet Portrait",
    group = "Tablet Size",
    widthDp = TABLET_WIDTH_DP,
    heightDp = TABLET_HEIGHT_DP,
    showBackground = true
)
@Preview(
    name = "Tablet Landscape",
    group = "Tablet Size",
    widthDp = TABLET_HEIGHT_DP,
    heightDp = TABLET_WIDTH_DP,
    showBackground = true
)
annotation class WindowSizePreviews
