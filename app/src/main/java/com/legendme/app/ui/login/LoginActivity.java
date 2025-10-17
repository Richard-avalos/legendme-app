package com.legendme.app.ui.login;

import android.content.Intent;
import android.credentials.CredentialManager;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;

import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.legendme.app.R;
import com.legendme.app.di.AppContainer;
import com.legendme.app.presentation.login.LoginUiState;


public class LoginActivity extends AppCompatActivity {
    private AppContainer app;

    private final ActivityResultLauncher<Intent> legacyLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getData() != null) {
                    app.idTokenProvider.handleLegacyResult(result.getData());
                } else {
                    Toast.makeText(this, "Operación cancelada", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        app = new AppContainer(
                this,
                getString(R.string.backend_base_url),
                getString(R.string.server_client_id),
                legacyLauncher
        );

        app.loginViewModel.getState().observe(this, new Observer<LoginUiState>() {
            @Override public void onChanged(LoginUiState s) {
                if (s.loading) {
                    // muestra loader
                } else if (s.message != null) {
                    Toast.makeText(LoginActivity.this, s.message, Toast.LENGTH_SHORT).show();
                    // si éxito, navega al Home
                    // startActivity(new Intent(LoginActivity.this, HomeActivity.class));
                    // finish();
                }
            }
        });

        LinearLayout btnGoogle = findViewById(R.id.btn_google);
        btnGoogle.setOnClickListener(v ->
                app.idTokenProvider.getIdToken(new com.legendme.app.data.google.IdTokenProvider.Callback() {
                    @Override public void onSuccess(String idToken) {
                        app.loginViewModel.loginWithIdToken(idToken);
                    }
                    @Override public void onError(Throwable t) {
                        Toast.makeText(LoginActivity.this, "Google error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }, getString(R.string.server_client_id))
        );
    }

}
