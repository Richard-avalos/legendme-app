package com.legendme.app.di;

import android.app.Activity;
import android.content.Intent;

import androidx.activity.result.ActivityResultLauncher;

import com.legendme.app.data.google.IdTokenProvider;
import com.legendme.app.data.remote.AuthApi;
import com.legendme.app.data.remote.AuthRepositoryImpl;
import com.legendme.app.data.storage.AuthStorage;
import com.legendme.app.domain.repository.AuthRepository;
import com.legendme.app.domain.usecase.LoginWithGoogleUseCase;
import com.legendme.app.presentation.login.LoginViewModel;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class AppContainer {
    public final LoginViewModel loginViewModel;
    public final IdTokenProvider idTokenProvider;

    public AppContainer(Activity activity, String baseUrl, String serverClientId, ActivityResultLauncher<Intent> legacyLauncher) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(new OkHttpClient.Builder().build())
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        AuthApi api = retrofit.create(AuthApi.class);
        AuthRepository repo = new AuthRepositoryImpl(api);
        AuthStorage storage = new AuthStorage(activity.getApplicationContext());
        LoginWithGoogleUseCase useCase = new LoginWithGoogleUseCase(repo);
        loginViewModel = new LoginViewModel(useCase, storage);

        idTokenProvider = new IdTokenProvider(activity, serverClientId, legacyLauncher);
    }
}
