package com.example.lugaresturisticosmaps;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.slider.Slider;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private static final int LOCATION_REQUEST_CODE = 100;
    private Spinner spinnerCategoria, spinnerSubcategoria;
    private TextView txtLatLong, txtLugaresCercanos;
    private double userLat, userLng;
    private float radio = 5000;
    private Circle searchCircle;
    private Slider sliderRadio;
    private Marker locationMarker;

    private List<String> categoriasList = new ArrayList<>();
    private HashMap<String, List<String>> subcategoriasMap = new HashMap<>();
    private List<Marker> currentMarkers = new ArrayList<>();

    private static final float MIN_RADIO = 1000f;
    private static final float MAX_RADIO = 10000f;
    private static final float DEFAULT_RADIO = 5000f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        txtLatLong = findViewById(R.id.txtLatLong);
        txtLugaresCercanos = findViewById(R.id.txtLugaresCercanos);
        spinnerCategoria = findViewById(R.id.spinnerCategoria);
        spinnerSubcategoria = findViewById(R.id.spinnerSubcategoria);
        sliderRadio = findViewById(R.id.sliderRadio);

        sliderRadio.setValueFrom(0f);
        sliderRadio.setValueTo(1f);
        sliderRadio.setValue(0.5f);

        sliderRadio.addOnChangeListener((slider, value, fromUser) -> {
            radio = MIN_RADIO + (MAX_RADIO - MIN_RADIO) * value;
            if (mMap != null && locationMarker != null) {
                actualizarMapaConRadio(locationMarker.getPosition());
                String categoriaSeleccionada = spinnerCategoria.getSelectedItem() != null ?
                        spinnerCategoria.getSelectedItem().toString() : null;
                String subcategoriaSeleccionada = spinnerSubcategoria.getSelectedItem() != null ?
                        spinnerSubcategoria.getSelectedItem().toString() : null;
                filtrarMarcadores(categoriaSeleccionada, subcategoriaSeleccionada);
            }
        });

        Button btnNormal = findViewById(R.id.btnNormal);
        Button btnSatelite = findViewById(R.id.btnSatelite);
        Button btnHibrido = findViewById(R.id.btnHibrido);
        Button btnMostrarTodos = findViewById(R.id.btnMostrarTodos);

        btnNormal.setOnClickListener(v -> mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL));
        btnSatelite.setOnClickListener(v -> mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE));
        btnHibrido.setOnClickListener(v -> mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID));
        btnMostrarTodos.setOnClickListener(v -> mostrarTodosLosLugares());

        cargarCategoriasYSubcategorias();

        spinnerCategoria.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String categoriaSeleccionada = categoriasList.get(position);
                actualizarSubcategorias(categoriaSeleccionada);
                filtrarMarcadores(categoriaSeleccionada, null);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        spinnerSubcategoria.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String categoriaSeleccionada = spinnerCategoria.getSelectedItem().toString();
                String subcategoriaSeleccionada = spinnerSubcategoria.getSelectedItem().toString();
                filtrarMarcadores(categoriaSeleccionada, subcategoriaSeleccionada);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
        } else {
            obtenerUbicacion();
        }

        mMap.setOnMarkerClickListener(marker -> {
            LatLng position = marker.getPosition();
            txtLatLong.setText("Latitud: " + position.latitude + ", Longitud: " + position.longitude);
            return false;
        });
    }

    private void obtenerUbicacion() {
        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                userLat = location.getLatitude();
                userLng = location.getLongitude();

                txtLatLong.setText("Latitud: " + userLat + ", Longitud: " + userLng);

                LatLng userLocation = new LatLng(userLat, userLng);

                if (locationMarker != null) {
                    locationMarker.remove();
                }

                locationMarker = mMap.addMarker(new MarkerOptions()
                        .position(userLocation)
                        .title("Mi ubicación")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
                locationMarker.showInfoWindow();

                actualizarMapaConRadio(userLocation);
            }
        });
    }

    private void actualizarMapaConRadio(LatLng userLocation) {
        if (searchCircle != null) {
            searchCircle.remove();
        }

        searchCircle = mMap.addCircle(new CircleOptions()
                .center(userLocation)
                .radius(radio)
                .strokeWidth(2)
                .strokeColor(0xFF0000FF)
                .fillColor(0x220000FF));

        float zoom = calcularZoom(radio);
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLocation, zoom));
    }

    private float calcularZoom(float radio) {
        return (float) (16 - Math.log(radio / 500) / Math.log(2));
    }

    private void cargarCategoriasYSubcategorias() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://turismoquevedo.com/lugar_turistico/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        ApiService apiService = retrofit.create(ApiService.class);
        apiService.getLugares(userLat, userLng, radio).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    try {
                        String jsonResponse = response.body().string();
                        JSONObject jsonObject = new JSONObject(jsonResponse);
                        JSONArray lugares = jsonObject.getJSONArray("data");

                        for (int i = 0; i < lugares.length(); i++) {
                            JSONObject lugar = lugares.getJSONObject(i);
                            String categoria = lugar.getString("categoria");
                            String subcategoria = lugar.getString("subcategoria");

                            if (!categoriasList.contains(categoria)) {
                                categoriasList.add(categoria);
                                subcategoriasMap.put(categoria, new ArrayList<>());
                            }

                            if (!subcategoriasMap.get(categoria).contains(subcategoria)) {
                                subcategoriasMap.get(categoria).add(subcategoria);
                            }
                        }

                        ArrayAdapter<String> categoriaAdapter = new ArrayAdapter<>(MainActivity.this,
                                android.R.layout.simple_spinner_item, categoriasList);
                        categoriaAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        spinnerCategoria.setAdapter(categoriaAdapter);
                    } catch (Exception e) {
                        Log.e("Error", "Error al cargar categorías: ", e);
                    }
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e("Error", "Error en la solicitud: ", t);
            }
        });
    }

    private void actualizarSubcategorias(String categoria) {
        List<String> subcategorias = subcategoriasMap.get(categoria);
        ArrayAdapter<String> subcategoriaAdapter = new ArrayAdapter<>(MainActivity.this,
                android.R.layout.simple_spinner_item, subcategorias);
        subcategoriaAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSubcategoria.setAdapter(subcategoriaAdapter);
    }

    private void filtrarMarcadores(String categoria, String subcategoria) {
        for (Marker marker : currentMarkers) {
            marker.remove();
        }
        currentMarkers.clear();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://turismoquevedo.com/lugar_turistico/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        ApiService apiService = retrofit.create(ApiService.class);
        apiService.getLugares(userLat, userLng, radio).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    try {
                        String jsonResponse = response.body().string();
                        JSONObject jsonObject = new JSONObject(jsonResponse);
                        JSONArray lugares = jsonObject.getJSONArray("data");

                        int contador = 0;

                        for (int i = 0; i < lugares.length(); i++) {
                            JSONObject lugar = lugares.getJSONObject(i);
                            String lugarCategoria = lugar.getString("categoria");
                            String lugarSubcategoria = lugar.getString("subcategoria");
                            String nombre = lugar.getString("nombre_lugar");
                            double lat = lugar.getDouble("latitud");
                            double lng = lugar.getDouble("longitud");

                            if (lugarCategoria.equals(categoria) && (subcategoria == null || lugarSubcategoria.equals(subcategoria))) {
                                LatLng position = new LatLng(lat, lng);
                                Marker marker = mMap.addMarker(new MarkerOptions()
                                        .position(position)
                                        .title(nombre)
                                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
                                currentMarkers.add(marker);
                                contador++;
                            }
                        }

                        txtLugaresCercanos.setText("Lugares cercanos: " + contador);

                    } catch (Exception e) {
                        Log.e("Error", "Error al filtrar marcadores: ", e);
                    }
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e("Error", "Error en la solicitud: ", t);
            }
        });
    }

    private void mostrarTodosLosLugares() {
        for (Marker marker : currentMarkers) {
            marker.remove();
        }
        currentMarkers.clear();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://turismoquevedo.com/lugar_turistico/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        ApiService apiService = retrofit.create(ApiService.class);
        apiService.getLugares(userLat, userLng, radio).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    try {
                        String jsonResponse = response.body().string();
                        JSONObject jsonObject = new JSONObject(jsonResponse);
                        JSONArray lugares = jsonObject.getJSONArray("data");

                        for (int i = 0; i < lugares.length(); i++) {
                            JSONObject lugar = lugares.getJSONObject(i);
                            String nombre = lugar.getString("nombre_lugar");
                            double lat = lugar.getDouble("latitud");
                            double lng = lugar.getDouble("longitud");

                            LatLng position = new LatLng(lat, lng);
                            Marker marker = mMap.addMarker(new MarkerOptions()
                                    .position(position)
                                    .title(nombre)
                                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
                            currentMarkers.add(marker);
                        }

                        txtLugaresCercanos.setText("Mostrando todos los lugares turísticos");

                    } catch (Exception e) {
                        Log.e("Error", "Error al mostrar todos los lugares: ", e);
                    }
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e("Error", "Error en la solicitud: ", t);
            }
        });
    }
}