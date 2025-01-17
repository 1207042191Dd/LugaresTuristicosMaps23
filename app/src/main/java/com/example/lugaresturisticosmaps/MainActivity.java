package com.example.lugaresturisticosmaps;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.slider.Slider;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {
//variables a usar en el codigo para moldear nuestra aplicacion
    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private static final int LOCATION_REQUEST_CODE = 100;
 //llamo a los elemntos del diseno
    private Spinner spinnerCategoria, spinnerSubcategoria;
    private TextView txtLatLong;
    private String categoriaSeleccionada, subcategoriaSeleccionada;
    private float radio = 5000; // Radio inicial en metros
    private double userLat, userLng;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);//llamar el diseno

        // Configurar Google Maps
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // Inicializar cliente de ubicación
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Inicializar el TextView para latitud y longitud
        txtLatLong = findViewById(R.id.txtLatLong);

        // Configurar el slider
        Slider slider = findViewById(R.id.sliderRadio);
        slider.addOnChangeListener((slider1, value, fromUser) -> {
            radio = value * 1000; // Convertir a metros
            cargarLugares();
        });

        // Configurar los spinners
        spinnerCategoria = findViewById(R.id.spinnerCategoria);
        spinnerSubcategoria = findViewById(R.id.spinnerSubcategoria);

        configurarSpinners();
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        // Solicitar permisos de ubicación
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
        } else {
            obtenerUbicacion();
        }
    }

    private void obtenerUbicacion() {
        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                userLat = location.getLatitude();
                userLng = location.getLongitude();

                // Mostrar latitud y longitud
                txtLatLong.setText("Latitud: " + userLat + ", Longitud: " + userLng);

                LatLng userLocation = new LatLng(userLat, userLng);

                // Dibujar un círculo que representa el radio
                mMap.addCircle(new CircleOptions()
                        .center(userLocation)
                        .radius(radio)
                        .strokeWidth(2)
                        .strokeColor(0xFF0000FF)
                        .fillColor(0x220000FF)); // Color azul transparente

                // Centrar la cámara en la ubicación del usuario
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 13));

                // Cargar lugares cercanos
                cargarLugares();
            }
        });
    }

    private void cargarLugares() {
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

                        // Limpiar el mapa antes de agregar nuevos lugares
                        mMap.clear();

                        for (int i = 0; i < lugares.length(); i++) {
                            JSONObject lugar = lugares.getJSONObject(i);
                            String nombre = lugar.getString("nombre_lugar");
                            double lat = lugar.getDouble("latitud");
                            double lng = lugar.getDouble("longitud");
                            String direccion = lugar.getString("direccion");

                            // Crear el marcador
                            LatLng latLng = new LatLng(lat, lng);
                            MarkerOptions markerOptions = new MarkerOptions()
                                    .position(latLng)
                                    .title(nombre)
                                    .snippet(direccion)
                                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));

                            mMap.addMarker(markerOptions);
                        }
                    } catch (Exception e) {
                        Log.e("Error", "Error al cargar los lugares: ", e);
                    }
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e("Error", "Error en la solicitud: ", t);
            }
        });
    }

    private void configurarSpinners() {
        // Cargar las categorías y subcategorías desde la API
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://turismoquevedo.com/lugar_turistico/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        ApiService apiService = retrofit.create(ApiService.class);
        apiService.getCategorias().enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    try {
                        String jsonResponse = response.body().string();
                        JSONObject jsonObject = new JSONObject(jsonResponse);
                        JSONArray categorias = jsonObject.getJSONArray("data");

                        // Cargar las categorías en el spinner
                        List<String> categoriaList = new ArrayList<>();
                        for (int i = 0; i < categorias.length(); i++) {
                            JSONObject categoria = categorias.getJSONObject(i);
                            categoriaList.add(categoria.getString("categoria"));
                        }

                        ArrayAdapter<String> categoriaAdapter = new ArrayAdapter<>(MainActivity.this,
                                android.R.layout.simple_spinner_item, categoriaList);
                        categoriaAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        spinnerCategoria.setAdapter(categoriaAdapter);

                        // Configurar el listener para cambios en la categoría
                        spinnerCategoria.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                            @Override
                            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                                categoriaSeleccionada = categoriaList.get(position);
                                cargarSubcategorias(categoriaSeleccionada);
                            }

                            @Override
                            public void onNothingSelected(AdapterView<?> parentView) {
                            }
                        });
                    } catch (Exception e) {
                        Log.e("Error", "Error al cargar las categorías: ", e);
                    }
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e("Error", "Error en la solicitud de categorías: ", t);
            }
        });
    }

    private void cargarSubcategorias(String categoriaSeleccionada) {
        // Similar a lo anterior, cargar las subcategorías en función de la categoría seleccionada
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://turismoquevedo.com/lugar_turistico/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        ApiService apiService = retrofit.create(ApiService.class);
        apiService.getSubcategorias(categoriaSeleccionada).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    try {
                        String jsonResponse = response.body().string();
                        JSONObject jsonObject = new JSONObject(jsonResponse);
                        JSONArray subcategorias = jsonObject.getJSONArray("data");

                        // Cargar las subcategorías en el spinner
                        List<String> subcategoriaList = new ArrayList<>();
                        for (int i = 0; i < subcategorias.length(); i++) {
                            JSONObject subcategoria = subcategorias.getJSONObject(i);
                            subcategoriaList.add(subcategoria.getString("subcategoria"));
                        }

                        ArrayAdapter<String> subcategoriaAdapter = new ArrayAdapter<>(MainActivity.this,
                                android.R.layout.simple_spinner_item, subcategoriaList);
                        subcategoriaAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        spinnerSubcategoria.setAdapter(subcategoriaAdapter);
                    } catch (Exception e) {
                        Log.e("Error", "Error al cargar las subcategorías: ", e);
                    }
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e("Error", "Error en la solicitud de subcategorías: ", t);
            }
        });
    }
}
