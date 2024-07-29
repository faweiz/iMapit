@file:OptIn(ExperimentalMaterial3Api::class)
package tony.imapit.car

import android.location.Location
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.google.android.gms.maps.model.LatLng
import tony.imapit.R

// ##START 100-top-bar
@Composable
fun CarTopBar(
    currentLocation: Location?,
    carLatLng: LatLng?,
    onSetCarLocation: () -> Unit,
    onGoToCurrentLocation: () -> Unit,
    onClearCarLocation: () -> Unit,
    onWalkToCar: () -> Unit,
    onSearchBar: () -> Unit,
    onMapTypeSelector: () -> Unit,
) {
    TopAppBar(
        title = {
//            Text(text = stringResource(id = R.string.app_name))
        },
        actions = {
            currentLocation?.let {
                IconButton(onClick = onSearchBar) {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription =
                        stringResource(id = R.string.search_location),
                    )
                }
                IconButton(onClick = onMapTypeSelector) {
                    Icon(
                        imageVector = Icons.Filled.Menu,
                        contentDescription =
                        stringResource(id = R.string.map_type_selector),
                    )
                }
                IconButton(onClick = onGoToCurrentLocation) {
                    Icon(
                        imageVector = Icons.Filled.GpsFixed,
                        contentDescription =
                        stringResource(id = R.string.go_to_current_location),
                    )
                }
                IconButton(onClick = onSetCarLocation) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription =
                        stringResource(id = R.string.remember_location),
                    )
                }
            }
            carLatLng?.let {
                IconButton(onClick = onWalkToCar) {
                    Icon(
                        imageVector = Icons.Filled.DirectionsWalk,
                        contentDescription =
                        stringResource(id = R.string.navigate),
                    )
                }
                IconButton(onClick = onClearCarLocation) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription =
                        stringResource(id = R.string.forget_location),
                    )
                }
            }
        },
    )
}
// ##END
