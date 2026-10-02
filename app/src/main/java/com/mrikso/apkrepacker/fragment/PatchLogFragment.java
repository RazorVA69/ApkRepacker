package com.mrikso.apkrepacker.fragment;

import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.mrikso.apkrepacker.R;
import com.mrikso.apkrepacker.utils.FragmentUtils;
import com.mrikso.apkrepacker.viewmodel.PatcherViewModel;

public class PatchLogFragment extends Fragment implements OnBackPressedListener {

    public static final String TAG = "PatchLogFragment";

    private PatcherViewModel mViewModel;
    private AppCompatTextView mLogger;
    private ProgressBar mRulesProgress;

    public static PatchLogFragment newInstance() {
        return new PatchLogFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_patch_log, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mViewModel = new ViewModelProvider(requireActivity()).get(PatcherViewModel.class);

        Toolbar toolbar = view.findViewById(R.id.toolbar);
        mLogger = view.findViewById(R.id.logger);
        mRulesProgress = view.findViewById(R.id.progress_bar_rules);
        mLogger.setMovementMethod(new ScrollingMovementMethod());

        mViewModel.getLogLiveData().observe(getViewLifecycleOwner(), text -> {
            mLogger.setText(text == null ? "" : text);
            mLogger.post(() -> mLogger.scrollTo(0, mLogger.getBottom()));
        });
        mViewModel.getPatchRulesSize().observe(getViewLifecycleOwner(), value -> {
            if (value != null) mRulesProgress.setMax(Math.max(value, 1));
        });
        mViewModel.getPatchCurrentRules().observe(getViewLifecycleOwner(), value -> {
            if (value != null) mRulesProgress.setProgress(value);
        });

        toolbar.setNavigationOnClickListener(v -> FragmentUtils.remove(this));
    }

    @Override
    public void onBackPressed() {
        FragmentUtils.remove(this);
    }
}
