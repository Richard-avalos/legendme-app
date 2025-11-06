package com.legendme.app.domain.model;

public class MissionCreateRequest {
    public String categoryId;
    public String title;
    public String description;
    public String dueAt;
    public String difficulty;
    public String streakGroup;

    public MissionCreateRequest() {}

    public MissionCreateRequest(String categoryId, String title, String description, String dueAt, String difficulty, String streakGroup) {
        this.categoryId = categoryId;
        this.title = title;
        this.description = description;
        this.dueAt = dueAt;
        this.difficulty = difficulty;
        this.streakGroup = streakGroup;
    }
}

