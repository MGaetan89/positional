package io.trewartha.positional.ui.location

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.Adjust
import androidx.compose.material.icons.twotone.Explore
import androidx.compose.material.icons.twotone.Speed
import androidx.compose.material.icons.twotone.Terrain
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.trewartha.positional.R
import io.trewartha.positional.ui.Divider
import io.trewartha.positional.ui.PositionalTheme
import io.trewartha.positional.ui.ThemePreviews
import io.trewartha.positional.ui.WindowSizePreviews

@Composable
fun StatsColumn(stats: LocationState.Stats?) {
    Column {
        val accuracyVisible = stats?.showAccuracies ?: false
        val placeholdersVisible = stats == null
        val dividerIndent = 16.dp
        LocationStatRow(
            icon = Icons.TwoTone.Adjust,
            name = stringResource(R.string.location_label_accuracy),
            value = stats?.accuracy,
            accuracy = null,
            accuracyVisible = accuracyVisible,
            placeholdersVisible = placeholdersVisible
        )
        Divider(modifier = Modifier.padding(horizontal = dividerIndent))
        LocationStatRow(
            icon = Icons.TwoTone.Explore,
            name = stringResource(R.string.location_label_bearing),
            value = stats?.bearing,
            accuracy = stats?.bearingAccuracy,
            accuracyVisible = accuracyVisible,
            placeholdersVisible = placeholdersVisible
        )
        Divider(modifier = Modifier.padding(horizontal = dividerIndent))
        LocationStatRow(
            icon = Icons.TwoTone.Terrain,
            name = stringResource(R.string.location_label_elevation),
            value = stats?.elevation,
            accuracy = stats?.elevationAccuracy,
            accuracyVisible = accuracyVisible,
            placeholdersVisible = placeholdersVisible
        )
        Divider(modifier = Modifier.padding(horizontal = dividerIndent))
        LocationStatRow(
            icon = Icons.TwoTone.Speed,
            name = stringResource(R.string.location_label_speed),
            value = stats?.speed,
            accuracy = stats?.speedAccuracy,
            accuracyVisible = accuracyVisible,
            placeholdersVisible = placeholdersVisible
        )
        Text(
            text = stats?.updatedAt ?: "",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier
                .padding(top = 12.dp, bottom = 24.dp)
                .align(Alignment.CenterHorizontally)
        )
    }
}

@ThemePreviews
@WindowSizePreviews
@Composable
private fun Preview() {
    PositionalTheme {
        Surface {
            StatsColumn(
                LocationState.Stats(
                    accuracy = "123.4",
                    bearing = "123.4",
                    bearingAccuracy = null,
                    elevation = "123.4",
                    elevationAccuracy = null,
                    speed = "123.4",
                    speedAccuracy = null,
                    showAccuracies = true,
                    updatedAt = "Updated at 12:00:00 PM"
                )
            )
        }
    }
}