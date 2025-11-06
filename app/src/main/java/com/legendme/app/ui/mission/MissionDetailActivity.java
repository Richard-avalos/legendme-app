package com.legendme.app.ui.mission;

import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.google.android.material.chip.Chip;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.legendme.app.R;
import com.legendme.app.domain.model.Mission;
import com.legendme.app.network.ApiClient;
import com.legendme.app.network.MissionService;

import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MissionDetailActivity extends AppCompatActivity {
    public static final String EXTRA_MISSION_ID = "extra_mission_id";
    private static final String PREFS = "legendme_auth";
    private static final String TAG = "MissionDetail";

    // Modo de depuración: si true, no cerramos la Activity automáticamente al fallar la carga
    private static final boolean DEBUG_SHOW_STATE = true;

    private ProgressBar progress;
    private TextView tvTitle, tvDescription, tvMeta, tvDates, tvState;

    // Nuevas vistas
    private Chip chipStatus;
    private LinearProgressIndicator linearProgress;
    private TextView tvProgressPercent;
    private TextView tvXp; // destacamos el XP
    private FloatingActionButton fabStartMission; // FAB para iniciar misión
    private ImageButton btnOptions; // Botón de opciones (menú desplegable)

    // Botón editar y estado
    private MaterialButton btnEditMission;
    private MaterialButton btnStartMission; // agregado: botón para iniciar misión
    private MaterialButton btnPauseMission; // agregado: botón para pausar misión
    private MaterialButton btnCancelMission;
    private MaterialButton btnCompleteMission;


    private String missionId;
    private String accessToken;
    private static final int REQ_EDIT = 1234;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Proteger inicialización completa para capturar errores que causen cierre sin log
        try {
            setContentView(R.layout.activity_mission_detail);

            // Toolbar (match dashboard)
            Toolbar toolbar = findViewById(R.id.toolbar);
            if (toolbar != null) {
                setSupportActionBar(toolbar);
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                    getSupportActionBar().setTitle(R.string.missions_title);
                }
                toolbar.setNavigationOnClickListener(v -> finish());
            }

            progress = findViewById(R.id.progress);
            tvTitle = findViewById(R.id.tvTitle);
            tvDescription = findViewById(R.id.tvDescription);
            tvMeta = findViewById(R.id.tvMeta);
            tvDates = findViewById(R.id.tvDates);
            tvState = findViewById(R.id.tvState);

            // Enlazar nuevas vistas
            chipStatus = findViewById(R.id.chipStatus);
            linearProgress = findViewById(R.id.linearProgress);
            tvProgressPercent = findViewById(R.id.tvProgressPercent);
            tvXp = findViewById(R.id.tvXp); // nueva vista para XP
            btnEditMission = findViewById(R.id.btnEditMission);
            btnStartMission = findViewById(R.id.btnStartMission); // enlazado
            btnPauseMission = findViewById(R.id.btnPauseMission); // enlazado
            btnCancelMission = findViewById(R.id.btnCancelMission);
            btnCompleteMission = findViewById(R.id.btnCompleteMission);
            btnOptions = findViewById(R.id.btnOptions); // botón de menú desplegable

            // Configurar el botón de opciones para mostrar menú desplegable
            if (btnOptions != null) {
                btnOptions.setOnClickListener(v -> showOptionsMenu(v));
            }

            // listener del FAB: dispara la misma acción que el botón iniciar
            if (fabStartMission != null) {
                fabStartMission.setOnClickListener(v -> {
                    fabStartMission.setEnabled(false);
                    if (btnStartMission != null) btnStartMission.setEnabled(false);
                    performStartMission();
                });
            }

            if (btnCompleteMission != null) {
                btnCompleteMission.setOnClickListener(v -> {
                    btnCompleteMission.setEnabled(false);
                    performCompleteMission();
                });
            }


            // click para pausar misión
            if (btnPauseMission != null) {
                btnPauseMission.setOnClickListener(v -> {
                    // desactivar para evitar múltiples clicks
                    btnPauseMission.setEnabled(false);
                    if (btnStartMission != null) btnStartMission.setEnabled(false);
                    performPauseMission();
                });
            }

            // click para iniciar misión
            if (btnStartMission != null) {
                btnStartMission.setOnClickListener(v -> {
                    // desactivar para evitar múltiples clicks
                    btnStartMission.setEnabled(false);
                    performStartMission();
                });
            }

            if (btnCancelMission != null) {
                btnCancelMission.setOnClickListener(v -> {
                    btnCancelMission.setEnabled(false);
                    performCancelMission();
                });
            }

            // Recuperar ID de la Intent y token de SharedPreferences
            String id = getIntent().getStringExtra(EXTRA_MISSION_ID);
            if (id == null || id.isEmpty()) {
                Toast.makeText(this, "ID de misión inválido", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            missionId = id;

            SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
            String token = prefs.getString("accessToken", null);
            if (token == null || token.isEmpty()) {
                Toast.makeText(this, "No hay token de acceso disponible", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            accessToken = token;

            // setear click del botón editar (si existe)
            if (btnEditMission != null) {
                btnEditMission.setOnClickListener(v -> {
                    try {
                        android.content.Intent it = new android.content.Intent(MissionDetailActivity.this, MissionEditActivity.class);
                        it.putExtra(EXTRA_MISSION_ID, missionId);
                        startActivityForResult(it, REQ_EDIT);
                    } catch (Exception e) {
                        Log.w(TAG, "No se pudo lanzar edición: " + e.getMessage());
                        Toast.makeText(MissionDetailActivity.this, "No se puede abrir editor", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            // Cargar datos iniciales
            loadMission(missionId, accessToken);
        } catch (Exception e) {
            Log.e(TAG, "Error en onCreate", e);
            try {
                StringWriter sw = new StringWriter();
                e.printStackTrace(new PrintWriter(sw));
                String trace = sw.toString();
                FileOutputStream fos = openFileOutput("last_crash.txt", MODE_PRIVATE);
                fos.write(trace.getBytes());
                fos.close();
            } catch (Exception ex) {
                Log.e(TAG, "No se pudo escribir el crash log: " + ex.getMessage());
            }
            Toast.makeText(this, "Se produjo un error al iniciar (ver last_crash.txt)", Toast.LENGTH_LONG).show();
            if (!DEBUG_SHOW_STATE) finish();
            else {
                // dejar la activity abierta en modo debug pero con vistas ocultas para evitar NPEs posteriores
                if (progress != null) progress.setVisibility(View.GONE);
            }
        }
    }

    /**
     * Muestra el menú desplegable con las acciones disponibles
     */
    private void showOptionsMenu(View anchorView) {
        // Inflar menú desde recursos para facilitar localización y estilos
        PopupMenu popupMenu = new PopupMenu(this, anchorView);
        popupMenu.getMenuInflater().inflate(R.menu.menu_mission_options, popupMenu.getMenu());

        // Ajustar visibilidad de items según estado actual de botones
        android.view.Menu menu = popupMenu.getMenu();
        android.view.MenuItem itStart = menu.findItem(R.id.action_start);
        android.view.MenuItem itPause = menu.findItem(R.id.action_pause);
        android.view.MenuItem itComplete = menu.findItem(R.id.action_complete);
        android.view.MenuItem itCancel = menu.findItem(R.id.action_cancel);
        android.view.MenuItem itEdit = menu.findItem(R.id.action_edit);

        if (itStart != null) itStart.setVisible(btnStartMission != null && btnStartMission.getVisibility() == View.VISIBLE);
        if (itPause != null) itPause.setVisible(btnPauseMission != null && btnPauseMission.getVisibility() == View.VISIBLE);
        if (itComplete != null) itComplete.setVisible(btnCompleteMission != null && btnCompleteMission.getVisibility() == View.VISIBLE);
        if (itCancel != null) itCancel.setVisible(btnCancelMission != null && btnCancelMission.getVisibility() == View.VISIBLE);
        if (itEdit != null) itEdit.setVisible(btnEditMission != null && btnEditMission.getVisibility() != View.GONE);

        // Aplicar títulos con color claro para asegurar contraste
        int textColor = ContextCompat.getColor(this, R.color.blanco_lunar);
        for (int i = 0; i < menu.size(); i++) {
            android.view.MenuItem mi = menu.getItem(i);
            if (mi != null && mi.isVisible()) {
                android.text.SpannableString s = new android.text.SpannableString(mi.getTitle());
                s.setSpan(new android.text.style.ForegroundColorSpan(textColor), 0, s.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                mi.setTitle(s);
            }
        }

        // Listener de selección
        popupMenu.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.action_start) {
                if (btnStartMission != null) { btnStartMission.setEnabled(false); performStartMission(); }
                return true;
            } else if (id == R.id.action_pause) {
                if (btnPauseMission != null) { btnPauseMission.setEnabled(false); if (btnStartMission!=null) btnStartMission.setEnabled(false); performPauseMission(); }
                return true;
            } else if (id == R.id.action_complete) {
                if (btnCompleteMission != null) { btnCompleteMission.setEnabled(false); performCompleteMission(); }
                return true;
            } else if (id == R.id.action_cancel) {
                if (btnCancelMission != null) { btnCancelMission.setEnabled(false); performCancelMission(); }
                return true;
            } else if (id == R.id.action_edit) {
                if (btnEditMission != null) {
                    try { android.content.Intent it = new android.content.Intent(MissionDetailActivity.this, MissionEditActivity.class); it.putExtra(EXTRA_MISSION_ID, missionId); startActivityForResult(it, REQ_EDIT); }
                    catch (Exception e){ Log.w(TAG, "No se pudo lanzar edición: " + e.getMessage()); Toast.makeText(MissionDetailActivity.this, "No se puede abrir editor", Toast.LENGTH_SHORT).show(); }
                }
                return true;
            }
            return false;
        });

        // Mostrar popup y luego aplicar fondo drawable mediante reflexión (compatibilidad amplia)
        popupMenu.show();
        try {
            java.lang.reflect.Field popupField = PopupMenu.class.getDeclaredField("mPopup");
            popupField.setAccessible(true);
            Object menuPopupHelper = popupField.get(popupMenu);
            if (menuPopupHelper != null) {
                java.lang.reflect.Method setBg = menuPopupHelper.getClass().getMethod("setPopupBackgroundDrawable", android.graphics.drawable.Drawable.class);
                android.graphics.drawable.Drawable bg = ContextCompat.getDrawable(this, R.drawable.popup_menu_bg);
                setBg.invoke(menuPopupHelper, bg);
            }
        } catch (Exception e) {
            // fallback: intentar acceder al ListView interno y poner background
            try {
                java.lang.reflect.Field popupField = PopupMenu.class.getDeclaredField("mPopup");
                popupField.setAccessible(true);
                Object menuPopupHelper = popupField.get(popupMenu);
                java.lang.reflect.Method getListView = menuPopupHelper.getClass().getMethod("getListView");
                android.view.View listView = (android.view.View) getListView.invoke(menuPopupHelper);
                if (listView != null) listView.setBackgroundResource(R.drawable.popup_menu_bg);
            } catch (Exception ex) {
                Log.w(TAG, "No se pudo aplicar fondo personalizado al PopupMenu: " + ex.getMessage());
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable android.content.Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_EDIT && resultCode == RESULT_OK) {
            if (data != null && data.getBooleanExtra("updated", false)) {
                // recargar misión
                if (missionId != null && accessToken != null) {
                    loadMission(missionId, accessToken);
                }
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void loadMission(String id, String accessToken) {
        progress.setVisibility(View.VISIBLE);
        MissionService svc = ApiClient.missionsRetrofit().create(MissionService.class);
        svc.getById("Bearer " + accessToken, id).enqueue(new Callback<Mission>() {
            @Override
            public void onResponse(Call<Mission> call, Response<Mission> response) {
                progress.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "Misión cargada: " + response.body());
                    populate(response.body());
                } else {
                    Log.w(TAG, "Respuesta no exitosa o cuerpo nulo, code=" + response.code());
                    Toast.makeText(MissionDetailActivity.this, "No se pudo cargar la misión", Toast.LENGTH_SHORT).show();
                    if (!DEBUG_SHOW_STATE) {
                        finish();
                    }
                }
            }

            @Override
            public void onFailure(Call<Mission> call, Throwable t) {
                progress.setVisibility(View.GONE);
                Log.e(TAG, "Error cargando misión", t);
                Toast.makeText(MissionDetailActivity.this, "Error al cargar la misión", Toast.LENGTH_SHORT).show();
                if (!DEBUG_SHOW_STATE) {
                    finish();
                }
            }
        });
    }

    private void populate(Mission m) {
        Log.d(TAG, "populate(): m=" + m);
        tvTitle.setText(safe(m.title));
        tvDescription.setText(safe(m.description));

        String dueAt = "Desconocida";
        String createdAt = "Desconocida";
        String startedAt = "No iniciada";
        String completedAt = "No completada";

        if (m.dueAt != null && !m.dueAt.isEmpty()) {
            dueAt = formatIsoDate(m.dueAt);
        }

        if (m.createdAt != null && !m.createdAt.isEmpty()) {
            createdAt = formatIsoDate(m.createdAt);
        }

        if (m.startedAt != null && !m.startedAt.isEmpty()) {
            startedAt = formatIsoDate(m.startedAt);
        }

        if (m.completedAt != null && !m.completedAt.isEmpty()) {
            completedAt = formatIsoDate(m.completedAt);
        }

        if (getSupportActionBar() != null && m.title != null) {
            getSupportActionBar().setTitle(m.title);
        }

        // Construir tvMeta con labels en negrita y valores normales
        try {
            SpannableStringBuilder metaBuilder = new SpannableStringBuilder();

            String lblXp = getString(R.string.lbl_xp);
            int start = metaBuilder.length();
            metaBuilder.append(lblXp);
            metaBuilder.setSpan(new StyleSpan(Typeface.BOLD), start, start + lblXp.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            // Insertar non-breaking space antes del valor para asegurar separación visible
            metaBuilder.append("\u00A0").append(String.valueOf(m.baseXp)).append("\n");

            String lblDiff = getString(R.string.lbl_difficulty);
            start = metaBuilder.length();
            metaBuilder.append(lblDiff);
            metaBuilder.setSpan(new StyleSpan(Typeface.BOLD), start, start + lblDiff.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            metaBuilder.append("\u00A0").append(safe(m.difficulty)).append("\n");

            String lblState = getString(R.string.lbl_state);
            start = metaBuilder.length();
            metaBuilder.append(lblState);
            metaBuilder.setSpan(new StyleSpan(Typeface.BOLD), start, start + lblState.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            metaBuilder.append("\u00A0").append(safe(m.status));

            tvMeta.setText(metaBuilder);
            tvMeta.setVisibility(View.VISIBLE);
        } catch (Exception e) {
            // fallback
            String meta = getString(R.string.lbl_xp) + "\u00A0" + m.baseXp
                    + "\n" + getString(R.string.lbl_difficulty) + "\u00A0" + safe(m.difficulty)
                    + "\n" + getString(R.string.lbl_state) + "\u00A0" + safe(m.status);
            tvMeta.setText(meta);
            tvMeta.setVisibility(View.VISIBLE);
        }

        // Fechas: Fecha límite / Fecha de creación con labels en negrita
        try {
            SpannableStringBuilder datesBuilder = new SpannableStringBuilder();
            String lblDue = getString(R.string.lbl_due);
            int s = datesBuilder.length();
            datesBuilder.append(lblDue);
            datesBuilder.setSpan(new StyleSpan(Typeface.BOLD), s, s + lblDue.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            datesBuilder.append("\u00A0").append(dueAt).append("\n");

            String lblCreated = getString(R.string.lbl_created);
            s = datesBuilder.length();
            datesBuilder.append(lblCreated);
            datesBuilder.setSpan(new StyleSpan(Typeface.BOLD), s, s + lblCreated.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            datesBuilder.append("\u00A0").append(createdAt);

            tvDates.setText(datesBuilder);
            tvDates.setVisibility(View.VISIBLE);
        } catch (Exception e) {
            tvDates.setText(getString(R.string.lbl_due) + "\u00A0" + dueAt + "\n" + getString(R.string.lbl_created) + "\u00A0" + createdAt);
            tvDates.setVisibility(View.VISIBLE);
        }

        // Formatear estados de inicio/completado con labels en negrita
        try {
            SpannableStringBuilder statesBuilder = new SpannableStringBuilder();
            String lblStart = getString(R.string.lbl_start);
            int ss = statesBuilder.length();
            statesBuilder.append(lblStart);
            statesBuilder.setSpan(new StyleSpan(Typeface.BOLD), ss, ss + lblStart.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            statesBuilder.append("\u00A0").append(startedAt).append("\n");

            String lblCompleted = getString(R.string.lbl_completed);
            ss = statesBuilder.length();
            statesBuilder.append(lblCompleted);
            statesBuilder.setSpan(new StyleSpan(Typeface.BOLD), ss, ss + lblCompleted.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            statesBuilder.append("\u00A0").append(completedAt);

            tvState.setText(statesBuilder);
            tvState.setVisibility(View.VISIBLE);
        } catch (Exception e) {
            tvState.setText(getString(R.string.lbl_start) + "\u00A0" + startedAt + "\n" + getString(R.string.lbl_completed) + "\u00A0" + completedAt);
            tvState.setVisibility(View.VISIBLE);
        }

        // Calcular y mostrar progreso según estado
        int percent = computeProgressPercentage(m);
        showProgressState(m.status, percent);

        // Mostrar XP con color y tamaño destacados
        try {
            // Mostrar como "100 XP" donde el número resalte mucho más
            String number = String.valueOf(m.baseXp);
            String xpText = number + " XP";
            SpannableString spannableString = new SpannableString(xpText);
            int numberColor = ContextCompat.getColor(this, R.color.legendme_teal);
            int labelColor = ContextCompat.getColor(this, R.color.azul_humo);
            // aplicar color y tamaño al número (posición 0..number.length())
            spannableString.setSpan(new ForegroundColorSpan(numberColor), 0, number.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            spannableString.setSpan(new RelativeSizeSpan(1.6f), 0, number.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            // aplicar color más tenue al " XP"
            spannableString.setSpan(new ForegroundColorSpan(labelColor), number.length(), xpText.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            tvXp.setText(spannableString);
            tvXp.setVisibility(View.VISIBLE);
            // aumentar tamaño base para que el número destaque (fallback si el span no aplica en algún caso)
            tvXp.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 24);
        } catch (Exception e) {
            Log.w(TAG, "No se pudo aplicar estilo a tvXp: " + e.getMessage());
            // usar string resource para formato de XP en fallback
            tvXp.setText(getString(R.string.xp_format, m.baseXp));
            tvXp.setVisibility(View.VISIBLE);
        }

        tvTitle.setVisibility(View.VISIBLE);
        tvDescription.setVisibility(View.VISIBLE);
        tvMeta.setVisibility(View.VISIBLE);
        tvDates.setVisibility(View.VISIBLE);
    }

    /**
     * Calcula un porcentaje (0-100) para la barra de progreso basado en el estado de la misión.
     * Reglas asumidas (si no hay un campo numérico explícito):
     * - COMPLETED -> 100
     * - CANCELLED, EXPIRED -> 0
     * - PENDING -> 0
     * - IN_PROGRESS/PAUSED -> si existen startedAt y dueAt, usa tiempo transcurrido / total; si no, valor por defecto 50
     *
     * Acepta tanto valores en español como en inglés y normaliza.
     */
    private int computeProgressPercentage(Mission m) {
        if (m == null) return 0;
        String raw = m.status == null ? "" : m.status.trim().toUpperCase(Locale.ROOT);

        // Normalizar textos comunes en español a claves en inglés
        if (raw.equals("PENDIENTE")) raw = "PENDING";
        else if (raw.equals("EN PROGRESO") || raw.equals("EN_PROGRESO") || raw.equals("ENPROGRESO")) raw = "IN_PROGRESS";
        else if (raw.equals("PAUSADA") || raw.equals("PAUSADO") ) raw = "PAUSED";
        else if (raw.equals("COMPLETADA") || raw.equals("COMPLETADO")) raw = "COMPLETED";
        else if (raw.equals("EXPIRADA") || raw.equals("EXPIRADO")) raw = "EXPIRED";
        else if (raw.equals("CANCELADA") || raw.equals("CANCELADO")) raw = "CANCELLED";

        switch (raw) {
            case "COMPLETED":
                return 100;
            case "CANCELLED":
            case "EXPIRED":
            case "PENDING":
                return 0;
            case "IN_PROGRESS":
                return 50;
            case "PAUSED":
                // intentar calcular basado en startedAt/dueAt
                if (m.startedAt != null && m.dueAt != null && !m.startedAt.isEmpty() && !m.dueAt.isEmpty()) {
                    try {
                        // El backend usa ISO sin zona: "yyyy-MM-dd'T'HH:mm:ss(.SSSSSS)"
                        SimpleDateFormat parser = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
                        Date started = null;
                        Date due = null;
                        try {
                            parser.applyPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS");
                            started = parser.parse(m.startedAt);
                        } catch (Exception ex) {
                            parser.applyPattern("yyyy-MM-dd'T'HH:mm:ss");
                            try { started = parser.parse(m.startedAt); } catch (Exception ignored) {}
                        }

                        try {
                            parser.applyPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS");
                            due = parser.parse(m.dueAt);
                        } catch (Exception ex) {
                            parser.applyPattern("yyyy-MM-dd'T'HH:mm:ss");
                            try { due = parser.parse(m.dueAt); } catch (Exception ignored) {}
                        }

                        if (started != null && due != null) {
                            long total = due.getTime() - started.getTime();
                            long elapsed = System.currentTimeMillis() - started.getTime();
                            if (total <= 0) return 0;
                            int pct = (int) Math.max(0, Math.min(100, (elapsed * 100) / total));
                            // si completado temporalmente pero status no es COMPLETED, cap en 99
                            if (pct >= 100) pct = 99;
                            return pct;
                        }
                    } catch (Exception e) {
                        Log.w(TAG, "Error calculando porcentaje desde fechas: " + e.getMessage());
                    }
                }
                // fallback
                return 50;
            default:
                // estado desconocido -> intentar usar m.status con contains
                if (raw.contains("COMPLETE") || raw.contains("COMPLET")) return 100;
                if (raw.contains("PROG")) return 50;
                return 0;
        }
    }

    private void showProgressState(String rawStatus, int percent) {
        // Normalizar para escoger color/etiqueta
        String label = rawStatus == null ? "Desconocido" : rawStatus;
        String key = label.trim().toUpperCase(Locale.ROOT);
        if (key.equals("PENDIENTE")) key = "PENDING";
        else if (key.equals("EN PROGRESO") || key.equals("EN_PROGRESO") || key.equals("ENPROGRESO")) key = "IN_PROGRESS";
        else if (key.equals("PAUSADA") || key.equals("PAUSADO")) key = "PAUSED";
        else if (key.equals("COMPLETADA") || key.equals("COMPLETADO")) key = "COMPLETED";
        else if (key.equals("EXPIRADA") || key.equals("EXPIRADO")) key = "EXPIRED";
        else if (key.equals("CANCELADA") || key.equals("CANCELADO")) key = "CANCELLED";

        // Mostrar chip
        if (chipStatus != null) {
            chipStatus.setText(label);
            chipStatus.setVisibility(View.VISIBLE);
            try {
                int bgColorRes = R.color.gris_neutro;
                switch (key) {
                    case "COMPLETED": bgColorRes = R.color.verde_exito; break;
                    case "IN_PROGRESS": bgColorRes = R.color.amarillo_resplandor; break;
                    case "PAUSED": bgColorRes = R.color.amarillo_resplandor; break;
                    case "PENDING": bgColorRes = R.color.azul_neblina; break;
                    case "CANCELLED": bgColorRes = R.color.rojo_alerta; break;
                    case "EXPIRED": bgColorRes = R.color.rojo_alerta; break;
                }
                int bgColor = ContextCompat.getColor(this, bgColorRes);
                chipStatus.setChipBackgroundColor(ColorStateList.valueOf(bgColor));
                // elegir color de texto según luminancia para buen contraste
                int r = (bgColor >> 16) & 0xff;
                int g = (bgColor >> 8) & 0xff;
                int b = (bgColor) & 0xff;
                double luminance = (0.2126 * r + 0.7152 * g + 0.0722 * b) / 255.0;
                int textColor = luminance > 0.6 ? Color.BLACK : ContextCompat.getColor(this, R.color.blanco_lunar);
                chipStatus.setTextColor(ColorStateList.valueOf(textColor));
                // Mantener stroke para elegancia
                chipStatus.setChipStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.azul_neblina)));
            } catch (Exception e) {
                Log.w(TAG, "No se pudo aplicar color al chip: " + e.getMessage());
            }
        }

        // Mostrar barra y porcentaje
        if (linearProgress != null && tvProgressPercent != null) {
            linearProgress.setVisibility(View.VISIBLE);
            linearProgress.setProgress(percent);
            tvProgressPercent.setVisibility(View.VISIBLE);
            try {
                tvProgressPercent.setText(getString(R.string.progress_percent, percent));
            } catch (Exception e) {
                tvProgressPercent.setText(percent + "%");
            }

            // Si está completado, usar color de éxito
            try {
                int colorRes = R.color.rojo_alerta;
                if (percent >= 100) colorRes = R.color.verde_exito;
                else if (key.equals("IN_PROGRESS") || key.equals("PAUSED")) colorRes = R.color.amarillo_resplandor;
                else if (key.equals("PENDING")) colorRes = R.color.azul_neblina;
                int color = ContextCompat.getColor(this, colorRes);
                linearProgress.setIndicatorColor(color);
            } catch (Exception e) {
                Log.w(TAG, "No se pudo aplicar color a linearProgress: " + e.getMessage());
            }
        }

        // Mostrar/ocultar botones de iniciar/pausar según estado
        try {
            if (btnStartMission != null && btnPauseMission != null) {
                switch (key) {
                    case "IN_PROGRESS":
                        btnStartMission.setVisibility(View.GONE);
                        btnPauseMission.setVisibility(View.VISIBLE);
                        btnPauseMission.setEnabled(true);
                        btnCancelMission.setVisibility(View.VISIBLE);
                        btnCancelMission.setEnabled(true);
                        break;
                    case "PAUSED":
                        btnStartMission.setVisibility(View.VISIBLE);
                        btnPauseMission.setVisibility(View.GONE);
                        btnStartMission.setEnabled(true);
                        btnCancelMission.setVisibility(View.VISIBLE);
                        btnCancelMission.setEnabled(true);
                        btnCompleteMission.setVisibility(View.GONE);
                        btnCompleteMission.setEnabled(false);
                        break;
                    case "PENDING":
                        btnStartMission.setVisibility(View.VISIBLE);
                        btnPauseMission.setVisibility(View.GONE);
                        btnStartMission.setEnabled(true);
                        btnCancelMission.setVisibility(View.VISIBLE);
                        btnCancelMission.setEnabled(true);
                        btnCompleteMission.setVisibility(View.VISIBLE);
                        btnCompleteMission.setEnabled(false);
                        break;
                    case "COMPLETED":
                        btnCompleteMission.setVisibility(View.GONE);
                        btnCompleteMission.setEnabled(false);
                    case "CANCELLED":
                        btnCompleteMission.setVisibility(View.GONE);
                        btnCompleteMission.setEnabled(false);
                        break;
                    case "EXPIRED":
                        btnStartMission.setVisibility(View.GONE);
                        btnPauseMission.setVisibility(View.GONE);
                        btnCompleteMission.setVisibility(View.GONE);
                        btnCompleteMission.setEnabled(false);
                        break;
                    default:
                        btnStartMission.setVisibility(View.VISIBLE);
                        btnPauseMission.setVisibility(View.GONE);
                        btnCancelMission.setVisibility(View.VISIBLE);
                        btnCancelMission.setEnabled(true);
                        btnCompleteMission.setVisibility(View.GONE);
                        btnCompleteMission.setEnabled(false);
                        break;
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "No se pudo ajustar visibilidad de botones: " + e.getMessage());
        }

        // Mostrar/ocultar botón de opciones según si hay acciones disponibles
        if (btnOptions != null) {
            boolean hasActions = (btnStartMission != null && btnStartMission.getVisibility() == View.VISIBLE) ||
                    (btnPauseMission != null && btnPauseMission.getVisibility() == View.VISIBLE) ||
                    (btnCompleteMission != null && btnCompleteMission.getVisibility() == View.VISIBLE) ||
                    (btnCancelMission != null && btnCancelMission.getVisibility() == View.VISIBLE) ||
                    (btnEditMission != null);
            btnOptions.setVisibility(hasActions ? View.VISIBLE : View.GONE);
        }
    }

    // Lanza la petición PATCH /missions/{id}/start y muestra un Toast "mision iniciada" en éxito
    private void performStartMission() {
        if (missionId == null || missionId.isEmpty() || accessToken == null || accessToken.isEmpty()) {
            Toast.makeText(this, "Datos de misión o token no disponibles", Toast.LENGTH_SHORT).show();
            if (btnStartMission != null) btnStartMission.setEnabled(true);
            if (fabStartMission != null) fabStartMission.setEnabled(true);
            return;
        }

        MissionService svc = ApiClient.missionsRetrofit().create(MissionService.class);
        // enviar un body vacío -> Gson lo serializará como {}
        svc.startMission("Bearer " + accessToken, missionId, new Object()).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (btnStartMission != null) btnStartMission.setEnabled(true);
                if (fabStartMission != null) fabStartMission.setEnabled(true);
                if (response.isSuccessful()) {
                    Toast.makeText(MissionDetailActivity.this, "mision iniciada", Toast.LENGTH_SHORT).show();
                    // recargar la misión para actualizar UI
                    loadMission(missionId, accessToken);
                } else {
                    Log.w(TAG, "startMission no exitoso, code=" + response.code());
                    Toast.makeText(MissionDetailActivity.this, "No se pudo iniciar la misión", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                if (btnStartMission != null) btnStartMission.setEnabled(true);
                if (fabStartMission != null) fabStartMission.setEnabled(true);
                Log.e(TAG, "Error al iniciar misión", t);
                Toast.makeText(MissionDetailActivity.this, "Error al iniciar la misión", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Lanza la petición PATCH /missions/{id}/pause y muestra un Toast "mision pausada" en éxito
    private void performPauseMission() {
        if (missionId == null || missionId.isEmpty() || accessToken == null || accessToken.isEmpty()) {
            Toast.makeText(this, "Datos de misión o token no disponibles", Toast.LENGTH_SHORT).show();
            if (btnStartMission != null) btnStartMission.setEnabled(true);
            if (btnPauseMission != null) btnPauseMission.setEnabled(true);
            if (fabStartMission != null) fabStartMission.setEnabled(true);
            return;
        }

        MissionService svc = ApiClient.missionsRetrofit().create(MissionService.class);
        svc.pauseMission("Bearer " + accessToken, missionId, new Object()).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (btnStartMission != null) btnStartMission.setEnabled(true);
                if (btnPauseMission != null) btnPauseMission.setEnabled(true);
                if (fabStartMission != null) fabStartMission.setEnabled(true);
                if (response.isSuccessful()) {
                    Toast.makeText(MissionDetailActivity.this, "mision pausada", Toast.LENGTH_SHORT).show();
                    // recargar la misión para actualizar UI
                    loadMission(missionId, accessToken);
                } else {
                    Log.w(TAG, "pauseMission no exitoso, code=" + response.code());
                    Toast.makeText(MissionDetailActivity.this, "No se pudo pausar la misión", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                if (btnStartMission != null) btnStartMission.setEnabled(true);
                if (btnPauseMission != null) btnPauseMission.setEnabled(true);
                if (fabStartMission != null) fabStartMission.setEnabled(true);
                Log.e(TAG, "Error al pausar misión", t);
                Toast.makeText(MissionDetailActivity.this, "Error al pausar la misión", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void performCancelMission() {
        if (missionId == null || missionId.isEmpty() || accessToken == null || accessToken.isEmpty()) {
            Toast.makeText(this, "Datos de misión o token no disponibles", Toast.LENGTH_SHORT).show();
            if (btnCancelMission != null) btnCancelMission.setEnabled(true);
            return;
        }

        MissionService svc = ApiClient.missionsRetrofit().create(MissionService.class);
        svc.cancelMission("Bearer " + accessToken, missionId, new Object()).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (btnCancelMission != null) btnCancelMission.setEnabled(true);
                if (response.isSuccessful()) {
                    Toast.makeText(MissionDetailActivity.this, "Misión cancelada", Toast.LENGTH_SHORT).show();
                    loadMission(missionId, accessToken); // recarga el estado
                } else {
                    Log.w(TAG, "cancelMission no exitoso, code=" + response.code());
                    Toast.makeText(MissionDetailActivity.this, "No se pudo cancelar la misión", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                if (btnCancelMission != null) btnCancelMission.setEnabled(true);
                Log.e(TAG, "Error al cancelar misión", t);
                Toast.makeText(MissionDetailActivity.this, "Error al cancelar la misión", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void performCompleteMission() {
        if (missionId == null || missionId.isEmpty() || accessToken == null || accessToken.isEmpty()) {
            Toast.makeText(this, "Datos de misión o token no disponibles", Toast.LENGTH_SHORT).show();
            if (btnCompleteMission != null) btnCompleteMission.setEnabled(true);
            return;
        }

        // Generar Idempotency-Key único para cada llamada
        String idempotencyKey = UUID.randomUUID().toString();

        MissionService svc = ApiClient.missionsRetrofit().create(MissionService.class);
        svc.completeMission("Bearer " + accessToken, idempotencyKey, missionId, new Object())
                .enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        if (btnCompleteMission != null) btnCompleteMission.setEnabled(true);
                        if (response.isSuccessful()) {
                            Toast.makeText(MissionDetailActivity.this, "✅ Misión completada con éxito", Toast.LENGTH_SHORT).show();
                            loadMission(missionId, accessToken);
                        } else {
                            Log.w(TAG, "completeMission no exitoso, code=" + response.code());
                            Toast.makeText(MissionDetailActivity.this, "No se pudo completar la misión", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {
                        if (btnCompleteMission != null) btnCompleteMission.setEnabled(true);
                        Log.e(TAG, "Error al completar misión", t);
                        Toast.makeText(MissionDetailActivity.this, "Error al completar la misión", Toast.LENGTH_SHORT).show();
                    }
                });
    }


    private String formatIsoDate(String isoString) {
        try {
            SimpleDateFormat parser = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
            Date date;

            try {
                parser.applyPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS");
                date = parser.parse(isoString);
            } catch (Exception e) {
                parser.applyPattern("yyyy-MM-dd'T'HH:mm:ss");
                date = parser.parse(isoString);
            }

            SimpleDateFormat formatter = new SimpleDateFormat("HH:mm dd/MM/yyyy", Locale.getDefault());
            return formatter.format(date);
        } catch (Exception ex) {
            Log.w(TAG, "Error formateando fecha: " + ex.getMessage());
            return "Desconocida";
        }
    }

    private static String safe(String s) { return s == null ? "" : s; }
}

