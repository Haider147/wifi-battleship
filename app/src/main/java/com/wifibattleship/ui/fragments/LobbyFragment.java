package com.wifibattleship.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import com.wifibattleship.R;
import com.wifibattleship.databinding.FragmentLobbyBinding;
import com.wifibattleship.ui.viewmodels.LobbyViewModel;

public class LobbyFragment extends Fragment {

    private FragmentLobbyBinding binding;
    private LobbyViewModel viewModel;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentLobbyBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        String connectionType = LobbyFragmentArgs.fromBundle(getArguments()).getConnectionType();
        binding.tvConnectionType.setText(connectionType);

        viewModel = new ViewModelProvider(this).get(LobbyViewModel.class);

        binding.btnDiscover.setOnClickListener(v -> viewModel.startDiscovery(connectionType));

        viewModel.getConnectionReady().observe(getViewLifecycleOwner(), ready -> {
            if (Boolean.TRUE.equals(ready)) {
                Navigation.findNavController(view)
                        .navigate(R.id.action_lobbyFragment_to_shipPlacementFragment);
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
