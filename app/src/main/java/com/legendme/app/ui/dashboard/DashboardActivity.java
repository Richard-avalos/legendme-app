package com.legendme.app.ui.dashboard;

import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.legendme.app.R;
import com.legendme.app.domain.model.Mission;
import com.legendme.app.domain.model.UserDetail;
import com.legendme.app.network.ApiClient;
import com.legendme.app.network.MissionService;
import com.legendme.app.network.UserService;
import com.legendme.app.ui.VerticalSpaceItemDecoration;
import com.legendme.app.ui.login.MissionsAdapter;

import java.util.Collections;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DashboardActivity extends AppCompatActivity {

    private MissionsAdapter adapter;
    private MaterialToolbar toolbar;
    private RecyclerView rv; // <-- campo (no lo sombres)

    private SharedPreferences prefs;
    private static final String PREFS = "legendme_auth";
    private static final String INTERNAL_TOKEN = "bGVnZW5kbWUtYmFja2VuZC1taWNyb3NlcnZpY2Vz";
    private static final int REQUEST_CREATE = 1001;

    @Override
    protected void onCreate(@Nullable Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_dashboard);

        // ------- UI -------
        toolbar = findViewById(R.id.toolbar);
        // Evita crash si el tema ya provee ActionBar: comprueba windowActionBar
        TypedArray ta = obtainStyledAttributes(new int[]{android.R.attr.windowActionBar});
        boolean hasWindowActionBar = ta.getBoolean(0, false);
        ta.recycle();
        if (!hasWindowActionBar) {
            setSupportActionBar(toolbar);
        }
        toolbar.setOnMenuItemClickListener(this::onMenuItemClick);

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        // Coloca un icono de navegación (usa un drawable del sistema para evitar dependencias)
        toolbar.setNavigationIcon(android.R.drawable.ic_menu_sort_by_size);
        toolbar.setNavigationOnClickListener(v -> {
            if (drawer != null) {
                if (drawer.isDrawerOpen(GravityCompat.START)) {
                    drawer.closeDrawer(GravityCompat.START);
                } else {
                    drawer.openDrawer(GravityCompat.START);
                }
            }
        });

        NavigationView nav = findViewById(R.id.nav_view);
        if (nav != null) {
            nav.setNavigationItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.nav_profile) {
                    Toast.makeText(DashboardActivity.this, "Perfil", Toast.LENGTH_SHORT).show();
                } else if (id == R.id.nav_settings) {
                    Toast.makeText(DashboardActivity.this, "Ajustes", Toast.LENGTH_SHORT).show();
                } else if (id == R.id.nav_logout) {
                    Toast.makeText(DashboardActivity.this, "Cerrar sesión", Toast.LENGTH_SHORT).show();
                }
                drawer.closeDrawer(GravityCompat.START);
                return true;
            });
        }

        rv = findViewById(R.id.rvMissions);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.addItemDecoration(new VerticalSpaceItemDecoration(
                getResources().getDimensionPixelSize(R.dimen.mission_item_spacing)
        ));

        // Adapter SIEMPRE primero
        adapter = new MissionsAdapter();
        rv.setAdapter(adapter);

        // ------- Datos / Prefs -------
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);

        // Título inicial con prefs (evita NPE)
        String name = safe(prefs.getString("name", "Usuario"));
        String lastname = safe(prefs.getString("lastname", ""));
        setTitle((name + " " + lastname).trim());

        // 1) Usuario (actualiza título si llega)
        fetchUserDetail(prefs.getString("userId", null));

        // 2) Misiones
        String accessToken = prefs.getString("accessToken", null);
        fetchMissions(accessToken);

        // 3) FloatingActionButton: crear misión
        FloatingActionButton fab = findViewById(R.id.fab_create_mission);
        if (fab != null) {
            fab.setOnClickListener(v -> {
                // Abre la Activity de creación en lugar de enviar datos fijos
                startActivityForResult(
                        new android.content.Intent(DashboardActivity.this, com.legendme.app.ui.mission.MissionCreateActivity.class),
                        REQUEST_CREATE
                );
            });
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable android.content.Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CREATE && resultCode == RESULT_OK) {
            // refresca misiones
            String accessToken = prefs.getString("accessToken", null);
            fetchMissions(accessToken);
        }
    }

    private boolean onMenuItemClick(MenuItem item) {
        if (item.getItemId() == R.id.action_menu) {
            // TODO abrir opciones
            return true;
        }
        return false;
    }

    private void fetchUserDetail(String userId) {
        if (userId == null) return;

        UserService svc = ApiClient.usersRetrofit().create(UserService.class);
        svc.getById(userId, INTERNAL_TOKEN).enqueue(new Callback<UserDetail>() {
            @Override public void onResponse(Call<UserDetail> call, Response<UserDetail> resp) {
                Log.d("DASH", "user code=" + resp.code());
                if (resp.isSuccessful() && resp.body() != null) {
                    UserDetail u = resp.body();
                    String full = (safe(u.name) + " " + safe(u.lastname)).trim();
                    if (!full.isEmpty() && getSupportActionBar() != null) {
                        getSupportActionBar().setTitle(full);
                    }
                    prefs.edit()
                            .putString("name", safe(u.name))
                            .putString("lastname", safe(u.lastname))
                            .apply();
                }
            }
            @Override public void onFailure(Call<UserDetail> call, Throwable t) { /* log */ }
        });
    }

    private void fetchMissions(String accessToken) {
        if (accessToken == null) {
            adapter.setItems(Collections.emptyList());
            return;
        }
        MissionService svc = ApiClient.missionsRetrofit().create(MissionService.class);
        RequestBody emptyBody = RequestBody.create(new byte[]{}, MediaType.parse("application/json"));

        svc.queryMissions("Bearer " + accessToken, emptyBody).enqueue(new Callback<List<Mission>>() {
            @Override public void onResponse(Call<List<Mission>> call, Response<List<Mission>> resp) {
                if (resp.isSuccessful() && resp.body() != null) {
                    Log.i("DASH", "Misiones recibidas " + resp.body());
                    adapter.setItems(resp.body()); // adapter NO es null
                } else {
                    adapter.setItems(Collections.emptyList());
                }
            }
            @Override public void onFailure(Call<List<Mission>> call, Throwable t) {
                adapter.setItems(Collections.emptyList());
                Toast.makeText(DashboardActivity.this, "Error cargando misiones", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private static String safe(String s) { return s == null ? "" : s; }
}
