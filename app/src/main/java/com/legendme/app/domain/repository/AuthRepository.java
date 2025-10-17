package com.legendme.app.domain.repository;

import com.legendme.app.domain.model.AuthResult;

public interface AuthRepository {
    interface Callback {
        void onSuccess(AuthResult result);
        void onError(Throwable t);
    }
    void loginWithGoogle(String idToken, Callback callback);
}
