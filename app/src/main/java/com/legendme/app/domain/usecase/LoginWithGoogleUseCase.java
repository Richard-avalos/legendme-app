package com.legendme.app.domain.usecase;

import com.legendme.app.domain.model.AuthResult;
import com.legendme.app.domain.repository.AuthRepository;

public class LoginWithGoogleUseCase {
    private final AuthRepository repo;
    public interface Callback {
        void onSuccess(AuthResult result);
        void onError(Throwable t);
    }

    public LoginWithGoogleUseCase(AuthRepository repo) {
        this.repo = repo;
    }

    public void execute(String idToken, Callback cb) {
        if (idToken == null || idToken.isEmpty()) {
            cb.onError(new IllegalArgumentException("ID Token vacío"));
            return;
        }

        repo.loginWithGoogle(idToken, new AuthRepository.Callback() {
            @Override public void onSuccess(AuthResult result) {
                cb.onSuccess(result);
            }
            @Override public void onError(Throwable t) {
                cb.onError(t);
            }

        });
    }

}
