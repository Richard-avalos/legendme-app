package com.legendme.app.ui.login;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.legendme.app.R;
import com.legendme.app.domain.model.Mission;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MissionsAdapter extends RecyclerView.Adapter<MissionsAdapter.VH> {

    private final List<Mission> data = new ArrayList<>();

    public void setItems(List<Mission> items) {
        int oldSize = data.size();
        data.clear();
        if (items != null) data.addAll(items);
        int newSize = data.size();
        // Notificaciones más específicas para reducir trabajo innecesario en el RecyclerView
        if (oldSize == 0 && newSize > 0) {
            notifyItemRangeInserted(0, newSize);
        } else if (newSize == 0 && oldSize > 0) {
            notifyItemRangeRemoved(0, oldSize);
        } else {
            notifyDataSetChanged();
        }
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_mission, parent, false);
        return new VH(v);
    }

    @Override public void onBindViewHolder(@NonNull VH h, int pos) {
        Mission m = data.get(pos);
        // m proviene de la lista interna: asumimos no-nulo
        h.tvTitle.setText(m.title);
        h.tvDescription.setText(m.description);

        final android.content.Context ctx = h.chipXp.getContext();

        // XP: texto con sufijo ' XP' o placeholder
        String xpFmt = ctx.getString(R.string.xp_format);
        String xpText = (m.baseXp > 0) ? String.format(Locale.getDefault(), xpFmt, m.baseXp) : ctx.getString(R.string.placeholder_dash);
        h.chipXp.setText(xpText);

        // Difficulty
        String diff = (m.difficulty != null) ? m.difficulty.trim() : "";
        boolean diffIsPlaceholder = isPlaceholder(diff);
        if (diffIsPlaceholder) {
            h.chipDifficulty.setText(ctx.getString(R.string.placeholder_dash));
            h.chipDifficulty.setAlpha(0.9f);
        } else {
            h.chipDifficulty.setText(diff);
            h.chipDifficulty.setAlpha(1f);
        }

        // Status
        String status = (m.status != null) ? m.status.trim() : "";
        boolean statusIsPlaceholder = isPlaceholder(status);
        if (statusIsPlaceholder) {
            h.chipStatus.setText(ctx.getString(R.string.placeholder_dash));
            h.chipStatus.setAlpha(0.9f);
        } else {
            h.chipStatus.setText(status);
            h.chipStatus.setAlpha(1f);
        }

        // Click: abrir detalle pasando el id (usamos setClassName para evitar dependencia directa)
        h.itemView.setOnClickListener(v -> {
            if (m.id != null) {
                Intent i = new Intent();
                i.setClassName(v.getContext(), "com.legendme.app.ui.mission.MissionDetailActivity");
                i.putExtra("extra_mission_id", m.id);
                v.getContext().startActivity(i);
            }
        });
    }

    // Considera varios placeholders que se usan como marcadores vacíos
    private static boolean isPlaceholder(String s) {
        if (s == null) return true;
        String t = s.trim();
        if (t.isEmpty()) return true;
        String lower = t.toLowerCase();
        return lower.equals("x") || lower.equals("✖") || lower.equals("×") || lower.equals("-") || lower.equals("na") || lower.equals("n/a") || lower.equals("?") || lower.equals("none") || lower.equals("no") || lower.equals("n.a");
    }

    @Override public int getItemCount() { return data.size(); }

    public static class VH extends RecyclerView.ViewHolder {
        TextView tvTitle, tvDescription;
        Chip chipXp, chipDifficulty, chipStatus;
        ImageView ivIcon;
        VH(@NonNull View v) {
            super(v);
            ivIcon = v.findViewById(R.id.ivIcon);
            tvTitle = v.findViewById(R.id.tvTitle);
            tvDescription = v.findViewById(R.id.tvDescription);
            chipXp = v.findViewById(R.id.chipXp);
            chipDifficulty = v.findViewById(R.id.chipDifficulty);
            chipStatus = v.findViewById(R.id.chipStatus);
        }
    }
}
