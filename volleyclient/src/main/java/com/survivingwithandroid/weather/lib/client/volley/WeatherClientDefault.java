/*
 * Copyright (C) 2014 Francesco Azzola
 *  Surviving with Android (http://www.survivingwithandroid.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.survivingwithandroid.weather.lib.client.volley;

import android.content.Context;
import android.graphics.Bitmap;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.SystemClock;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.survivingwithandroid.weather.lib.WeatherClient;
import com.survivingwithandroid.weather.lib.WeatherConfig;
import com.survivingwithandroid.weather.lib.exception.ApiKeyRequiredException;
import com.survivingwithandroid.weather.lib.exception.LocationProviderNotFoundException;
import com.survivingwithandroid.weather.lib.exception.WeatherLibException;
import com.survivingwithandroid.weather.lib.model.City;
import com.survivingwithandroid.weather.lib.model.CurrentWeather;
import com.survivingwithandroid.weather.lib.model.HistoricalWeather;
import com.survivingwithandroid.weather.lib.model.WeatherForecast;
import com.survivingwithandroid.weather.lib.model.WeatherHourForecast;
import com.survivingwithandroid.weather.lib.provider.IWeatherProvider;
import com.survivingwithandroid.weather.lib.request.Params;
import com.survivingwithandroid.weather.lib.util.LogUtils;

import java.util.Date;
import java.util.List;

/**
 * This class represents the entry point to get the Weather information.
 * This class uses a IWeatherProvider to parse the response. Internally this class is based on Android Volley lib
 * so that you don't have to worry about Thread hanlding because the class makes the remote HTTP request
 * on a separate thread.
 * <p>
 * Usually you need only one instance of this class in your app so that the class implements the Singleton pattern
 * To get the reference to this class you have to use getInstance method:
 * </p>
 * {@code WeatherClient client = WeatherClient.getInstance();}
 * <p>
 * soon after you the the reference and before you make any requests you have to pass the {@link android.content.Context}
 * to this class.
 * </p>
 *
 * @author Francesco Azzola
 */

public class WeatherClientDefault extends WeatherClient {



    private CityEventListener cityListener;
    private static WeatherClientDefault me;
    private RequestQueue queue;

    /**
     * @deprecated Release 1.4 repleaced by {@link com.survivingwithandroid.weather.lib.WeatherClient.ClientBuilder}
     **/
    public static WeatherClientDefault getInstance() {
        if (me != null)
            return me;

        me = new WeatherClientDefault();
        return me;
    }

    @Override
    public void init(Context ctx) {
        super.init(ctx);
        queue = Volley.newRequestQueue(ctx);
    }

    /**
     * This method update the current provider configuration
     *
     * @param config WeatherConfig info
     * @see WeatherConfig
     */
    @Override
    public void updateWeatherConfig(WeatherConfig config) {
        provider.setConfig(config);
    }

    /**
     * Get the current weather condition. It returns a class structure that is indipendent from the
     * provider used to ge the weather data.
     * This method is an async method, in other word you have to implement your listener {@link com.survivingwithandroid.weather.lib.WeatherClient.WeatherEventListener} to
     * get notified when the weather data is ready.
     * <p>
     * When the data is ready this method calls the onWeatherRetrieved passing the current weather information.
     * If there are some errors during the request parsing, it calls onWeatherError passing the exception or
     * onConnectionError if the errors happened during the HTTP connection
     * </p>
     *
     * @param location a String representing the location id
     * @param listener {@link com.survivingwithandroid.weather.lib.WeatherClient.WeatherEventListener}
     * @throws com.survivingwithandroid.weather.lib.exception.ApiKeyRequiredException
     */
    @Override
    public void getCurrentCondition(String location, final WeatherEventListener listener) throws ApiKeyRequiredException {
        String url = provider.getQueryCurrentWeatherURL(location);
        LogUtils.LOGD("Current Condition URL [" + url + "]");
        StringRequest req = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String data) {
                        // We handle the response
                        try {
                            CurrentWeather weather = provider.getCurrentCondition(data);
                            listener.onWeatherRetrieved(weather);
                        } catch (WeatherLibException t) {
                            listener.onWeatherError(t);
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError volleyError) {
                        listener.onConnectionError(volleyError.getCause());
                    }
                }
        );

        queue.add(req);
    }

    /**
     * Search the city using a name pattern. It returns a class structure that is indipendent from the
     * provider used that holds the city list matching the pattern.
     * This method is an async method, in other word you have to implement your listener {@link com.survivingwithandroid.weather.lib.WeatherClient.CityEventListener} to
     * get notified when the weather data is ready.
     * <p>
     * When the data is ready this method calls the onCityListRetrieved passing a {@link java.util.List} of cities.
     * If there are some errors during the request parsing, it calls onWeatherError passing the exception or
     * onConnectionError if the errors happened during the HTTP connection
     * </p>
     *
     * @param pattern  a String representing the pattern
     * @param listener {@link com.survivingwithandroid.weather.lib.WeatherClient.CityEventListener}
     * @throws com.survivingwithandroid.weather.lib.exception.ApiKeyRequiredException
     */
    @Override
    public void searchCity(String pattern, final CityEventListener listener) throws ApiKeyRequiredException {
        String url = provider.getQueryCityURL(pattern);
        LogUtils.LOGD("Search city URL [" + url + "]");
        StringRequest req = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String data) {
                        // We handle the response
                        try {
                            List<City> cityResult = provider.getCityResultList(data);
                            listener.onCityListRetrieved(cityResult);
                        } catch (WeatherLibException t) {
                            listener.onWeatherError(t);
                        }


                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError volleyError) {
                        listener.onConnectionError(volleyError.getCause());
                    }
                }
        );

        queue.add(req);
    }

    /**
     * Get the forecast weather condition. It returns a class structure that is indipendent from the
     * provider used to ge the weather data.
     * This method is an async method, in other word you have to implement your listener {@link com.survivingwithandroid.weather.lib.WeatherClient.ForecastWeatherEventListener} to
     * get notified when the weather data is ready.
     * <p>
     * When the data is ready this method calls the onWeatherRetrieved passing the {@link com.survivingwithandroid.weather.lib.model.WeatherForecast} weather information.
     * If there are some errors during the request parsing, it calls onWeatherError passing the exception or
     * onConnectionError if the errors happened during the HTTP connection
     * </p>
     *
     * @param location a String representing the location id
     * @param listener {@link com.survivingwithandroid.weather.lib.WeatherClient.ForecastWeatherEventListener}
     * @throws com.survivingwithandroid.weather.lib.exception.ApiKeyRequiredException
     */

    @Override
    public void getForecastWeather(String location, final ForecastWeatherEventListener listener) throws ApiKeyRequiredException {
        String url = provider.getQueryForecastWeatherURL(location);
        LogUtils.LOGD("Forecast URL [" + url + "]");
        StringRequest req = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String data) {
                        // We handle the response
                        try {
                            WeatherForecast forecast = provider.getForecastWeather(data);
                            listener.onWeatherRetrieved(forecast);
                        } catch (WeatherLibException t) {
                            listener.onWeatherError(t);
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError volleyError) {
                        listener.onConnectionError(volleyError.getCause());
                    }
                }
        );

        queue.add(req);
    }

    /**
     * Get the forecast weather condition. It returns a class structure that is independent from the
     * provider used to ge the weather data.
     * This method is an async method, in other word you have to implement your listener {@link com.survivingwithandroid.weather.lib.WeatherClient.HourForecastWeatherEventListener} to
     * get notified when the weather data is ready.
     * <p>
     * When the data is ready this method calls the onWeatherRetrieved passing the {@link com.survivingwithandroid.weather.lib.model.WeatherHourForecast} weather information.
     * If there are some errors during the request parsing, it calls onWeatherError passing the exception or
     * onConnectionError if the errors happened during the HTTP connection
     * </p>
     *
     * @param location a String representing the location id
     * @param listener {@link com.survivingwithandroid.weather.lib.WeatherClient.HourForecastWeatherEventListener}
     * @throws com.survivingwithandroid.weather.lib.exception.ApiKeyRequiredException
     */
    @Override
    public void getHourForecastWeather(String location, final HourForecastWeatherEventListener listener) throws ApiKeyRequiredException {
        String url = provider.getQueryHourForecastWeatherURL(location);
        LogUtils.LOGD("Forecast Hourly URL [" + url + "]");
        StringRequest req = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String data) {
                        // We handle the response
                        try {
                            WeatherHourForecast forecast = provider.getHourForecastWeather(data);
                            listener.onWeatherRetrieved(forecast);
                        } catch (WeatherLibException t) {
                            listener.onWeatherError(t);
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError volleyError) {
                        listener.onConnectionError(volleyError.getCause());
                    }
                }
        );

        queue.add(req);
    }

    /**
     * Get the Historical weather condition. It returns a class structure that is independent from the
     * provider used to ge the weather data.
     * This method is an async method, in other word you have to implement your listener {@link com.survivingwithandroid.weather.lib.WeatherClient.HistoricalWeatherEventListener} to
     * get notified when the weather data is ready.
     * <p>
     * When the data is ready this method calls the onWeatherRetrieved passing the {@link com.survivingwithandroid.weather.lib.model.HistoricalWeather} weather information.
     * If there are some errors during the request parsing, it calls onWeatherError passing the exception or
     * onConnectionError if the errors happened during the HTTP connection
     * </p>
     *
     * @param location a String representing the location id
     * @param listener {@link com.survivingwithandroid.weather.lib.WeatherClient.HistoricalWeatherEventListener}
     * @throws com.survivingwithandroid.weather.lib.exception.ApiKeyRequiredException
     */
    @Override
    public void getHistoricalWeather(String location, Date startDate, Date endDate, final HistoricalWeatherEventListener listener) throws ApiKeyRequiredException {
        String url = provider.getQueryHistoricalWeatherURL(location, startDate, endDate);
        LogUtils.LOGD("Historical weather URL [" + url + "]");
        StringRequest req = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String data) {
                        // We handle the response
                        try {
                            HistoricalWeather hWeather = provider.getHistoricalWeather(data);
                            listener.onWeatherRetrieved(hWeather);
                        } catch (WeatherLibException t) {
                            listener.onWeatherError(t);
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError volleyError) {
                        listener.onConnectionError(volleyError.getCause());
                    }
                }
        );

        queue.add(req);
    }


    /**
    * This is the default image Provider that can be used to get the image provided by the Weather provider
    * @param icon String    The icon containing the weather code to retrieve the image
    * @param listener       {@link com.survivingwithandroid.weather.lib.WeatherClient.WeatherImageListener}
    * */
    @Override
    public void getDefaultProviderImage(String icon, final WeatherImageListener listener) {
        String imageURL = provider.getQueryImageURL(icon);
        ImageRequest ir = new ImageRequest(imageURL, new Response.Listener<Bitmap>() {

            @Override
            public void onResponse(Bitmap response) {
                if (listener != null)
                    listener.onImageReady(response);
            }
        }, 0, 0, null, null);

        queue.add(ir);
    }


    @Override
    public void searchCityByLocation(Criteria criteria, final CityEventListener listener) throws LocationProviderNotFoundException {
        super.handleLocation(criteria, listener);
    }

    /**
     * This method is used to set the Weather provider. The Weather provider must implement ({@link com.survivingwithandroid.weather.lib.provider.IWeatherProvider}
     * interface. This method has to be called before making any HTTP request otherwise the client doesn't know how
     * to parse the request
     *
     * @param provider {@link com.survivingwithandroid.weather.lib.provider.IWeatherProvider}
     * @see {@link com.survivingwithandroid.weather.lib.provider.openweathermap.OpenweathermapProvider}
     * @see {@link com.survivingwithandroid.weather.lib.provider.yahooweather.YahooWeatherProvider}
     */
    @Override
    public void setProvider(IWeatherProvider provider) {
        super.setProvider(provider);
    }


    /**
     * This method retrieves the image using the url generated by the weahter provider {@link com.survivingwithandroid.weather.lib.provider.IWeatherProvider}
     *
     * @param cityId String representing the city id
     * @param params   {@link com.survivingwithandroid.weather.lib.request.Params}: list of parameters used to create the image
     * @param listener {@link com.survivingwithandroid.weather.lib.WeatherClient.WeatherImageListener} listener that gets notified when the image is ready to use
     */
    @Override
    public void getWeatherImage(String cityId, Params params, final WeatherImageListener listener) {
        String imageURL = provider.getQueryRadar(cityId, params);
        ImageRequest ir = new ImageRequest(imageURL, new Response.Listener<Bitmap>() {

            @Override
            public void onResponse(Bitmap response) {
                if (listener != null)
                    listener.onImageReady(response);
            }
        }, 0, 0, null, null);

        queue.add(ir);
    }


    @Override
    protected void searchCityByLocation(Location location, final CityEventListener listener) throws ApiKeyRequiredException {
        String url = provider.getQueryCityURLByLocation(location);
        LogUtils.LOGD("Search city by Loc Url [" + url + "]");
        StringRequest req = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String data) {
                        // We handle the response
                        try {
                            List<City> cityResult = provider.getCityResultList(data);
                            listener.onCityListRetrieved(cityResult);
                        } catch (WeatherLibException t) {
                            listener.onWeatherError(t);
                        }


                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError volleyError) {
                        listener.onConnectionError(volleyError.getCause());
                    }
                }
        );

        queue.add(req);
    }




}
