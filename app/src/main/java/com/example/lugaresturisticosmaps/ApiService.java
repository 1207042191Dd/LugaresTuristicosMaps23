package com.example.lugaresturisticosmaps;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface ApiService {

    // Obtener los lugares turísticos cercanos dentro de un radio
    @GET("json_getlistado")
    Call<ResponseBody> getLugares(
            @Query("latitud") double latitud,
            @Query("longitud") double longitud,
            @Query("radio") float radio
    );

    // Obtener categorías disponibles
    @GET("json_getcategorias")
    Call<ResponseBody> getCategorias();

    // Obtener subcategorías por categoría
    @GET("json_getsubcategorias")
    Call<ResponseBody> getSubcategorias(@Query("categoria") String categoria);
}