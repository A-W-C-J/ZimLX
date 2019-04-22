package net.oneplus.launcher;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;

import com.android.launcher3.LauncherModel;
import com.android.launcher3.R;
import com.android.launcher3.util.PackageManagerHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;

import androidx.annotation.DrawableRes;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;

public class OPWeatherProvider {

    private static final String TAG = "OPWeatherProvider";
    public static final int TEMPERATURE_NONE = -99;
    public static final String TEMP_UNIT_CELSIUS = "℃";
    // TIL these glyphs exist
    public static final String TEMP_UNIT_DEGREE = "˚";
    public static final String TEMP_UNIT_FAHRENHEIT = "℉";
    public static final int WEATHER_CODE_NONE = 9999;
    public static final String WEATHER_LAUNCH_ACTIVITY = "net.oneplus.weather.app.MainActivity";
    public static final String WEATHER_NAME_NONE = "N/A";
    public static final String WEATHER_PACKAGE_NAME = "net.oneplus.weather";
    private final String KEY_CITY_NAME = "weather_city";
    private final String KEY_TEMPERATURE = "weather_temp";
    private final String KEY_TEMPERATURE_HIGH = "weather_temp_high";
    private final String KEY_TEMPERATURE_LOW = "weather_temp_low";
    private final String KEY_TEMPERATURE_UNIT = "weather_temp_unit";
    private final String KEY_TIMESTAMP = "weather_timestamp";
    private final String KEY_WEATHER_CODE = "weather_code";
    private final String KEY_WEATHER_NAME = "weather_description";
    private final Uri WEATHER_CONTENT_URI = Uri
            .parse("content://com.oneplus.weather.ContentProvider/data");
    private ArrayList<IWeatherCallback> mCallbacks;
    private Context mContext;
    private WeatherObserver mObserver;

    private Handler mUiWorkerHandler = new Handler(LauncherModel.getUiWorkerLooper());

    public interface IWeatherCallback {

        void onWeatherUpdated(@NonNull WeatherData weatherData);
    }

    private enum WEATHER_COLUMNS {
        TIMESTAMP(0),
        CITY_NAME(1),
        WEATHER_CODE(2),
        WEATHER_NAME(6),
        TEMP(3),
        TEMP_HIGH(4),
        TEMP_LOW(5),
        TEMP_UNIT(7);

        private int index;

        WEATHER_COLUMNS(int i) {
            this.index = i;
        }
    }

    private enum WEATHER_TYPE {
        SUNNY(1001, R.drawable.weather_01, R.drawable.weather_01n),
        SUNNY_INTERVALS(1002, R.drawable.weather_01, R.drawable.weather_01n),
        CLOUDY(1003, R.drawable.weather_02, R.drawable.weather_02n),
        OVERCAST(1004, R.drawable.weather_03, R.drawable.weather_03n),
        DRIZZLE(1005, R.drawable.weather_10, R.drawable.weather_10n),
        RAIN(1006, R.drawable.weather_10, R.drawable.weather_10n),
        SHOWER(1007, R.drawable.weather_09, R.drawable.weather_09),
        DOWNPOUR(1008, R.drawable.weather_09, R.drawable.weather_09),
        RAINSTORM(1009, R.drawable.weather_09, R.drawable.weather_09),
        SLEET(1010, R.drawable.weather_13, R.drawable.weather_13),
        FLURRY(1011, R.drawable.weather_13, R.drawable.weather_13),
        SNOW(1012, R.drawable.weather_13, R.drawable.weather_13),
        SNOWSTORM(1013, R.drawable.weather_13, R.drawable.weather_13),
        // TODO: get proper hail icon
        HAIL(1014, R.drawable.weather_10, R.drawable.weather_10),
        THUNDERSHOWER(1015, R.drawable.weather_11, R.drawable.weather_11),
        // TODO: get proper icons for some more extreme conditions
        SANDSTORM(1016, R.drawable.weather_50, R.drawable.weather_50),
        FOG(1017, R.drawable.weather_50, R.drawable.weather_50),
        HURRICANE(1018, R.drawable.weather_11, R.drawable.weather_11),
        HAZE(1019, R.drawable.weather_50, R.drawable.weather_50),
        NONE(9999, R.drawable.weather_none_available, R.drawable.weather_none_available);

        int iconId;
        int nightId;
        int weatherCode;

        WEATHER_TYPE(@IntRange(from = 1000, to = 9999) int i, @DrawableRes int i2, @DrawableRes int night) {
            weatherCode = i;
            iconId = i2;
            nightId = night;
        }

        public static WEATHER_TYPE getWeather(int i) {
            for (WEATHER_TYPE weather_type : values()) {
                if (weather_type.weatherCode == i) {
                    Log.d(TAG, "get weather: " + weather_type);
                    return weather_type;
                }
            }
            Log.d(TAG, "get weather: " + NONE);
            return NONE;
        }

        public String toString() {
            return String.valueOf(this.weatherCode);
        }
    }

    public class WeatherData {

        public String cityName = "";
        public int temperature = TEMPERATURE_NONE;
        public int temperatureHigh = TEMPERATURE_NONE;
        public int temperatureLow = TEMPERATURE_NONE;
        public String temperatureUnit = TEMP_UNIT_CELSIUS;
        public long timestamp = 0;
        public int weatherCode = WEATHER_CODE_NONE;
        public String weatherName = WEATHER_NAME_NONE;

        public String toString() {
            return "[timestamp] " + timestamp + "; "
                    + "[cityName] " + cityName + "; "
                    + "[weatherCode] " + weatherCode + "; "
                    + "[weatherName] " + weatherName + "; "
                    + "[temperature] " + temperature + "; "
                    + "[temperatureHigh] " + temperatureHigh + "; "
                    + "[temperatureLow] " + temperatureLow + "; "
                    + "[temperatureUnit] " + temperatureUnit + "; ";
        }
    }

    private class WeatherObserver extends ContentObserver {

        public WeatherObserver() {
            super(new Handler());
        }

        public void onChange(boolean z) {
            super.onChange(z);
            mUiWorkerHandler.post(OPWeatherProvider.this::queryWeatherInformation);
        }
    }

    public OPWeatherProvider(Context context) {
        mContext = context;
        mObserver = new WeatherObserver();
        mCallbacks = new ArrayList<>();
    }

    public WeatherData getOfflineWeatherInformation() {
        SharedPreferences defaultSharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(mContext);
        if (defaultSharedPreferences.contains(KEY_TIMESTAMP)) {
            WeatherData weatherData = new WeatherData();
            weatherData.timestamp = defaultSharedPreferences.getLong(KEY_TIMESTAMP, 0);
            weatherData.cityName = defaultSharedPreferences.getString(KEY_CITY_NAME, "");
            weatherData.weatherCode = defaultSharedPreferences.getInt(KEY_WEATHER_CODE, 9999);
            weatherData.weatherName = defaultSharedPreferences
                    .getString(KEY_WEATHER_NAME, WEATHER_NAME_NONE);
            weatherData.temperature = defaultSharedPreferences.getInt(KEY_TEMPERATURE, -99);
            weatherData.temperatureHigh = defaultSharedPreferences
                    .getInt(KEY_TEMPERATURE_HIGH, -99);
            weatherData.temperatureLow = defaultSharedPreferences.getInt(KEY_TEMPERATURE_LOW, -99);
            weatherData.temperatureUnit = defaultSharedPreferences
                    .getString(KEY_TEMPERATURE_UNIT, TEMP_UNIT_CELSIUS);
            return weatherData;
        }
        mUiWorkerHandler.post(this::queryWeatherInformation);
        Log.d(TAG, "never get the weather information, querying... ");
        return null;
    }

    public void getCurrentWeatherInformation() {
        getCurrentWeatherInformation(null);
    }

    public void getCurrentWeatherInformation(IWeatherCallback iWeatherCallback) {
        WeatherData offlineWeatherInformation = getOfflineWeatherInformation();
        if (offlineWeatherInformation != null) {
            updateWeatherCallbacks(offlineWeatherInformation, iWeatherCallback);
        }
    }

    public void subscribeCallback(@NonNull IWeatherCallback iWeatherCallback) {
        Log.i(TAG,
                "subscribe new weather callback: " + iWeatherCallback.getClass().getSimpleName());
        if (!isAppEnabled()) {
            Log.w(TAG, "the weather application is not installed, may not receive the updates");
        }
        if (mCallbacks.contains(iWeatherCallback)) {
            Log.d(TAG, "the callback is existed, remove the old one");
            mCallbacks.remove(iWeatherCallback);
        }
        mCallbacks.add(iWeatherCallback);
        getCurrentWeatherInformation(iWeatherCallback);
    }

    public void unsubscribeCallback(@NonNull IWeatherCallback iWeatherCallback) {
        if (mCallbacks.contains(iWeatherCallback)) {
            Log.i(TAG, "un-subscribe the weather callback: " + iWeatherCallback.getClass()
                    .getSimpleName());
            mCallbacks.remove(iWeatherCallback);
            return;
        }
        Log.d(TAG, "the target callback is not found in the callback list");
    }

    @DrawableRes
    public static int getWeatherIconResourceId(int i) {
        return WEATHER_TYPE.getWeather(i).iconId;
    }

    @DrawableRes
    public static int getNightWeatherIconResourceId(int i) {
        return WEATHER_TYPE.getWeather(i).nightId;
    }

    @DrawableRes
    public static int getDefaultWeatherIconResourceId() {
        return WEATHER_TYPE.NONE.iconId;
    }

    public void registerContentObserver(ContentResolver contentResolver) {
        if (isAppEnabled()) {
            if (mObserver == null) {
                mObserver = new WeatherObserver();
            }
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("registerContentObserver receiver = ");
            stringBuilder.append(mObserver);
            Log.d(str, stringBuilder.toString());
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("registerContentObserver mContext = ");
            stringBuilder.append(mContext);
            Log.d(str, stringBuilder.toString());
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("registerContentObserver obj = ");
            stringBuilder.append(this);
            Log.d(str, stringBuilder.toString());
            try {
                contentResolver
                        .registerContentObserver(WEATHER_CONTENT_URI, true, mObserver);
                mUiWorkerHandler.post(this::queryWeatherInformation);
            } catch (SecurityException e) {
                Log.e(TAG, "register with Weather provider failed: ", e);
            }
            return;
        }
        Log.w(TAG, "the weather application is not installed");
    }

    public void unregisterContentObserver(ContentResolver contentResolver) {
        mCallbacks.clear();
        if (mObserver != null) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("unregisterContentObserver mContext = ");
            stringBuilder.append(mContext);
            Log.d(str, stringBuilder.toString());
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("unregisterContentObserver receiver = ");
            stringBuilder.append(mObserver);
            Log.d(str, stringBuilder.toString());
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("unregisterContentObserver obj = ");
            stringBuilder.append(this);
            Log.d(str, stringBuilder.toString());
            contentResolver.unregisterContentObserver(mObserver);
            mObserver = null;
        }
    }

    private boolean isAppEnabled() {
        return PackageManagerHelper
                .isAppEnabled(mContext.getPackageManager(), WEATHER_PACKAGE_NAME, 0);
    }

    private void queryWeatherInformation() {
        if (isAppEnabled()) {
            processWeatherInformation(mContext.getContentResolver()
                    .query(WEATHER_CONTENT_URI, null, null, null, null));
        } else {
            Log.w(TAG, "the weather application is not installed, stop querying...");
        }
    }

    private void processWeatherInformation(Cursor cursor) {
        StringBuilder stringBuilder;
        if (cursor == null) {
            Log.e(TAG, "cannot get weather information by querying content resolver");
        } else if (cursor.moveToFirst()) {
            if (cursor.getColumnCount() < WEATHER_COLUMNS.values().length) {
                Log.e(TAG, "the column count is not met the spec, contact OPWeather owner.");
                String stringBuilder2 = "expected columns: " + WEATHER_COLUMNS.values().length
                        + ", actual columns: " + cursor.getColumnCount();
                Log.e(TAG, stringBuilder2);
            }
            WeatherData weatherData = new WeatherData();
            String string;
            try {
                String string2 = cursor.getString(WEATHER_COLUMNS.TIMESTAMP.index);
                string = cursor.getString(WEATHER_COLUMNS.CITY_NAME.index);
                String string3 = cursor.getString(WEATHER_COLUMNS.WEATHER_CODE.index);
                String string4 = cursor.getString(WEATHER_COLUMNS.WEATHER_NAME.index);
                String string5 = cursor.getString(WEATHER_COLUMNS.TEMP.index);
                String string6 = cursor.getString(WEATHER_COLUMNS.TEMP_HIGH.index);
                String string7 = cursor.getString(WEATHER_COLUMNS.TEMP_LOW.index);
                String string8 = cursor.getString(WEATHER_COLUMNS.TEMP_UNIT.index);
                Log.d(TAG, "[Raw Weather Data] timestamp: " + string2
                        + ", city: " + string + ", code: " + string3 + ", name: " + string4
                        + ", temp: " + string5 + ", high: " + string6 + ", low: " + string7
                        + ", unit: " + string8);
                weatherData.timestamp =
                        new SimpleDateFormat("yyyyMMddkkmm", Locale.getDefault()).parse(string2)
                                .getTime() / 1000;
                weatherData.cityName = string;
                weatherData.weatherCode = WEATHER_TYPE
                        .getWeather(Integer.parseInt(string3)).weatherCode;
                weatherData.weatherName = string4;
                weatherData.temperature = Integer.parseInt(string5);
                weatherData.temperatureHigh = Integer.parseInt(string6);
                weatherData.temperatureLow = Integer.parseInt(string7);
                weatherData.temperatureUnit = string8;
            } catch (IllegalStateException e) {
                string = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("invalid Cursor data: ");
                stringBuilder.append(e);
                Log.e(string, stringBuilder.toString());
            } catch (NullPointerException e2) {
                string = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("got unexpected weather data: ");
                stringBuilder.append(e2);
                Log.e(string, stringBuilder.toString());
            } catch (Throwable th) {
                cursor.close();
                Log.d(TAG, weatherData.toString());
            }
            cursor.close();
            Log.d(TAG, weatherData.toString());
            updateWeatherCallbacks(weatherData, null);
            writePreferences(weatherData);
        } else {
            Log.e(TAG, "cannot move the cursor point to the first row, is the cursor empty?");
            cursor.close();
        }
    }

    private void updateWeatherCallbacks(WeatherData weatherData,
                                        IWeatherCallback iWeatherCallback) {
        if (iWeatherCallback == null) {
            Log.d(TAG, "push the weather information to the callbacks");
            for (IWeatherCallback callback : mCallbacks) {
                if (callback == null) {
                    Log.d(TAG, "updateWeatherCallbacks callback = null ");
                } else {
                    callback.onWeatherUpdated(weatherData);
                }
            }
            return;
        }
        Log.d(TAG, "push the weather information to: " + iWeatherCallback.getClass().getSimpleName());
        iWeatherCallback.onWeatherUpdated(weatherData);
    }

    private void writePreferences(WeatherData weatherData) {
        PreferenceManager.getDefaultSharedPreferences(mContext).edit()
                .putLong(KEY_TIMESTAMP, weatherData.timestamp)
                .putString(KEY_CITY_NAME, weatherData.cityName)
                .putInt(KEY_WEATHER_CODE, weatherData.weatherCode)
                .putString(KEY_WEATHER_NAME, weatherData.weatherName)
                .putInt(KEY_TEMPERATURE, weatherData.temperature)
                .putInt(KEY_TEMPERATURE_HIGH, weatherData.temperatureHigh)
                .putInt(KEY_TEMPERATURE_LOW, weatherData.temperatureLow)
                .putString(KEY_TEMPERATURE_UNIT, weatherData.temperatureUnit).apply();
    }
}