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
import com.wifibattleship.databinding.FragmentShipPlacementBinding;
import com.wifibattleship.ui.viewmodels.GameViewModel;

public class ShipPlacementFragment extends Fragment {

    private FragmentShipPlacementBinding binding;
    private GameViewModel gameViewModel;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentShipPlacementBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        gameViewModel = new ViewModelProvider(requireActivity()).get(GameViewModel.class);

        // TODO: render board grid and ship list in fl_board / rv_ships

        binding.btnRotate.setOnClickListener(v -> {
            // TODO: toggle selected ship orientation
        });

        binding.btnReady.setOnClickListener(v -> {
            // TODO: validate all ships placed, send SHIP_PLACEMENT_READY via network
            Navigation.findNavController(view)
                    .navigate(R.id.action_shipPlacementFragment_to_gameFragment);
        });

        gameViewModel.getBothPlayersReady().observe(getViewLifecycleOwner(), ready -> {
            if (Boolean.TRUE.equals(ready)) {
                Navigation.findNavController(view)
                        .navigate(R.id.action_shipPlacementFragment_to_gameFragment);
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
