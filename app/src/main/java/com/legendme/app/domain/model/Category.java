package com.legendme.app.domain.model;

public class Category {
    public int id;
    public String code;
    public String name;
    public int baseXP;
    public boolean isActive;

    @Override
    public String toString() {
        return "Category{" +
                "id=" + id +
                ", code='" + code + '\'' +
                ", name='" + name + '\'' +
                ", baseXP=" + baseXP +
                ", isActive=" + isActive +
                '}';
    }
}

