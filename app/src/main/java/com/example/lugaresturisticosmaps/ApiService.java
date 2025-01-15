package com.example.lugaresturisticosmaps;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface ApiService {
    @GET("json_getlistadoGridLT")
    Call<ResponseBody> getLugares(
            @Query("latitud") double latitud,
            @Query("longitud") double longitud,
            @Query("radio") float radio
    );
}
