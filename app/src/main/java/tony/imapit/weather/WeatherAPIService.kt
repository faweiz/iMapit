package tony.imapit.weather

import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

const val Weather_BASE_URL = "https://api.openweathermap.org/data/2.5/" // host computer for emulator

interface WeatherAPIService {
    @GET("weather")
    suspend fun getWeatherReport(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("appid") apiKey: String,
        @Query("units") units: String
    ): Response<WeatherJson>

    companion object {
        fun create(): WeatherAPIService =
            Retrofit.Builder()
                .addConverterFactory(GsonConverterFactory.create())
                .baseUrl(Weather_BASE_URL)
                .build()
                .create(WeatherAPIService::class.java)
    }
}