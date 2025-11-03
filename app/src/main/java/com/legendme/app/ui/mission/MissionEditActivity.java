package com.legendme.app.ui.mission;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.legendme.app.R;
import com.legendme.app.domain.model.Category;
import com.legendme.app.domain.model.Mission;
import com.legendme.app.domain.model.MissionUpdateRequest;
import com.legendme.app.network.ApiClient;
import com.legendme.app.network.MissionService;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MissionEditActivity extends AppCompatActivity {
    private static final String TAG = "MissionEdit";
    private static final String PREFS = "legendme_auth";

    public static final String EXTRA_MISSION_ID = "extra_mission_id";

    private TextInputEditText etTitle, etDescription, etStreakGroup;
    private Spinner spinnerCategory, spinnerDifficulty;
    private MaterialButton btnSave;
    private CircularProgressIndicator progressSave;

    private Mission current;
    private final List<Category> categories = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mission_edit);

        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            }
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        etTitle = findViewById(R.id.etTitle);
        etDescription = findViewById(R.id.etDescription);
        etStreakGroup = findViewById(R.id.etStreakGroup);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        spinnerDifficulty = findViewById(R.id.spinnerDifficulty);
        btnSave = findViewById(R.id.btnSave);
        progressSave = findViewById(R.id.progressSave);

        // Cargar opciones de dificultad estáticas
        // Adapter personalizado para aplicar color al texto del spinner
        final int spinnerTextColor = ContextCompat.getColor(this, R.color.blanco_lunar);
        ArrayAdapter<String> diffAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, new String[]{"EASY","MEDIUM","HARD"}){
            @Override
            public TextView getView(int position, android.view.View convertView, android.view.ViewGroup parent) {
                TextView tv = (TextView) super.getView(position, convertView, parent);
                tv.setTextColor(spinnerTextColor);
                return tv;
            }

            @Override
            public android.view.View getDropDownView(int position, android.view.View convertView, android.view.ViewGroup parent) {
                TextView tv = (TextView) super.getDropDownView(position, convertView, parent);
                tv.setTextColor(spinnerTextColor);
                return tv;
            }
        };
        diffAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDifficulty.setAdapter(diffAdapter);

        String id = getIntent().getStringExtra(EXTRA_MISSION_ID);
        if (id == null || id.isEmpty()) {
            Toast.makeText(this, "ID de misión inválido", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        String token = prefs.getString("accessToken", null);
        if (token == null || token.isEmpty()) {
            Toast.makeText(this, "No hay token de acceso", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Primero cargar categorías
        loadCategories(token);

        // También cargar la misión actual para prellenar
        loadMission(id, token);

        btnSave.setOnClickListener(v -> onSaveClicked(token));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void loadCategories(String token) {
        MissionService svc = ApiClient.missionsRetrofit().create(MissionService.class);
        svc.listCategories("Bearer " + token).enqueue(new Callback<List<Category>>() {
            @Override
            public void onResponse(Call<List<Category>> call, Response<List<Category>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    categories.clear();
                    categories.addAll(response.body());
                    List<String> names = new ArrayList<>();
                    for (Category c : categories) names.add(c.name);
                    // Adapter personalizado para aplicar color al texto del spinner de categorías
                    final int spinnerTextColor = ContextCompat.getColor(MissionEditActivity.this, R.color.blanco_lunar);
                    ArrayAdapter<String> adapter = new ArrayAdapter<String>(MissionEditActivity.this, android.R.layout.simple_spinner_item, names) {
                        @Override
                        public TextView getView(int position, android.view.View convertView, android.view.ViewGroup parent) {
                            TextView tv = (TextView) super.getView(position, convertView, parent);
                            tv.setTextColor(spinnerTextColor);
                            return tv;
                        }

                        @Override
                        public android.view.View getDropDownView(int position, android.view.View convertView, android.view.ViewGroup parent) {
                            TextView tv = (TextView) super.getDropDownView(position, convertView, parent);
                            tv.setTextColor(spinnerTextColor);
                            return tv;
                        }
                    };
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerCategory.setAdapter(adapter);

                    // si ya tenemos la misión cargada, intentar seleccionar la categoría correspondiente
                    if (current != null) selectCategoryForCurrent();
                } else {
                    Log.w(TAG, "listCategories no exitosa: code=" + response.code());
                    Toast.makeText(MissionEditActivity.this, "No se pudieron cargar categorías", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Category>> call, Throwable t) {
                Log.e(TAG, "Error cargando categorías", t);
                Toast.makeText(MissionEditActivity.this, "Error al cargar categorías", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadMission(String id, String token) {
        MissionService svc = ApiClient.missionsRetrofit().create(MissionService.class);
        svc.getById("Bearer " + token, id).enqueue(new Callback<Mission>() {
            @Override
            public void onResponse(Call<Mission> call, Response<Mission> response) {
                if (response.isSuccessful() && response.body() != null) {
                    current = response.body();
                    prefillFields();
                } else {
                    Log.w(TAG, "getById no exitosa: code=" + response.code());
                    Toast.makeText(MissionEditActivity.this, "No se pudo cargar la misión", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onFailure(Call<Mission> call, Throwable t) {
                Log.e(TAG, "Error cargando misión", t);
                Toast.makeText(MissionEditActivity.this, "Error al cargar misión", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void prefillFields() {
        if (current == null) return;
        runOnUiThread(() -> {
            etTitle.setText(current.title);
            etDescription.setText(current.description);
            etStreakGroup.setText(current.streakGroup);
            // dificultad
            if (current.difficulty != null) {
                String up = current.difficulty.toUpperCase();
                if (up.contains("EASY") || up.contains("FÁCIL") || up.contains("FACIL")) spinnerDifficulty.setSelection(0);
                else if (up.contains("MEDIUM") || up.contains("MEDIA") ) spinnerDifficulty.setSelection(1);
                else spinnerDifficulty.setSelection(2);
            }
            // categoría
            selectCategoryForCurrent();
        });
    }

    private void selectCategoryForCurrent() {
        if (current == null || categories.isEmpty()) return;
        // intentar comparar por id si existe, o por nombre/codigo
        for (int i = 0; i < categories.size(); i++) {
            Category c = categories.get(i);
            try {
                if (current.categoryCode != null && current.categoryCode.equals(c.code)) { spinnerCategory.setSelection(i); return; }
            } catch (Exception ignored) {}
            try {
                if (current.categotyName != null && current.categotyName.equalsIgnoreCase(c.name)) { spinnerCategory.setSelection(i); return; }
            } catch (Exception ignored) {}
            try {
                // si mission tuviera un category id en otro campo
                if (String.valueOf(c.id).equals(String.valueOf(current.id))) { spinnerCategory.setSelection(i); return; }
            } catch (Exception ignored) {}
        }
    }

    private void setUiEnabled(boolean enabled) {
        etTitle.setEnabled(enabled);
        etDescription.setEnabled(enabled);
        etStreakGroup.setEnabled(enabled);
        spinnerCategory.setEnabled(enabled);
        spinnerDifficulty.setEnabled(enabled);
        btnSave.setEnabled(enabled);
        progressSave.setVisibility(enabled ? android.view.View.GONE : android.view.View.VISIBLE);
    }

    private void onSaveClicked(String token) {
        if (current == null) return;
        setUiEnabled(false);
        String title = etTitle.getText() == null ? "" : etTitle.getText().toString().trim();
        String desc = etDescription.getText() == null ? "" : etDescription.getText().toString().trim();
        String streak = etStreakGroup.getText() == null ? "" : etStreakGroup.getText().toString().trim();
        int selCat = spinnerCategory.getSelectedItemPosition();
        String catId = selCat >= 0 && selCat < categories.size() ? String.valueOf(categories.get(selCat).id) : "1";
        String difficulty = spinnerDifficulty.getSelectedItem() == null ? "MEDIUM" : spinnerDifficulty.getSelectedItem().toString();

        MissionUpdateRequest req = new MissionUpdateRequest(current.id, title, desc, catId, difficulty, streak);

        MissionService svc = ApiClient.missionsRetrofit().create(MissionService.class);
        svc.updateMission("Bearer " + token, current.id, req).enqueue(new Callback<Mission>() {
            @Override
            public void onResponse(Call<Mission> call, Response<Mission> response) {
                setUiEnabled(true);
                if (response.isSuccessful() && response.body() != null) {
                    Toast.makeText(MissionEditActivity.this, "Misión actualizada", Toast.LENGTH_SHORT).show();
                    // volver al detalle y forzar recarga
                    Intent it = new Intent();
                    it.putExtra("updated", true);
                    setResult(RESULT_OK, it);
                    finish();
                } else {
                    Log.w(TAG, "updateMission no exitosa: code=" + response.code());
                    Toast.makeText(MissionEditActivity.this, "Fallo al actualizar misión", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Mission> call, Throwable t) {
                setUiEnabled(true);
                Log.e(TAG, "Error actualizando misión", t);
                Toast.makeText(MissionEditActivity.this, "Error al actualizar misión", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
