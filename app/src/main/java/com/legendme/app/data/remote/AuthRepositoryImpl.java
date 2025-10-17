package com.legendme.app.data.remote;

import com.legendme.app.domain.model.AuthResult;
import com.legendme.app.domain.repository.AuthRepository;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class AuthRepositoryImpl implements AuthRepository {

    private final AuthApi api;

    public AuthRepositoryImpl(AuthApi api) { this.api = api; }

    @Override
    public void loginWithGoogle(String idToken, AuthRepository.Callback callback) {
        api.loginWithGoogle(new AuthApi.GoogleLoginRequest(idToken))
                .enqueue(new retrofit2.Callback<AuthApi.AuthResponse>() {
                    @Override
                    public void onResponse(Call<AuthApi.AuthResponse> call, Response<AuthApi.AuthResponse> res) {
                        if (res.isSuccessful() && res.body() != null) {
                            AuthApi.AuthResponse b = res.body();
                            callback.onSuccess(new AuthResult(b.accessToken, b.refreshToken, b.userId, b.email, b.name));
                        } else {
                            callback.onError(new RuntimeException("HTTP " + res.code()));
                        }
                    }

                    @Override
                    public void onFailure(Call<AuthApi.AuthResponse> call, Throwable t) {
                        callback.onError(t);
                    }
                });
    }

}
