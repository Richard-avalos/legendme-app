package com.legendme.app.network;

import com.legendme.app.domain.model.UserDetail;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Path;

public interface UserService {
    @GET("/users/search/by-id/{id}")
    Call<UserDetail> getById(
            @Path("id") String userId,
            @Header("X-Internal-Token") String internalToken
    );
}