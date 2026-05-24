package com.example.smartvision;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Path;

public interface APISmartVision {

    @GET("obtenerUsuario/{usuario}")
    Call<Usuario> getUsuario(@Path("usuario") String usuario);

    @FormUrlEncoded
    @POST("registro")
    Call<Usuario> registrar(
            @Field("usuario") String user,
            @Field("nombre") String nom,
            @Field("apellido") String ape,
            @Field("email") String email,
            @Field("contrasena") String contra
    );

    @Multipart
    @POST("obtenerTexto")
    Call<Usuario> procesarVision(
            @Part("usuario") RequestBody userId,
            @Part MultipartBody.Part file
    );
}