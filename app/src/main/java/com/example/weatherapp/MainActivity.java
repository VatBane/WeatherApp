package com.example.weatherapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.common.internal.Objects;
import com.google.android.material.textfield.TextInputEditText;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private RelativeLayout homeRL;
    private ProgressBar loadingPB;
    private TextView cityNameTV, temperatureTV, conditionTV;
    private TextInputEditText cityEdt;
    private ImageView backIV, iconIV, searchIV;
    private RecyclerView weatherRV;
    private ArrayList<WeatherRVModal> weatherRVModalArrayList;
    private WeatherRVAdapter weatherRVAdapter;
    private LocationManager locationManager;
    private int PERMISSION_CODE = 1;
    private String cityName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        setContentView(R.layout.activity_main);
        homeRL = findViewById(R.id.idRLHome);
        loadingPB = findViewById(R.id.idPBLoading);
        cityNameTV = findViewById(R.id.idTVCityName);
        temperatureTV = findViewById(R.id.idTVTemperature);
        conditionTV = findViewById(R.id.idTVCondition);
        cityEdt = findViewById(R.id.idEdtCity);
        backIV = findViewById(R.id.idIVBack);
        iconIV = findViewById(R.id.idIVIcon);
        searchIV = findViewById(R.id.idIVSearch);
        weatherRV = findViewById(R.id.idRVWeather);
        weatherRVModalArrayList = new ArrayList<>();
        weatherRVAdapter = new WeatherRVAdapter(this, weatherRVModalArrayList);
        weatherRV.setAdapter(weatherRVAdapter);

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_CODE);
        }

        Location location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
//        cityName = getCityName(location.getLatitude(), location.getLongitude());
        getCityLatLon("Kyiv");

        searchIV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String city = cityEdt.getText().toString();
                if (city.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Please enter city name.", Toast.LENGTH_SHORT).show();
                } else {
                    cityNameTV.setText(cityName);
                    getCityLatLon(city);
                }
            }
        });
    }

    private void getCityLatLon(String cityName) {
        String url = "https://geocoding-api.open-meteo.com/v1/search?name=" + cityName + "&count=1";
        cityNameTV.setText(cityName);
        RequestQueue requestQueue = Volley.newRequestQueue(MainActivity.this);

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                loadingPB.setVisibility(View.GONE);
                homeRL.setVisibility(View.VISIBLE);
                weatherRVModalArrayList.clear();

                try {
                    Double lat = Double.valueOf(response.getJSONArray("results").getJSONObject(0)
                            .getString("latitude"));
                    Double lon = Double.valueOf(response.getJSONArray("results").getJSONObject(0)
                            .getString("longitude"));
                    getWeatherInfo(lat, lon);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(MainActivity.this, "Please enter valid city name..", Toast.LENGTH_SHORT).show();
            }
        });

        requestQueue.add(jsonObjectRequest);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permissions granted..", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Please provide permissions", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private String getCityName(double latitude, double longitude) {
        String cityName = "Not Found";
        Geocoder gcd = new Geocoder(getBaseContext(), Locale.getDefault());
        try {
            List<Address> addresses = gcd.getFromLocation(latitude, longitude, 10);

            for (Address adr : addresses) {
                if (adr != null) {
                    String city = adr.getLocality();
                    if (city != null && !city.equals("")) {
                        cityName = city;
                    } else {
                        Log.d("TAG", "CITY NOT FOUND");
                        Toast.makeText(this, "User City Not Found..", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return cityName;
    }

    private void getWeatherInfo(double lat, double lon) {
        String url = "https://api.open-meteo.com/v1/forecast?latitude=" + lat + "&longitude=" + lon +
                "&hourly=temperature_2m,rain,cloudcover,windspeed_10m";

        RequestQueue requestQueue = Volley.newRequestQueue(MainActivity.this);
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                loadingPB.setVisibility(View.GONE);
                homeRL.setVisibility(View.VISIBLE);
                weatherRVModalArrayList.clear();

                try {
                    JSONObject units = response.getJSONObject("hourly_units");
                    JSONObject forecast = response.getJSONObject("hourly");

                    JSONArray times = forecast.getJSONArray("time");
                    JSONArray temperatures = forecast.getJSONArray("temperature_2m");
                    JSONArray rains = forecast.getJSONArray("rain");
                    JSONArray clouds = forecast.getJSONArray("cloudcover");
                    JSONArray winds = forecast.getJSONArray("windspeed_10m");
                    String img = "https://cdn-icons-png.flaticon.com/512/869/869869.png";
                    int current_hour = Calendar.getInstance().getTime().getHours();

                    while (true) {
                        int hour = Integer.parseInt(times.getString(0).split("T")[1].split(":")[0]);

                        if (Math.abs(hour - current_hour) >= 1) {
                            times.remove(0);
                        } else
                            break;
                    }

                    for (int i = 0; i < times.length(); i++) {
                        String time = times.getString(i);
                        String temper = String.valueOf(temperatures.getDouble(i)) + units.getString("temperature_2m");
                        double rainCount = rains.getDouble(i);
                        int cloudCover = clouds.getInt(i);
                        String wind = String.valueOf(winds.getDouble(i)) + units.getString("windspeed_10m");
                        img = selectIcon(time, cloudCover, rainCount);

                        if (i == 0) {
                            temperatureTV.setText(temper);
//                            conditionTV.setText(img);
                            Picasso.get().load(img).into(iconIV);
                        } else {
                            weatherRVModalArrayList.add(new WeatherRVModal(time, temper, img, wind));
                        }
                    }

                    int current = Calendar.getInstance().getTime().getHours();
                    if (current > 7 && current < 19) {
                        Picasso.get().load("https://i.pinimg.com/564x/b6/8e/4d/b68e4da9c6c71aa32f33ec358bd8bb1c.jpg").into(backIV);
                    } else {
                        Picasso.get().load("https://i.pinimg.com/564x/5f/c0/68/5fc0689db0d7241476b5bf72568039f7.jpg").into(backIV);
                    }

                    weatherRVAdapter.notifyDataSetChanged();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(MainActivity.this, "Please enter valid city name..", Toast.LENGTH_SHORT).show();
            }
        });

        requestQueue.add(jsonObjectRequest);
    }

    private String selectIcon(String time, int cloudCover, double rainCount) {
        int hours = Integer.parseInt(time.split("T")[1].split(":")[0]);

        if (cloudCover < 33) {
            if (hours > 7 && hours < 19) {
                return "https://cdn-icons-png.flaticon.com/512/869/869869.png";
            } else {
                return "https://cdn-icons-png.flaticon.com/512/740/740878.png";
            }
        } else if (cloudCover >= 33 || cloudCover < 67) {
            if (rainCount > 0) {
                if (hours > 7 && hours < 19) {
                    return "https://cdn-icons-png.flaticon.com/512/1163/1163657.png";
                } else {
                    return "https://cdn-icons-png.flaticon.com/512/4005/4005742.png";
                }
            } else {
                if (hours > 7 && hours < 19) {
                    return "https://cdn-icons-png.flaticon.com/512/1163/1163661.png";
                } else {
                    return "https://cdn-icons-png.flaticon.com/512/2387/2387889.png";
                }
            }
        } else {
            if (rainCount > 0) {
                return "https://cdn-icons-png.flaticon.com/512/4735/4735072.png";
            } else {
                return "https://cdn-icons-png.flaticon.com/512/1163/1163624.png";
            }
        }
    }
}