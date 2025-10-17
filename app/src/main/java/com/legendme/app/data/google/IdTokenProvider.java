package com.legendme.app.data.google;

import android.app.Activity;
import android.content.Intent;
import android.os.CancellationSignal; // ✅ IMPORTANTE
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback; // ✅ IMPORTANTE
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.GetCredentialException;

import com.google.android.gms.auth.api.signin.*;
import com.google.android.gms.common.api.ApiException;
import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;

public class IdTokenProvider {

    public interface Callback {
        void onSuccess(String idToken);
        void onError(Throwable t);
    }

    private final Activity activity;
    private final CredentialManager cm;
    private final GoogleSignInClient legacyClient;
    private final ActivityResultLauncher<Intent> legacyLauncher;

    private Callback pendingCallback; // mantiene callback para fallback legacy

    public IdTokenProvider(Activity activity, String serverClientId, ActivityResultLauncher<Intent> legacyLauncher) {
        this.activity = activity;
        this.cm = CredentialManager.create(activity);
        this.legacyLauncher = legacyLauncher;

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(serverClientId)
                .requestEmail()
                .build();
        this.legacyClient = GoogleSignIn.getClient(activity, gso);
    }

    public void getIdToken(Callback cb, String serverClientId) {
        GetGoogleIdOption googleIdOption = new GetGoogleIdOption.Builder()
                .setServerClientId(serverClientId)
                .setFilterByAuthorizedAccounts(false)
                .setAutoSelectEnabled(false)
                .build();

        GetCredentialRequest request = new GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build();

        // ✅ Nuevo: necesitas un CancellationSignal
        CancellationSignal cancellationSignal = new CancellationSignal();

        // ✅ Firma correcta (context, request, cancellation, executor, callback)
        cm.getCredentialAsync(
                activity,                      // context
                request,                       // request
                cancellationSignal,            // cancellation
                activity.getMainExecutor(),    // executor
                new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                    @Override
                    public void onResult(GetCredentialResponse response) {
                        try {
                            Credential c = response.getCredential();
                            GoogleIdTokenCredential gc =
                                    GoogleIdTokenCredential.createFrom(c.getData());
                            String token = gc.getIdToken();
                            if (token == null || token.isEmpty()) {
                                cb.onError(new IllegalStateException("ID Token vacío"));
                            } else {
                                cb.onSuccess(token);
                            }
                        } catch (Exception e) {
                            fallback(cb);
                        }
                    }

                    @Override
                    public void onError(GetCredentialException e) {
                        fallback(cb);
                    }
                }
        );
    }

    private void fallback(Callback cb) {
        Intent intent = legacyClient.getSignInIntent();
        legacyLauncher.launch(intent);
        Toast.makeText(activity, "Usando método de compatibilidad…", Toast.LENGTH_SHORT).show();
        this.pendingCallback = cb; // se completará en handleLegacyResult
    }

    public void handleLegacyResult(Intent data) {
        if (pendingCallback == null) return;
        try {
            GoogleSignInAccount acct =
                    GoogleSignIn.getSignedInAccountFromIntent(data).getResult(ApiException.class);
            String token = (acct != null) ? acct.getIdToken() : null;
            if (token == null || token.isEmpty()) {
                pendingCallback.onError(new IllegalStateException("ID Token vacío (legacy)"));
            } else {
                pendingCallback.onSuccess(token);
            }
        } catch (ApiException e) {
            pendingCallback.onError(e);
        } finally {
            pendingCallback = null;
        }
    }
}
