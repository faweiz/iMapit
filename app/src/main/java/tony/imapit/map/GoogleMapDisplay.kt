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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MarkerInfoWindowContent
import com.google.maps.android.compose.rememberMarkerState
import com.google.maps.android.compose.streetview.StreetView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import com.google.android.gms.maps.model.StreetViewSource
import kotlinx.coroutines.withContext
import tony.imapit.BuildConfig.MAPS_API_KEY
import tony.imapit.R
import tony.imapit.car.CarTopBar

import tony.imapit.search.LocationSearchViewModel
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.graphics.Color

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
    searchViewModel: LocationSearchViewModel // Injecting LocationSearchViewModel
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

        // Google Map Properties
        var mapProperties by remember(currentMapType) {
            mutableStateOf(MapProperties(
                isBuildingEnabled = true,
                isIndoorEnabled = true,
                isTrafficEnabled = true,
//                isMyLocationEnabled = true,
                mapType = currentMapType),
            )
        }

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
        var carShowStreetViewFlag by remember { mutableStateOf(false) }
        var carShowStreetViewChangedFlag by remember { mutableStateOf(false) }
        var currentShowStreetViewFlag by remember { mutableStateOf(false) }
        var streetViewResultReturn by remember { mutableStateOf(Status.NOT_FOUND) }
        var previousLatLng by remember { mutableStateOf<LatLng?>(null) }

        // Point of Interest (POI) Location marker
        var poiLocationName by remember { mutableStateOf(String ?: "") }
        var poiLocation by remember { mutableStateOf<LatLng?>(null) }
        var poiIcon by remember { mutableStateOf<BitmapDescriptor?>(null) }
        var poiClickFlag by remember { mutableStateOf(false) }

        // Search bar
        var searchLatLong by remember { mutableStateOf<LatLng?>(null) }
        var searchChangeFlag by remember { mutableStateOf(false) }
        var searchLatLongFlag by remember { mutableStateOf(false) }
        var onSearchBarFlag by remember { mutableStateOf(false) }

        var onMapTypeSelectorFlag by remember { mutableStateOf(false) }

        LaunchedEffect(key1 = currentLocation) {
            if (currentLocation != null) {
                if (!initialBoundsSet) {
                    val current =
                        LatLng(currentLocation.latitude, currentLocation.longitude)

                    if(carLatLng != null){
                        carLatLng?.let { car ->
                            val bounds =
                                LatLngBounds(current, current).including(car)
                            cameraPositionState.animate(
                                CameraUpdateFactory.newLatLngBounds(
                                    bounds,
                                    boundsPadding.toInt()
                                ), 1000
                            )
                            initialBoundsSet = true
                        }
                    }else{
                        cameraPositionState.animate(
                            CameraUpdateFactory.newLatLngZoom(
                                current,
                                15f
                            ), 1000
                        )
                        initialBoundsSet = true
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
                                        18f
                                    ), 1000
                                )
                            }
                        } ?: Toast.makeText(
                            context,
                            "No current location available",
                            Toast.LENGTH_LONG
                        ).show()
                    },
                    onSearchBar = {
                        onSearchBarFlag = true
                    },
                    onMapTypeSelector = {
                        onMapTypeSelectorFlag = true
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

                        if (onMapTypeSelectorFlag) {
                            MapTypeSelector(
                                currentValue = currentMapType,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                mapProperties = mapProperties.copy(mapType = it)
                                currentMapType = it
                                onMapTypeSelectorFlag = false
                            }
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
                            onMapClick = {
                                scope.launch {
                                    cameraPositionState.animate(CameraUpdateFactory.newLatLng(it))
                                }
                            },
                            onMapLongClick = {
                                // Street View
                                carShowStreetViewFlag = false
                                currentShowStreetViewFlag = false
                                // POI
                                poiClickFlag = true
                                // Search Bar
                                searchChangeFlag = true
                                onSearchBarFlag = false
                                // Map Type
                                onMapTypeSelectorFlag = false
                            },
                            onPOIClick = {
                                poiClickFlag = false
                                poiLocationName = it.name
                                poiLocation = it.latLng
                            }
                            // ##END
                            // ##END
                        ) {
                            // ##START 080-marker for current location
                            currentLocationState?.let {
                                var currentLocationAddress by mutableStateOf("")
                                val currLatLng =
                                    LatLng(it.position.latitude, it.position.longitude)
                                val address = searchViewModel.geoCoder.getFromLocation(
                                    currLatLng.latitude,
                                    currLatLng.longitude,
                                    1
                                )
                                // Marker for current location
                                currentLocationAddress = address?.get(0)?.getAddressLine(0).toString()
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
                                            Text(
                                                text = stringResource(id = R.string.current_location),
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(text = "Latitude: ${currentLocationState.position.latitude}")
                                            Text(text = "Longitude: ${currentLocationState.position.longitude}")
                                            Text(text = "Address: $currentLocationAddress")
                                        }
                                    },
                                    onClick = {
                                        currentLocationState.showInfoWindow()
                                        true
                                    },
                                    onInfoWindowClick = { currentShowStreetViewFlag = true },
                                )
                            }

                            // Marker for car location
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
                                    onInfoWindowClick = { carShowStreetViewFlag = true },
                                )
                            }

                            // Marker for POI
                            if (!poiClickFlag) {
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

                            // Marker for Search Result
                            if (!searchChangeFlag) {
                                val position = searchViewModel.searchLatLong?.let {
                                    LatLng(
                                        it.latitude,
                                        it.longitude
                                    )
                                }
                                val poiState =
                                    position?.let { rememberMarkerState(position = it) }
                                var poiName by remember { mutableStateOf("") }
                                poiName = searchViewModel.text
                                if (poiState != null) {
                                    MarkerInfoWindowContent(
                                        state = poiState,
                                        anchor = Offset(0.5f, 0.5f),
                                        title = poiName,
                                        onClick = {
                                            poiState.showInfoWindow()
                                            true
                                        },
                                    )
                                }
                            }

                            // Get searchLatLong from searchViewModel
                            searchLatLong = searchViewModel.searchLatLong
                            LaunchedEffect(searchLatLong) {
                                if (searchLatLongFlag) {
                                    searchViewModel.searchLatLong?.let { search ->
                                        val position = LatLng(search.latitude, search.longitude)
                                        cameraPositionState.animate(
                                            CameraUpdateFactory.newLatLngZoom(
                                                position,
                                                15f
                                            ), 1000
                                        )
                                        searchChangeFlag = false
                                        searchLatLongFlag = false
                                    }
                                }
                            }
                        } // End of GoogleMap()

                        // StreetView for current location and car location
                        if (carShowStreetViewFlag || currentShowStreetViewFlag) {
                            if (carShowStreetViewFlag)
                                streetViewResultReturn = streetViewValidator(carLatLng)
                            if (currentShowStreetViewFlag) {
                                val currentLoc =
                                    currentLocationState?.position?.let {
                                        LatLng(
                                            it.latitude,
                                            it.longitude
                                        )
                                    }
                                streetViewResultReturn = streetViewValidator(currentLoc)
                            }
                            LaunchedEffect(carLatLng) {
                                if (carLatLng != previousLatLng) {
                                    previousLatLng = carLatLng
                                    carShowStreetViewChangedFlag = true
                                }
                            }
                            if (carShowStreetViewChangedFlag) {
                                if (streetViewResultReturn == Status.OK) {
                                    if (carLatLng != previousLatLng) {
                                        // Render Street View for carLatLng
                                        streetViewResultReturn = streetViewValidator(carLatLng)
                                        carShowStreetViewChangedFlag = false
                                        carShowStreetViewFlag = false
                                    }
                                }
                            }
                            // Return/exit if street view not available
                            if (streetViewResultReturn == Status.ZERO_RESULTS) {
                                carShowStreetViewFlag = false
                                currentShowStreetViewFlag = false
                            }
                            BackHandler {
                                carShowStreetViewFlag = false
                                currentShowStreetViewFlag = false
                                previousLatLng = null
                            }
                        } // End of if (carShowStreetViewFlag || currentShowStreetViewFlag)
                    } // End of Column

                    // Start Bottom Search Bar
                    if (onSearchBarFlag) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(10.dp)
                                .fillMaxWidth(),
                            color = Color.White,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                AnimatedVisibility(
                                    searchViewModel.locationAutofill.isNotEmpty(),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp)
                                ) {
                                    // LazyColumn to display the list of autocomplete results
                                    LazyColumn(
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        // Limit the number of displayed search results to 5
                                        val limitedSearchResults =
                                            searchViewModel.locationAutofill.take(3)
                                        items(limitedSearchResults) { result ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(8.dp)
                                                    .clickable {
                                                        searchViewModel.text = result.address
                                                        searchViewModel.locationAutofill.clear()
                                                        searchViewModel.getCoordinates(result)
                                                        onSearchBarFlag = false
                                                        searchChangeFlag = true
                                                        searchLatLongFlag = true
//                                                        initialBoundsSet = true
                                                    }
                                            ) {
                                                Text(result.address)
                                            }
                                        }
                                    }
                                    Spacer(Modifier.height(16.dp))
                                }
                                OutlinedTextField(
                                    value = searchViewModel.text,
                                    placeholder = { Text(text = "Enter to Search") },
                                    onValueChange = {
                                        searchViewModel.text = it
                                        searchViewModel.searchPlaces(it)
                                    }, modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(4.dp)
                                )
                            }
                        } // End of search bar surface
                    } // End of if (onSearchBarFlag)
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
                } // ##END of if (!mapLoaded)
            } // End of content
        ) // End of Scaffold
    } // End of with(LocalDensity.current)
}

@Composable
fun streetViewValidator(targetLatLng: LatLng?) : Status{
    var streetViewResult by remember { mutableStateOf(Status.ZERO_RESULTS) }
    var streetViewLoaded by remember { mutableStateOf(true) }
    val context = LocalContext.current

    LaunchedEffect(targetLatLng) {
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
            Alignment.BottomCenter
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
