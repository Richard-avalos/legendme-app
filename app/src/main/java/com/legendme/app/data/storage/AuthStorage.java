package com.legendme.app.data.storage;

import android.content.Context;
import android.content.SharedPreferences;

public class AuthStorage {
    private final SharedPreferences sp;
    public AuthStorage(Context ctx) {
        sp = ctx.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE);
    }
    public void saveTokens(String access, String refresh) {
        sp.edit().putString("access", access).putString("refresh", refresh).apply();
    }

    public void saveProfile(String userId, String email, String name) {
        sp.edit().putString("userId", userId).putString("email", email).putString("name", name).apply();
    }

    public String getAccessToken() { return sp.getString("access", null); }
    public void clear() { sp.edit().clear().apply(); }
}
