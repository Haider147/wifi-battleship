package app.wifibattleship.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import app.wifibattleship.R;
import app.wifibattleship.net.DiscoveredGame;

public class DiscoveredGameAdapter extends RecyclerView.Adapter<DiscoveredGameAdapter.VH> {

    public interface OnGameClickListener {
        void onGameClick(DiscoveredGame game);
    }

    private final List<DiscoveredGame> items = new ArrayList<>();
    private final OnGameClickListener listener;

    public DiscoveredGameAdapter(OnGameClickListener listener) {
        this.listener = listener;
    }

    public void add(DiscoveredGame game) {
        for (int i = 0; i < items.size(); i++) {
            DiscoveredGame existing = items.get(i);
            if (existing.getDeviceAddress().equals(game.getDeviceAddress())) {
                items.set(i, game);
                notifyItemChanged(i);
                return;
            }
        }
        items.add(game);
        notifyItemInserted(items.size() - 1);
    }

    public void clear() {
        int size = items.size();
        items.clear();
        notifyItemRangeRemoved(0, size);
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_discovered_game, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        DiscoveredGame game = items.get(position);
        holder.tvName.setText(game.getName());
        holder.tvHost.setText(game.getDeviceName());
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onGameClick(game);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    @Override
    public long getItemId(int position) {
        return items.get(position).getDeviceAddress().hashCode();
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView tvName;
        final TextView tvHost;

        VH(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvServiceName);
            tvHost = itemView.findViewById(R.id.tvServiceHost);
        }
    }
}
