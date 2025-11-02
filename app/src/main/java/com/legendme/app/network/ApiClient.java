package com.legendme.app.network;

import android.util.Log;

import com.legendme.app.R;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {
    private static final String BASE_URL = "http://10.0.2.2";



    public static Retrofit usersRetrofit() {

        return new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(defaultClient())
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }

    public static Retrofit missionsRetrofit() {
        return new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(defaultClient())
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }

    private static OkHttpClient defaultClient() {
        HttpLoggingInterceptor log = new HttpLoggingInterceptor(msg -> Log.d("HTTP", msg));
        log.setLevel(HttpLoggingInterceptor.Level.BODY); //

        return new OkHttpClient.Builder()
                .addInterceptor(log)                      // ✅ usa el interceptor
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }
}
