package com.legendme.app.presentation.login;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.legendme.app.data.storage.AuthStorage;
import com.legendme.app.domain.model.AuthResult;
import com.legendme.app.domain.usecase.LoginWithGoogleUseCase;

public class LoginViewModel {
    private final LoginWithGoogleUseCase useCase;
    private final AuthStorage storage;

    private final MutableLiveData<LoginUiState> state = new MutableLiveData<>(LoginUiState.idle());

    public LiveData<LoginUiState> getState() {
        return state;
    }

    public LoginViewModel(LoginWithGoogleUseCase useCase, AuthStorage storage) {
        this.useCase = useCase;
        this.storage = storage;
    }

    public void loginWithIdToken(String idToken) {
        state.postValue(LoginUiState.loading());
        useCase.execute(idToken, new LoginWithGoogleUseCase.Callback() {
            @Override
            public void onSuccess(AuthResult r) {
                storage.saveTokens(r.AccessToken(), r.refreshToken());
                storage.saveProfile(r.userId(), r.email(), r.name());
                String msg = "¡Bienvenido, " + (r.name() != null ? r.name() : "usuario") + "!";
                state.postValue(LoginUiState.success(msg, r));
            }

            @Override
            public void onError(Throwable t) {
                state.postValue(LoginUiState.error("Error: " + (t.getMessage() != null ? t.getMessage() : "desconocido")));
            }
        });
    }
}
