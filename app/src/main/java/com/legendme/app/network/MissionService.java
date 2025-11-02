package com.legendme.app.network;

import com.legendme.app.domain.model.Mission;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface MissionService {
    // El endpoint es POST missions/query con body vacío
    @POST("/missions/query")
    Call<List<Mission>> queryMissions(
            @Header("Authorization") String bearerToken,
            @Body Object empty // o usa RequestBody.create("{}", ...)
    );
}