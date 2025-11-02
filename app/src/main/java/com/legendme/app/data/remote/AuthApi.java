package com.legendme.app.data.remote;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface  AuthApi {
    class GoogleLoginRequest {
        String idToken;
        public GoogleLoginRequest(String idToken) {
            this.idToken = idToken;
        }
    }

    class AuthResponse {
        String accessToken;
        String refreshToken;
        String userId;
        String email;
        String name;
    }

    @POST("/login/google")
    Call<AuthResponse> loginWithGoogle(@Body GoogleLoginRequest body);
}
