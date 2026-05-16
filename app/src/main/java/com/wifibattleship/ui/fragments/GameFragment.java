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
import com.wifibattleship.databinding.FragmentGameBinding;
import com.wifibattleship.ui.viewmodels.GameViewModel;

public class GameFragment extends Fragment {

    private FragmentGameBinding binding;
    private GameViewModel gameViewModel;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentGameBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        gameViewModel = new ViewModelProvider(requireActivity()).get(GameViewModel.class);

        // TODO: render own board (fl_own_board) y enemy board (fl_enemy_board) como custom GridViews
        // fl_enemy_board solo acepta clicks cuando isMyTurn == true

        gameViewModel.getIsMyTurn().observe(getViewLifecycleOwner(), myTurn ->
                binding.tvTurn.setText(Boolean.TRUE.equals(myTurn) ? "Tu turno" : "Turno del enemigo"));

        gameViewModel.getGameOver().observe(getViewLifecycleOwner(), winner -> {
            if (winner != null) {
                GameFragmentDirections.ActionGameFragmentToResultFragment action =
                        GameFragmentDirections.actionGameFragmentToResultFragment(winner);
                Navigation.findNavController(view).navigate(action);
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
