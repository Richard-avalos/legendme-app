package com.legendme.app.domain.model;

public class Mission {
    public String id;
    public String userId;
    public String title;
    public String description;
    public String categoryCode;
    public String categotyName; // el backend a veces usa esta clave con typo
    public int baseXp;
    public String difficulty; // "Fácil", "Media", "Difícil"
    public String streakGroup;
    public String status;     // "Pendiente", "En progreso", "Completada"

    public String startedAt;
    public String dueAt;
    public String completedAt;
    public String createdAt;
    public String updatedAt;

    @Override
    public String toString() {
        return "Mission{" +
                "id='" + id + '\'' +
                ", title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", xp=" + baseXp +
                ", difficulty='" + difficulty + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}