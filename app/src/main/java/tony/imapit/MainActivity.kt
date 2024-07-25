package tony.imapit

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import tony.imapit.ui.theme.IMapitTheme
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.maps.android.compose.rememberCameraPositionState
import tony.imapit.car.CarViewModel
import tony.imapit.map.GoogleMapDisplay

class MainActivity : ComponentActivity() {
    private val viewModel: CarViewModel by viewModels()

    // ##START 080-location-client-and-callback
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            viewModel.updateLocation(locationResult.lastLocation)
        }
    }
    // ##END

    // ##START 080-permission-launcher
    private val getLocationPermission =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { isGranted ->
            if (isGranted.values.any { it }) {
                startLocationAndMap()
            } else {
                // if the user denied permissions, tell them they
                //   cannot use the app without them. In general,
                //   you should try to just reduce function and let the
                //   user continue, but location is a key part of this
                //   application.
                //   (Note that a real version of this application
                //   might allow the user to manually click on the map
                //   to set their current location, and we wouldn't
                //   show this dialog, or perhaps only show it once)
                // NOTE: This is a normal Android-View-based dialog, not a compose one!
                AlertDialog.Builder(this)
                    .setTitle("Permissions Needed")
                    .setMessage(
                        "We need coarse-location or fine-location permission " +
                                "to locate a car (fine location is highly " +
                                "recommended for accurate car locating). " +
                                "Please allow these permissions via App Info " +
                                "settings")
                    .setCancelable(false)
                    .setNegativeButton("Quit") { _, _ -> finish() }
                    .setPositiveButton("App Info") { _, _ ->
                        startActivity(
                            Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                            ).apply {
                                data = Uri.parse("package:$packageName")
                            }
                        )
                        finish()
                    }
                    .show()
            }
        }
    // ##END

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ##START 080-check-play-services
        GoogleApiAvailability.getInstance()
            .makeGooglePlayServicesAvailable(this)
            .addOnSuccessListener {
                // ##START 080-have-permission
                if (ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    getLocationPermission.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                } else {
                    startLocationAndMap()
                }
                // ##END
            }.addOnFailureListener(this) {
                Toast.makeText(
                    this,
                    "Google Play services required (or upgrade required)",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        // ##END
    }

    @SuppressLint("MissingPermission")
    fun startLocationAndMap() {
        // ##START 080-request-location-updates
        val locationRequest =
            LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
                .setWaitForAccurateLocation(false)
                .setMinUpdateIntervalMillis(0)
                .setMaxUpdateDelayMillis(5000)
                .build()

        fusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(this)
        fusedLocationProviderClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
        // ##END

        setContent {
            IMapitTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // ##START 050-camera-position-state
                    val cameraPositionState = rememberCameraPositionState()
                    // ##END

                    // ##START 080-collect-location-state
                    val currentLocation by viewModel.currentLocation.collectAsStateWithLifecycle(
                        initialValue = null
                    )
                    // ##END

                    val carLatLng by
                    viewModel.carLatLng.collectAsStateWithLifecycle(initialValue = null)

                    // ##START 040-factor-map
                    GoogleMapDisplay(
                        currentLocation = currentLocation,
                        carLatLng = carLatLng,
                        cameraPositionState = cameraPositionState,
                        onSetCarLocation = viewModel::setCarLocation,
                        onClearCarLocation = viewModel::clearCarLocation,
                        onMoveCar = viewModel::setCarLocation,
                        modifier = Modifier.fillMaxSize(),
                    )
                    // ##END
                }
            }
        }
    }
}