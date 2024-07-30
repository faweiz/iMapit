package tony.imapit.weather

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private val weatherApiService = WeatherAPIService.create()
var weatherReportFlag = false
var weatherReport: WeatherJson? = null
var weatherInfo: String = ""

@SuppressLint("CoroutineCreationDuringComposition")
@Composable
fun startWeatherReporting(lat: Double, lon: Double, apiKey: String) {
    val scope = rememberCoroutineScope()
    scope.launch() {
        try {
            if(!weatherReportFlag) {
                val response = weatherApiService.getWeatherReport(lat, lon, apiKey, "imperial")
                Log.d("TAG", "response: $response")
                if (response.isSuccessful) {
                    response.body()?.let {
                        weatherReport = response.body()
                        if (weatherReport != null) {
                            Log.d("TAG", "weatherReport: $weatherReport")
                            weatherInfo = getFormattedWeatherInfo(weatherReport!!)
                            weatherReportFlag = true
                        } else {
                            Log.d("TAG","Response body is null")
                            weatherReportFlag = false
                        }
                    }
                } else if (response.code() == 404) {
                    Log.e("TAG", "404 Error response code: ${response.code()}")
                    weatherReportFlag = false
                } else {
                    Log.e("TAG", "Error response code: ${response.code()}")
                    weatherReportFlag = false
                }
            } // End of if(!weatherReportFlag)
        } catch (e: Exception) {
            // Handle the exception
            Log.e("TAG", "Exception: ${e.message}")
        }
        // make a request once per second, delay a second
        delay(1000)
    }
}

fun getFormattedWeatherInfo(weatherReport: WeatherJson): String {
    val weatherDescription = weatherReport.weather.firstOrNull()?.description ?: "No description available"
    val windSpeed = weatherReport.wind.speed
    val windDirection = weatherReport.wind.deg
//    val temperatureFahrenheit = (weatherReport.main.temp * 9/5 - 459.67).toInt()  // Convert Kelvin to Fahrenheit
    val temperatureFahrenheit = weatherReport.main.temp
    val feelsLike = weatherReport.main.feels_like
    val tempMin = weatherReport.main.temp_min
    val tempMax = weatherReport.main.temp_max
    val humidity = weatherReport.main.humidity
    val pressure = weatherReport.main.pressure
    val currentDateTime = getDateTime(weatherReport.dt)
    val sunriseTime = getDateTime(weatherReport.sys.sunrise)
    val sunsetTime = getDateTime(weatherReport.sys.sunset)

    return """
        Time: $currentDateTime
        Temperature: ${temperatureFahrenheit}°F (${tempMin}°F - ${tempMax}°F)
        Weather: $weatherDescription
        Wind Speed: ${windSpeed}mph, ${windDirection}°
        Humidity: ${humidity}%
        Pressure: ${pressure} hPa       
        Sunrise: $sunriseTime
        Sunset: $sunsetTime
    """.trimIndent()
}

fun getDateTime(timestamp: Long): String {
    val date = Date(timestamp * 1000)  // Convert seconds to milliseconds
    val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    format.timeZone = TimeZone.getDefault()  // Use default timezone
    return format.format(date)
}