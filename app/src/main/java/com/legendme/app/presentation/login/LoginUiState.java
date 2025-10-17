package com.legendme.app.presentation.login;

public class LoginUiState {
    public final boolean loading;
    public final String message;
    public LoginUiState(boolean loading, String message) {
        this.loading = loading; this.message = message;
    }
    public static LoginUiState idle() { return new LoginUiState(false, null); }
    public static LoginUiState loading() { return new LoginUiState(true, null); }
    public static LoginUiState success(String msg) { return new LoginUiState(false, msg); }
    public static LoginUiState error(String msg) { return new LoginUiState(false, msg); }
}
