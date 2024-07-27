package tony.imapit.map

import android.content.Intent
import android.location.Location
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.StreetViewPanoramaOptions
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.Status
import com.google.maps.android.StreetViewUtils.Companion.fetchStreetViewData
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.DragState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MarkerInfoWindowContent
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberMarkerState
import com.google.maps.android.compose.streetview.StreetView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.StreetViewSource
import com.google.maps.android.compose.Marker
import kotlinx.coroutines.withContext
import tony.imapit.BuildConfig.MAPS_API_KEY
import tony.imapit.R
import tony.imapit.car.CarTopBar

@Composable
fun GoogleMapDisplay(
    currentLocation: Location?,
    carLatLng: LatLng?,
    cameraPositionState: CameraPositionState,
    onSetCarLocation: () -> Unit,
    onClearCarLocation: () -> Unit,
    // ##START 130-event
    onMoveCar: (LatLng) -> Unit,
    // ##END
    modifier: Modifier,
) {
    // ##START 150-density-padding
    with(LocalDensity.current) {
        val boundsPadding = 48.dp.toPx()
        // ##END

        // ##START 060-marker-state-holder
        var mapLoaded by remember { mutableStateOf(false) }
        // ##END

        // ##START 070-current-map-type
        var currentMapType by remember { mutableStateOf(MapType.HYBRID) }

//        var mapProperties by remember(currentMapType) {
//            mutableStateOf(MapProperties(mapType = currentMapType))
//        }
        var mapProperties by remember(currentMapType) {
            mutableStateOf(MapProperties(
                isBuildingEnabled = true,
                isIndoorEnabled = true,
                isTrafficEnabled = true,
                isMyLocationEnabled = true,
                mapType = currentMapType),
            )
        }

//        var mapProperties by remember {
//            mutableStateOf(MapProperties(mapType = MapType.SATELLITE))
//        }
        // ##END

        // ##START 080-current-location-state
        val currentLocationState = currentLocation?.let {
                LatLng(it.latitude, it.longitude)
            }?.let {
                rememberMarkerState(position = it)
            }
        // ##END

        val carState = rememberMarkerState("car")

        // ##START 090-icon-state
        val context = LocalContext.current
        var currentLocationIcon by
        remember { mutableStateOf<BitmapDescriptor?>(null) }
        var carIcon by remember { mutableStateOf<BitmapDescriptor?>(null) }
        val scope = rememberCoroutineScope()
        // ##END

        // ##START 090-jump-to-location
        var initialBoundsSet by remember { mutableStateOf(false) }

        // Street View variables
        var carShowStreetView by remember { mutableStateOf(false) }
        var carShowStreetViewChanged by remember { mutableStateOf(false) }
        var currentShowStreetView by remember { mutableStateOf(false) }
        var streetViewResultReturn by remember { mutableStateOf(Status.NOT_FOUND) }
        var previousLatLng by remember { mutableStateOf<LatLng?>(null) }

        // Point of Interest (POI) Location marker
        var poiLocationName by remember { mutableStateOf(String ?: "") }
        var poiLocation by remember { mutableStateOf<LatLng?>(null) }
        var poiIcon by remember { mutableStateOf<BitmapDescriptor?>(null) }
        var onMapLongClickFlag by remember { mutableStateOf(false) }


        // ##START 150-jump-to-bounds
        LaunchedEffect(key1 = currentLocation) {
            if (currentLocation != null) {
                if (!initialBoundsSet) {
                    initialBoundsSet = true
                    val current =
                        LatLng(currentLocation.latitude, currentLocation.longitude)
                    carLatLng?.let { car ->
                        val bounds =
                            LatLngBounds(current, current).including(car)
                        cameraPositionState.animate(
                            CameraUpdateFactory.newLatLngBounds(
                                bounds,
                                boundsPadding.toInt()
                            ), 1000
                        )
                    } ?: run {
                        cameraPositionState.animate(
                            CameraUpdateFactory.newLatLngZoom(
                                current,
                                16f
                            ), 1000
                        )
                    }
                }
            }
        }

        // ##START 130-snapshot-flow
        LaunchedEffect(true) {
            var dragged = false

            snapshotFlow { carState.dragState }
                .collect {
                    // Make sure we've seen at least one drag state before updating
                    //   the view model. Otherwise we'll see the initial (0.0, 0.0)
                    //   value that was set when the MarkerState was created
                    if (it == DragState.DRAG) {
                        dragged = true

                    } else if (it == DragState.END && dragged) {
                        dragged = false
                        onMoveCar(carState.position)
                    }
                }
        }
        // ##END

        Scaffold(
            topBar = {
                CarTopBar(
                    currentLocation = currentLocation,
                    carLatLng = carLatLng,
                    onSetCarLocation = onSetCarLocation,
                    onClearCarLocation = onClearCarLocation,
                    // ##START 140-navigate
                    onWalkToCar = {
                        currentLocation?.let { curr ->
                            carLatLng?.let { car ->
                                val uri =
                                    Uri.parse(
                                        "https://www.google.com/maps/dir/" +
                                                "?api=1&origin=${curr.latitude}," +
                                                "${curr.longitude}&" +
                                                "destination=${car.latitude}," +
                                                "${car.longitude}&travelmode=walking"
                                    )
                                context.startActivity(
                                    Intent(
                                        Intent.ACTION_VIEW,
                                        uri
                                    ).apply {
                                        setPackage("com.google.android.apps.maps")
                                    })
                            } ?: Toast.makeText(
                                context,
                                "Cannot navigate; no car location available",
                                Toast.LENGTH_LONG
                            ).show()
                        } ?: Toast.makeText(
                            context,
                            "Cannot navigate; no current location available",
                            Toast.LENGTH_LONG
                        ).show()
                    },
                    // ##END
                    onGoToCurrentLocation = {
                        currentLocation?.let { curr ->
                            scope.launch {
                                cameraPositionState.animate(
                                    CameraUpdateFactory.newLatLngZoom(
                                        LatLng(curr.latitude, curr.longitude),
                                        20f
                                    ), 1000
                                )
                            }
                        } ?: Toast.makeText(
                            context,
                            "No current location available",
                            Toast.LENGTH_LONG
                        ).show()
                    },
                )
            },
            content = { paddingValues ->
                Box(
                    // ##START 060-top-level-modifier
                    modifier = modifier.padding(paddingValues),
                    // ##END
                ) {

                    // ##START 070-column
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        MapTypeSelector(
                            currentValue = currentMapType,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            mapProperties = mapProperties.copy(mapType = it)
                            currentMapType = it
                        }
                        // ##END

                        GoogleMap(
                            // ##START 050-use-camera-position-state
                            cameraPositionState = cameraPositionState,
                            // ##END
                            // ##START 060-set-mapLoaded
                            onMapLoaded = {
                                mapLoaded = true
                                // ##START 090-load-icon
                                scope.launch(Dispatchers.IO) {
                                    currentLocationIcon =
                                        context.loadBitmapDescriptor(
                                            R.drawable.ic_current_location
                                        )
                                    carIcon =
                                        context.loadBitmapDescriptor(
                                            R.drawable.ic_car
                                        )
                                    poiIcon =
                                        context.loadBitmapDescriptor(
                                        R.drawable.ic_ufo_flying
                                    )
                                }
                                // ##END
                            },
                            // ##END
                            // ##START 070-pass-properties
                            properties = mapProperties,
                            // ##END
                            // ##START 040-map-modifier
                            // ##START 060-map-modifier-change
                            // ##START 070-map-weight
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            // ##END
                            onMapLongClick ={
                                onMapLongClickFlag = true
                            },
                            onPOIClick = {
                                onMapLongClickFlag = false
                                poiLocationName = it.name
                                poiLocation = it.latLng
                            }
                            // ##END
                            // ##END
                        ) {
                            // ##START 080-marker for current location
                            currentLocationState?.let {
                                MarkerInfoWindowContent(
                                    state = currentLocationState,
                                    // ##START 090-icon-offset
                                    icon = currentLocationIcon,
                                    anchor = Offset(0.5f, 0.5f),
                                    // ##END
                                    title = stringResource(
                                        id = R.string.current_location
                                    ),
                                    //snippet = "Latitude: ${it.position.latitude}, Longitude: ${it.position.longitude}",
                                    content = {
                                        Column {
                                            Text(text = stringResource( id = R.string.current_location), fontWeight = FontWeight.Bold)
                                            Text(text = "Latitude: ${currentLocationState.position.latitude}")
                                            Text(text = "Longitude: ${currentLocationState.position.longitude}")
                                        }
                                    },
                                    onClick = {
                                        //currentShowStreetView = true
                                        currentLocationState.showInfoWindow()
                                        true
                                    },
                                    onInfoWindowClick = { currentShowStreetView = true },
                                )
                            }
                            // marker for car location
                            carLatLng?.let {
                                carState.position = it
                                MarkerInfoWindowContent(
                                    state = carState,
                                    // ##START 130-draggable
                                    draggable = true,
                                    // ##END
                                    icon = carIcon,
                                    anchor = Offset(0.5f, 0.5f),
                                    title = stringResource(
                                        id = R.string.car_location
                                    ),
                                    onClick = {
                                        carState.showInfoWindow()
                                        true
                                    },
                                    onInfoWindowClick = { carShowStreetView = true },
                                )
                            }

                            if(!onMapLongClickFlag) {
                                poiLocation?.let {
                                    val poiState = rememberMarkerState(position = it)
                                    MarkerInfoWindowContent(
                                        state = poiState,
//                                        icon = poiIcon,
                                        anchor = Offset(0.5f, 0.5f),
                                        title = poiLocationName.toString(),
                                        onClick = {
                                            poiState.showInfoWindow()
                                            true
                                        },
                                    )
                                }
                            }
                        } // End of GoogleMap()


                        if(carShowStreetView || currentShowStreetView) {
                            if (carShowStreetView)
                                streetViewResultReturn = StreetViewValidator(carLatLng)
                            if (currentShowStreetView) {
                                val currentLoc =
                                    currentLocationState?.position?.let {
                                        LatLng(
                                            it.latitude,
                                            it.longitude
                                        )
                                    }
                                streetViewResultReturn = StreetViewValidator(currentLoc)
                            }
                            LaunchedEffect(carLatLng) {
                                if (carLatLng != previousLatLng) {
                                    previousLatLng = carLatLng
                                    carShowStreetViewChanged = true
                                }
                            }

                            if (carShowStreetViewChanged ) {
                                if (streetViewResultReturn == Status.OK) {
                                    if (carLatLng != previousLatLng) {
                                        // Render Street View for carLatLng
                                        streetViewResultReturn = StreetViewValidator(carLatLng)
                                        carShowStreetViewChanged = false
                                        carShowStreetView = false
                                    }
                                }
                            }
                            // Return/exit if street view not available
                            if (streetViewResultReturn == Status.ZERO_RESULTS) {
                                carShowStreetView = false
                                currentShowStreetView = false
                            }
                            BackHandler {
                                carShowStreetView = false
                                currentShowStreetView = false
                                previousLatLng = null
                            }
                        } // End of if (carShowStreetView || currentShowStreetView)
                    } // End of Column
                } // End of Box

                // ##START 060-progress-spinner
                if (!mapLoaded) {
                    AnimatedVisibility(
                        visible = true,
                        modifier = Modifier.fillMaxSize(),
                        enter = EnterTransition.None,
                        exit = fadeOut()
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.background)
                                .wrapContentSize()
                        )
                    }
                }
                // ##END
            } // End of content
        ) // End of Scaffold
    } // End of with(LocalDensity.current)
}


@Composable
fun StreetViewValidator(targetLatLng: LatLng?) : Status{
    var streetViewResult by remember { mutableStateOf(Status.ZERO_RESULTS) }
    var streetViewLoaded by remember { mutableStateOf(true) }
    val context = LocalContext.current

    LaunchedEffect(true) {
        targetLatLng?.let {
            streetViewResult = withContext(Dispatchers.IO) {
                fetchStreetViewData(it, MAPS_API_KEY)
            }
        }
    }

    if (streetViewResult == Status.OK) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp)),
            Alignment.BottomStart
        ){
            StreetView(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .height(200.dp),
                streetViewPanoramaOptionsFactory = {
                    StreetViewPanoramaOptions().position(targetLatLng, StreetViewSource.OUTDOOR)
                },
            )
        }
    } else if (streetViewResult == Status.ZERO_RESULTS && !streetViewLoaded) {
        Toast.makeText(context, "Street View not available.", Toast.LENGTH_SHORT).show()
    }
    if(streetViewLoaded){
        streetViewLoaded = false
        streetViewResult = Status.NOT_FOUND
    }

    return streetViewResult
}


//fun onLocationUpdated(poiLocation: String, latitude: Double, longitude: Double) {

//@Composable
fun onLocationUpdated(poiLocation: String, position: LatLng) {
// marker for car location
//    MarkerInfoWindowContent{
//
//    }


//    carLatLng?.let {
//        carState.position = it
//        MarkerInfoWindowContent(
//            state = carState,
//            // ##START 130-draggable
//            draggable = true,
//            // ##END
//            icon = carIcon,
//            anchor = Offset(0.5f, 0.5f),
//            title = stringResource(
//                id = R.string.car_location
//            ),
//            onClick = {
//                carState.showInfoWindow()
//                true
//            },
//            onInfoWindowClick = { carShowStreetView = true },
//        )
//    }


}