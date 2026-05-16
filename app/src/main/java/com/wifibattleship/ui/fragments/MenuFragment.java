package com.wifibattleship.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import com.wifibattleship.R;
import com.wifibattleship.databinding.FragmentMenuBinding;

public class MenuFragment extends Fragment {

    private FragmentMenuBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentMenuBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.btnWifi.setOnClickListener(v -> {
            MenuFragmentDirections.ActionMenuFragmentToLobbyFragment action =
                    MenuFragmentDirections.actionMenuFragmentToLobbyFragment("WIFI");
            Navigation.findNavController(v).navigate(action);
        });

        binding.btnBluetooth.setOnClickListener(v -> {
            MenuFragmentDirections.ActionMenuFragmentToLobbyFragment action =
                    MenuFragmentDirections.actionMenuFragmentToLobbyFragment("BLUETOOTH");
            Navigation.findNavController(v).navigate(action);
        });

        binding.btnSettings.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.action_menuFragment_to_settingsFragment));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
