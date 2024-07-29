package tony.imapit

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

import com.google.android.libraries.places.api.Places

@HiltAndroidApp
class MyApplication : Application() {
    companion object {

    }
    override fun onCreate() {
        super.onCreate()
        // Initialize any application-wide resources here
        // Initialize Google Places SDK
        Places.initialize(applicationContext, BuildConfig.MAPS_API_KEY)
    }
}
