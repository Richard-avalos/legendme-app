package com.legendme.app.network;

import com.legendme.app.domain.model.Mission;
import com.legendme.app.domain.model.Category;
import com.legendme.app.domain.model.MissionUpdateRequest;
import com.legendme.app.domain.model.MissionCreateRequest;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.PUT;
import retrofit2.http.PATCH;

public interface MissionService {
    // El endpoint es POST missions/query con body vacío
    @POST("/missions/query")
    Call<List<Mission>> queryMissions(
            @Header("Authorization") String bearerToken,
            @Body Object empty // o usa RequestBody.create("{}", ...)
    );

    // Nuevo: buscar misión por id (GET)
    @GET("/missions/search-by-id/{id}")
    Call<Mission> getById(
            @Header("Authorization") String bearerToken,
            @Path("id") String id
    );

    // Nuevo: listar categorías
    @GET("/categories/list-categories")
    Call<List<Category>> listCategories(
            @Header("Authorization") String bearerToken
    );

    // Nuevo: actualizar misión
    @PUT("/missions/update/{id}")
    Call<Mission> updateMission(
            @Header("Authorization") String bearerToken,
            @Path("id") String id,
            @Body MissionUpdateRequest body
    );

    // Nuevo: iniciar misión (PATCH /missions/{id}/start) - el backend acepta un PATCH con body vacío
    @PATCH("/missions/{id}/start")
    Call<Void> startMission(
            @Header("Authorization") String bearerToken,
            @Path("id") String id,
            @Body Object empty
    );

    // Nuevo: pausar misión (PATCH /missions/{id}/pause) - el backend acepta un PATCH con body vacío
    @PATCH("/missions/{id}/pause")
    Call<Void> pauseMission(
            @Header("Authorization") String bearerToken,
            @Path("id") String id,
            @Body Object empty
    );


    // Nuevo: cancelar misión (PATCH /missions/{id}/cancel)
    @PATCH("/missions/{id}/cancel")
    Call<Void> cancelMission(
            @Header("Authorization") String bearerToken,
            @Path("id") String id,
            @Body Object empty
    );

    // Nuevo: completar misión (PATCH /missions/{id}/complete)
    @PATCH("/missions/{id}/complete")
    Call<Void> completeMission(
            @Header("Authorization") String bearerToken,
            @Header("Idempotency-Key") String idempotencyKey,
            @Path("id") String id,
            @Body Object empty
    );

    // Nuevo: crear misión (POST /missions/create)
    @POST("/missions/create")
    Call<Mission> createMission(
            @Header("Authorization") String bearerToken,
            @Body MissionCreateRequest body
    );
}