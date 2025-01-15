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

    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private static final int LOCATION_REQUEST_CODE = 100;

    private Spinner spinnerCategoria, spinnerSubcategoria;
    private String categoriaSeleccionada, subcategoriaSeleccionada;
    private float radio = 5000; // Radio inicial en metros
    private double userLat, userLng;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Configurar Google Maps
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // Inicializar cliente de ubicación
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

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
                .baseUrl("https://uealecpeterson.net/turismo/lugar_turistico/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        ApiService apiService = retrofit.create(ApiService.class);
        apiService.getLugares(userLat, userLng, radio).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    try {
                        JSONObject jsonObject = new JSONObject(response.body().string());
                        JSONArray data = jsonObject.getJSONArray("data");
                        mMap.clear(); // Limpiar marcadores anteriores
                        mMap.addCircle(new CircleOptions()
                                .center(new LatLng(userLat, userLng))
                                .radius(radio)
                                .strokeWidth(2)
                                .strokeColor(0xFF0000FF)
                                .fillColor(0x220000FF)); // Redibujar círculo

                        for (int i = 0; i < data.length(); i++) {
                            JSONObject lugar = data.getJSONObject(i);
                            double lat = lugar.getDouble("lat");
                            double lng = lugar.getDouble("lng");
                            String nombre = lugar.getString("nombre_lugar");
                            String descripcion = lugar.getString("descripcion");

                            // Marcadores personalizados
                            mMap.addMarker(new MarkerOptions()
                                    .position(new LatLng(lat, lng))
                                    .title(nombre)
                                    .snippet(descripcion)
                                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE))); // Marcador naranja
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                t.printStackTrace();
            }
        });
    }

    private void configurarSpinners() {
        List<String> categorias = new ArrayList<>();
        categorias.add("Agencia de Viajes");
        categorias.add("Restaurante");
        categorias.add("Hotel");

        ArrayAdapter<String> adapterCategoria = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, categorias);
        adapterCategoria.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategoria.setAdapter(adapterCategoria);

        spinnerCategoria.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                categoriaSeleccionada = categorias.get(position);
                cargarLugares();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        List<String> subcategorias = new ArrayList<>();
        subcategorias.add("Viajes");
        subcategorias.add("Paquetes turísticos");

        ArrayAdapter<String> adapterSubcategoria = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, subcategorias);
        adapterSubcategoria.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSubcategoria.setAdapter(adapterSubcategoria);

        spinnerSubcategoria.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                subcategoriaSeleccionada = subcategorias.get(position);
                cargarLugares();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_REQUEST_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            obtenerUbicacion();
        }
    }
}
