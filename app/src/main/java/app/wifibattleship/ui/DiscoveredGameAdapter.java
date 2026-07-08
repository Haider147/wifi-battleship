package app.wifibattleship.ui;

import android.net.nsd.NsdServiceInfo;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import app.wifibattleship.R;

public class DiscoveredGameAdapter extends RecyclerView.Adapter<DiscoveredGameAdapter.VH> {

    public interface OnGameClickListener {
        void onGameClick(NsdServiceInfo info);
    }

    private final List<NsdServiceInfo> items = new ArrayList<>();
    private final OnGameClickListener listener;

    public DiscoveredGameAdapter(OnGameClickListener listener) {
        this.listener = listener;
    }

    public void add(NsdServiceInfo info) {
        for (NsdServiceInfo existing : items) {
            if (existing.getServiceName().equals(info.getServiceName())) {
                return;
            }
        }
        items.add(info);
        notifyItemInserted(items.size() - 1);
    }

    public void remove(NsdServiceInfo info) {
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).getServiceName().equals(info.getServiceName())) {
                items.remove(i);
                notifyItemRemoved(i);
                return;
            }
        }
    }

    public void clear() {
        items.clear();
        notifyDataSetChanged();
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
        NsdServiceInfo info = items.get(position);
        holder.tvName.setText(info.getServiceName());
        holder.tvHost.setText(info.getHost() != null ? info.getHost().getHostAddress() : "…");
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onGameClick(info);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
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
