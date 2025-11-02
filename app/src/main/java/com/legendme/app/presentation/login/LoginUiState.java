package com.legendme.app.presentation.login;

import androidx.annotation.Nullable;

import com.legendme.app.domain.model.AuthResult;

public class LoginUiState {
    public final boolean loading;
    public final String message;
public final AuthResult result;

    public LoginUiState(boolean loading, String message, @Nullable AuthResult result) {
        this.loading = loading; this.message = message;
        this.result = result;
    }

    public boolean isLoading() { return loading; }
    @Nullable public String getMessage() { return message; }
    @Nullable public AuthResult getResult() { return result; }

    public static LoginUiState idle() { return new LoginUiState(false, null, null); }
    public static LoginUiState loading() { return new LoginUiState(true, null, null); }
    public static LoginUiState success(String msg, AuthResult result) { return new LoginUiState(false, msg, result); }
    public static LoginUiState error(String msg) { return new LoginUiState(false, msg, null); }
}
