package com.legendme.app.domain.model;

public class MissionUpdateRequest {
    public String id;
    public String title;
    public String description;
    public String categoryId; // enviamos como string para coincidir con el ejemplo
    public String difficulty;
    public String streakGroup;

    public MissionUpdateRequest() {}

    public MissionUpdateRequest(String id, String title, String description, String categoryId, String difficulty, String streakGroup) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.categoryId = categoryId;
        this.difficulty = difficulty;
        this.streakGroup = streakGroup;
    }
}

