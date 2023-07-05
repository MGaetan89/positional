package io.trewartha.positional.ui.compass

import android.hardware.SensorManager
import android.hardware.SensorManager.getOrientation
import android.hardware.SensorManager.remapCoordinateSystem
import android.view.Surface
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import io.trewartha.positional.R
import io.trewartha.positional.data.compass.CompassAccuracy
import io.trewartha.positional.data.compass.CompassMode
import io.trewartha.positional.ui.IconButton
import io.trewartha.positional.ui.NavDestination.Compass
import io.trewartha.positional.ui.PositionalTheme
import io.trewartha.positional.ui.ThemePreviews
import io.trewartha.positional.ui.WindowSizePreviews
import io.trewartha.positional.ui.utils.placeholder

fun NavGraphBuilder.compassView(
    onNavigateToInfo: () -> Unit,
) {
    composable(Compass.route) {
        val viewModel: CompassViewModel = hiltViewModel()
        val state by viewModel.state.collectAsStateWithLifecycle()
        CompassView(
            state = state,
            onNavigateToInfo = onNavigateToInfo
        )
    }
}

@Composable
private fun CompassView(
    state: CompassViewModel.State,
    onNavigateToInfo: () -> Unit,
) {
    var showMissingSensorDialog by remember { mutableStateOf(false) }
    if (showMissingSensorDialog) {
        AlertDialog(
            onDismissRequest = { showMissingSensorDialog = false },
            confirmButton = {
                TextButton(onClick = { showMissingSensorDialog = false }) {
                    Text(stringResource(R.string.compass_missing_hardware_dialog_confirm))
                }
            },
            text = {
                Text(
                    stringResource(R.string.compass_missing_hardware_dialog_text),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                )
            }
        )
    }
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                actions = {
                    IconButton(onClick = onNavigateToInfo) {
                        Icon(
                            Icons.Rounded.Info,
                            stringResource(R.string.compass_button_info_content_description),
                        )
                    }
                },
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { contentPadding ->
        when (state) {
            is CompassViewModel.State.SensorsMissing ->
                SensorsMissingContent(
                    onWhyClick = { showMissingSensorDialog = !showMissingSensorDialog },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(contentPadding)
                        .padding(dimensionResource(R.dimen.standard_padding))
                )
            is CompassViewModel.State.SensorsPresent ->
                SensorsPresentContent(
                    state = state,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(contentPadding)
                        .nestedScroll(scrollBehavior.nestedScrollConnection)
                        .verticalScroll(rememberScrollState())
                        .padding(dimensionResource(R.dimen.standard_padding))
                )
        }
    }
}

@Composable
private fun SensorsMissingContent(
    onWhyClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 384.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.Memory,
                contentDescription = null,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.compass_missing_hardware_title),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.compass_missing_hardware_body),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))
            TextButton(onClick = onWhyClick) {
                Text(text = stringResource(id = R.string.compass_missing_hardware_button_why))
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun SensorsPresentContent(
    state: CompassViewModel.State.SensorsPresent,
    modifier: Modifier = Modifier
) {
    var showAccuracyHelpDialog by remember { mutableStateOf(false) }
    if (showAccuracyHelpDialog) {
        AccuracyHelpDialog(onDismissRequest = { showAccuracyHelpDialog = false })
    }
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        val placeholdersVisible = state is CompassViewModel.State.SensorsPresent.Loading
        val remappedRotationMatrix = remember { FloatArray(ROTATION_MATRIX_SIZE) }
        val orientation = remember { FloatArray(ORIENTATION_VECTOR_SIZE) }
        val azimuthDegrees = if (state is CompassViewModel.State.SensorsPresent.Loaded) {
            val (newXAxis, newYAxis) = when (
                val rotation = LocalContext.current.display?.rotation ?: Surface.ROTATION_0
            ) {
                Surface.ROTATION_0 -> SensorManager.AXIS_X to SensorManager.AXIS_Y
                Surface.ROTATION_90 -> SensorManager.AXIS_Y to SensorManager.AXIS_MINUS_X
                Surface.ROTATION_180 -> SensorManager.AXIS_MINUS_Y to SensorManager.AXIS_MINUS_X
                Surface.ROTATION_270 -> SensorManager.AXIS_MINUS_Y to SensorManager.AXIS_X
                else -> error("Unexpected rotation: $rotation")
            }
            remapCoordinateSystem(state.rotationMatrix, newXAxis, newYAxis, remappedRotationMatrix)
            getOrientation(remappedRotationMatrix, orientation)
            val degreesToMagneticNorth =
                (Math.toDegrees(orientation[0].toDouble()).toFloat() + DEGREES_360) % DEGREES_360
            when (state.mode) {
                CompassMode.MAGNETIC_NORTH ->
                    degreesToMagneticNorth
                CompassMode.TRUE_NORTH ->
                    degreesToMagneticNorth - state.magneticDeclinationDegrees
            }
        } else {
            0f
        }
        Compass(
            azimuthDegrees = azimuthDegrees,
            modifier = Modifier
                .weight(1f, fill = true)
                .widthIn(max = 400.dp)
                .placeholder(visible = placeholdersVisible)
        )
        StatsColumn(state = state)
    }
}

@Composable
private fun AccuracyHelpDialog(onDismissRequest: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        icon = { Icon(imageVector = Icons.Rounded.Warning, contentDescription = null) },
        title = { Text(stringResource(R.string.compass_dialog_accuracy_help_title)) },
        text = { Text(stringResource(R.string.compass_dialog_accuracy_help_text)) },
        confirmButton = {
            TextButton(onClick = onDismissRequest) { Text(stringResource(R.string.common_ok)) }
        },
    )
}

private const val DEGREES_360 = 360f
private const val ROTATION_MATRIX_SIZE = 9
private const val ORIENTATION_VECTOR_SIZE = 3

@ThemePreviews
@WindowSizePreviews
@Composable
private fun SensorsMissingPreview() {
    PositionalTheme {
        SensorsMissingContent(onWhyClick = {})
    }
}

@ThemePreviews
@WindowSizePreviews
@Composable
private fun SensorsPresentLoadingPreview() {
    PositionalTheme {
        SensorsPresentContent(state = CompassViewModel.State.SensorsPresent.Loading)
    }
}

@ThemePreviews
@WindowSizePreviews
@Composable
private fun SensorsPresentLoadedPreview() {
    PositionalTheme {
        SensorsPresentContent(
            state = CompassViewModel.State.SensorsPresent.Loaded(
                rotationMatrix = FloatArray(9).apply {
                    // Set the identity rotation matrix
                    set(0, 1f)
                    set(4, 1f)
                    set(8, 1f)
                },
                accelerometerAccuracy = CompassAccuracy.HIGH,
                magnetometerAccuracy = null,
                magneticDeclinationDegrees = 5f,
                mode = CompassMode.TRUE_NORTH,
            )
        )
    }
}
