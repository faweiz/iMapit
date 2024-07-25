package tony.imapit.car

import android.app.Application
import android.content.Context
import android.location.Location
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private val LAT_PREF = stringPreferencesKey("lat")
private val LON_PREF = stringPreferencesKey("lon")

class CarViewModel(application: Application) : AndroidViewModel(application) {
    // ##START 080-track-current-location
    private val _currentLocation = MutableStateFlow<Location?>(null)
    val currentLocation: Flow<Location?>
        get() = _currentLocation
    // ##END

    // ##START 080-allow-activity-to-update
    fun updateLocation(location: Location?) {
        _currentLocation.value = location
    }
    // ##END

    // ##START 120-add-car-location
    private val Context.preferencesDataStore:
            DataStore<Preferences> by preferencesDataStore(name = "carfinder")

    val carLatLng = application.preferencesDataStore.data.map { preferences ->
        preferences[LAT_PREF]?.let { latString ->
            preferences[LON_PREF]?.let { lonString ->
                LatLng(latString.toDouble(), lonString.toDouble())
            }
        }
    }

    fun clearCarLocation() {
        viewModelScope.launch {
            getApplication<Application>().preferencesDataStore.edit { preferences ->
                preferences.remove(LAT_PREF)
                preferences.remove(LON_PREF)
            }
        }
    }

    fun setCarLocation() {
        viewModelScope.launch {
            _currentLocation.value?.let { location ->
                getApplication<Application>().preferencesDataStore.edit { preferences ->
                    preferences[LAT_PREF] = location.latitude.toString()
                    preferences[LON_PREF] = location.longitude.toString()
                }
            } ?: run {
                clearCarLocation()
            }
        }
    }
    // ##END

    fun setCarLocation(latLng: LatLng) {
        viewModelScope.launch {
            getApplication<Application>().preferencesDataStore.edit { preferences ->
                preferences[LAT_PREF] = latLng.latitude.toString()
                preferences[LON_PREF] = latLng.longitude.toString()
            }
        }
    }
}
