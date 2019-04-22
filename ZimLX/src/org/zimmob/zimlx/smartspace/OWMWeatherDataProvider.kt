/*
 *     This file is part of Lawnchair Launcher.
 *
 *     Lawnchair Launcher is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Lawnchair Launcher is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Lawnchair Launcher.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.zimmob.zimlx.smartspace

import android.Manifest
import android.content.Context
import android.location.Criteria
import android.location.LocationManager
import android.util.Log
import android.widget.Toast
import androidx.annotation.Keep
import com.android.launcher3.R
import com.android.launcher3.Utilities
import com.kwabenaberko.openweathermaplib.constants.Units
import com.kwabenaberko.openweathermaplib.implementation.OpenWeatherMapHelper
import com.kwabenaberko.openweathermaplib.implementation.callbacks.CurrentWeatherCallback
import com.kwabenaberko.openweathermaplib.models.currentweather.CurrentWeather
import org.zimmob.zimlx.ZimPreferences
import org.zimmob.zimlx.util.Temperature
import org.zimmob.zimlx.zimApp
import kotlin.math.roundToInt

@Keep
@Suppress("DEPRECATION")
class OWMWeatherDataProvider(controller: ZimSmartspaceController) :
        ZimSmartspaceController.PeriodicDataProvider(controller), ZimPreferences.OnPreferenceChangeListener, CurrentWeatherCallback {

    private val context = controller.context
    private val prefs = Utilities.getZimPrefs(context)
    private val owm by lazy { OpenWeatherMapHelper(prefs.weatherApiKey) }
    private val iconProvider by lazy { WeatherIconProvider(context) }

    private val locationAccess
        get() = Utilities.hasPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ||
                Utilities.hasPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
    private val locationManager: LocationManager? by lazy {
        if (locationAccess) {
            context.getSystemService(Context.LOCATION_SERVICE) as LocationManager?
        } else null
    }

    init {
        prefs.addOnPreferenceChangeListener(this, "pref_weatherApiKey", "pref_weather_city", "pref_weather_units")
    }

    override fun updateData() {
        // TODO: Create a search/dropdown for cities, make Auto the default
        if (prefs.weatherCity == "##Auto") {
            if (!locationAccess) {
                Utilities.requestLocationPermission(context.zimApp.activityHandler.foregroundActivity)
                return
            }
            val locationProvider = locationManager?.getBestProvider(Criteria(), true)
            val location = locationManager?.getLastKnownLocation(locationProvider)
            if (location != null) {
                owm.getCurrentWeatherByGeoCoordinates(location.latitude, location.longitude, this)
            }
        } else {
            owm.getCurrentWeatherByCityName(prefs.weatherCity, this)
        }
    }

    override fun onSuccess(currentWeather: CurrentWeather) {
        val temp = currentWeather.main?.temp ?: return
        val icon = currentWeather.weather.getOrNull(0)?.icon ?: return
        updateData(ZimSmartspaceController.WeatherData(
                iconProvider.getIcon(icon),
                Temperature(
                        temp.roundToInt(),
                        if (prefs.weatherUnit != Temperature.Unit.Fahrenheit) Temperature.Unit.Celsius else Temperature.Unit.Fahrenheit
                ),
                "https://openweathermap.org/city/${currentWeather.id}"
        ), null)
    }

    override fun onFailure(throwable: Throwable?) {
        Log.w("OWM", "Updating weather data failed", throwable)
        if (prefs.weatherApiKey == context.getString(R.string.default_owm_key)) {
            Toast.makeText(context, R.string.owm_get_your_own_key, Toast.LENGTH_LONG).show()
        } else if (throwable != null) {
            Toast.makeText(context, throwable.message, Toast.LENGTH_LONG).show()
        }
        updateData(null, null)
    }

    override fun onDestroy() {
        super.onDestroy()
        prefs.removeOnPreferenceChangeListener(this, "pref_weatherApiKey", "pref_weather_city", "pref_weather_units")
    }

    override fun onValueChanged(key: String, prefs: ZimPreferences, force: Boolean) {
        if (key in arrayOf("pref_weatherApiKey", "pref_weather_city", "pref_weather_units")) {
            if (key == "pref_weather_units") {
                owm.setUnits(when (prefs.weatherUnit) {
                    Temperature.Unit.Celsius -> Units.METRIC
                    Temperature.Unit.Fahrenheit -> Units.IMPERIAL
                    else -> Units.METRIC
                })
            } else if (key == "pref_weatherApiKey" && !force) {
                owm.setApiKey(prefs.weatherApiKey)
            }
            if (!force) updateNow()
        }
    }
}
