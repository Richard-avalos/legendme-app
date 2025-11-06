package com.legendme.app.ui.mission;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.legendme.app.R;
import com.legendme.app.domain.model.Category;
import com.legendme.app.domain.model.Mission;
import com.legendme.app.domain.model.MissionCreateRequest;
import com.legendme.app.network.ApiClient;
import com.legendme.app.network.MissionService;

import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MissionCreateActivity extends AppCompatActivity {

    private Spinner spinnerCategory;
    private Spinner spinnerDifficulty;
    private EditText etTitle, etDescription, etStreakGroup;
    private Button btnDueAt, btnSubmit;
    private ProgressBar progress;

    private List<Category> categories = new ArrayList<>();
    private Calendar dueAtCal = Calendar.getInstance();

    private SharedPreferences prefs;
    private static final String PREFS = "legendme_auth";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Protegemos todo el flujo de inicialización para capturar cualquier excepción no esperada
        try {
            setContentView(R.layout.activity_mission_create);

            spinnerCategory = findViewById(R.id.spinner_category);
            spinnerDifficulty = findViewById(R.id.spinner_difficulty);
            etTitle = findViewById(R.id.et_title);
            etDescription = findViewById(R.id.et_description);
            etStreakGroup = findViewById(R.id.et_streak_group);
            btnDueAt = findViewById(R.id.btn_due_at);
            btnSubmit = findViewById(R.id.btn_submit);
            progress = findViewById(R.id.progress);

            prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
            String token = prefs.getString("accessToken", null);
            if (token == null) {
                Toast.makeText(this, "No autorizado", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            // Populate difficulty spinner
            ArrayAdapter<String> diffAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item,
                    new String[]{"EASY", "MEDIUM", "HARD"});
            diffAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerDifficulty.setAdapter(diffAdapter);

            // DueAt button
            updateDueAtButtonText();
            btnDueAt.setOnClickListener(v -> pickDateTime());

            // Load categories from API
            loadCategories(token);

            btnSubmit.setOnClickListener(v -> {
                createMission(token);
            });
        } catch (Exception e) {
            // Registrar en Logcat y volcar stacktrace a un archivo interno para que lo puedas recuperar desde Device File Explorer o adb
            Log.e("MCREATE", "Error en onCreate", e);
            try {
                StringWriter sw = new StringWriter();
                e.printStackTrace(new PrintWriter(sw));
                String trace = sw.toString();
                FileOutputStream fos = openFileOutput("last_crash.txt", MODE_PRIVATE);
                fos.write(trace.getBytes());
                fos.close();
            } catch (Exception ex) {
                Log.e("MCREATE", "No se pudo escribir el crash log: " + ex.getMessage());
            }
            Toast.makeText(this, "Se produjo un error al iniciar (ver last_crash.txt)", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void updateDueAtButtonText() {
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
        btnDueAt.setText(fmt.format(dueAtCal.getTime()));
    }

    private void pickDateTime() {
        int year = dueAtCal.get(Calendar.YEAR);
        int month = dueAtCal.get(Calendar.MONTH);
        int day = dueAtCal.get(Calendar.DAY_OF_MONTH);
        int hour = dueAtCal.get(Calendar.HOUR_OF_DAY);
        int minute = dueAtCal.get(Calendar.MINUTE);

        new DatePickerDialog(this, (view, y, m, d) -> {
            dueAtCal.set(Calendar.YEAR, y);
            dueAtCal.set(Calendar.MONTH, m);
            dueAtCal.set(Calendar.DAY_OF_MONTH, d);
            new TimePickerDialog(MissionCreateActivity.this, (timeView, h, min) -> {
                dueAtCal.set(Calendar.HOUR_OF_DAY, h);
                dueAtCal.set(Calendar.MINUTE, min);
                dueAtCal.set(Calendar.SECOND, 0);
                updateDueAtButtonText();
            }, hour, minute, true).show();
        }, year, month, day).show();
    }

    private void loadCategories(String token) {
        setLoading(true);
        MissionService svc = ApiClient.missionsRetrofit().create(MissionService.class);
        svc.listCategories("Bearer " + token).enqueue(new Callback<List<Category>>() {
            @Override
            public void onResponse(Call<List<Category>> call, Response<List<Category>> response) {
                setLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    categories = response.body();
                    List<String> names = new ArrayList<>();
                    for (Category c : categories) names.add(c.name != null ? c.name : String.valueOf(c.id));
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(MissionCreateActivity.this, android.R.layout.simple_spinner_item, names);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerCategory.setAdapter(adapter);
                } else {
                    Toast.makeText(MissionCreateActivity.this, "Error cargando categorías: " + response.code(), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<List<Category>> call, Throwable t) {
                setLoading(false);
                Toast.makeText(MissionCreateActivity.this, "Fallo de red al cargar categorías", Toast.LENGTH_LONG).show();
                Log.e("MCREATE", "listCategories failure", t);
            }
        });
    }

    private void setLoading(boolean loading) {
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnSubmit.setEnabled(!loading);
    }

    private void createMission(String token) {
        // Validaciones básicas
        if (spinnerCategory.getSelectedItemPosition() < 0 || categories.isEmpty()) {
            Toast.makeText(this, "Selecciona una categoría", Toast.LENGTH_SHORT).show();
            return;
        }
        String categoryId = String.valueOf(categories.get(spinnerCategory.getSelectedItemPosition()).id);
        String title = etTitle.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        String difficulty = (String) spinnerDifficulty.getSelectedItem();
        String streakGroup = etStreakGroup.getText().toString().trim();
        if (title.isEmpty()) { Toast.makeText(this, "Título requerido", Toast.LENGTH_SHORT).show(); return; }

        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
        String dueAt = fmt.format(dueAtCal.getTime());

        MissionCreateRequest req = new MissionCreateRequest(categoryId, title, description, dueAt, difficulty, streakGroup);

        setLoading(true);
        MissionService svc = ApiClient.missionsRetrofit().create(MissionService.class);
        svc.createMission("Bearer " + token, req).enqueue(new Callback<Mission>() {
            @Override
            public void onResponse(Call<Mission> call, Response<Mission> response) {
                setLoading(false);
                if (response.isSuccessful()) {
                    Toast.makeText(MissionCreateActivity.this, "Misión creada", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                } else {
                    Toast.makeText(MissionCreateActivity.this, "Error creando misión: " + response.code(), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<Mission> call, Throwable t) {
                setLoading(false);
                Toast.makeText(MissionCreateActivity.this, "Fallo de red creando misión", Toast.LENGTH_LONG).show();
                Log.e("MCREATE", "createMission failure", t);
            }
        });
    }
}
