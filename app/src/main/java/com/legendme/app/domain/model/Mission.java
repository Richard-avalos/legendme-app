package com.legendme.app.domain.model;

public class Mission {
    public String id;
    public String title;
    public String description;
    public int baseXp;
    public String difficulty; // "Fácil", "Media", "Difícil"
    public String status;     // "Pendiente", "En progreso", "Completada"

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