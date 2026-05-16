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
import com.wifibattleship.databinding.FragmentResultBinding;

public class ResultFragment extends Fragment {

    private FragmentResultBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentResultBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        String winner = ResultFragmentArgs.fromBundle(getArguments()).getWinner();
        binding.tvResult.setText("LOCAL".equals(winner) ? "¡Ganaste!" : "Perdiste");

        binding.btnPlayAgain.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.action_resultFragment_to_menuFragment));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
