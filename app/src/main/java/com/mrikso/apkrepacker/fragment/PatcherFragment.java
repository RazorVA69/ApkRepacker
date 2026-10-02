package com.mrikso.apkrepacker.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.mrikso.apkrepacker.R;
import com.mrikso.apkrepacker.adapter.PatchAdapter;
import com.mrikso.apkrepacker.adapter.PatchItem;
import com.mrikso.apkrepacker.filepicker.FilePickerDialog;
import com.mrikso.apkrepacker.utils.FileUtil;
import com.mrikso.apkrepacker.utils.FragmentUtils;
import com.mrikso.apkrepacker.viewmodel.PatcherViewModel;

import java.io.File;

public class PatcherFragment extends Fragment implements View.OnClickListener, OnBackPressedListener {

    public static final String TAG = "PatcherFragment";

    private PatcherViewModel mViewModel;
    private PatchAdapter mPathAdapter;
    private MaterialButton mStartPatch;
    private TextView mPatchCount;

    public static PatcherFragment newInstance() {
        return new PatcherFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_patcher, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mViewModel = new ViewModelProvider(requireActivity()).get(PatcherViewModel.class);

        Toolbar toolbar = view.findViewById(R.id.toolbar);
        MaterialButton selectPatch = view.findViewById(R.id.btn_select_patch);
        mStartPatch = view.findViewById(R.id.start_patch);
        mPatchCount = view.findViewById(R.id.patch_count);

        selectPatch.setOnClickListener(this);
        mStartPatch.setOnClickListener(this);

        mPathAdapter = new PatchAdapter(requireContext());
        RecyclerView patchList = view.findViewById(R.id.patch_list);
        patchList.setLayoutManager(new LinearLayoutManager(requireContext()));
        patchList.setAdapter(mPathAdapter);

        toolbar.setNavigationOnClickListener(v -> FragmentUtils.remove(this));
        updateActionState();
    }

    private void selectPatch() {
        new FilePickerDialog(requireContext())
                .setTitleText(getString(R.string.select_patch))
                .setSelectMode(FilePickerDialog.MODE_MULTI)
                .setSelectType(FilePickerDialog.TYPE_FILE)
                .setExtensions(new String[]{"zip"})
                .setRootDir(FileUtil.getInternalStorage().getAbsolutePath())
                .setBackCancelable(true)
                .setOutsideCancelable(true)
                .setDialogListener(getString(R.string.choose_button_label), getString(R.string.cancel_button_label), new FilePickerDialog.FileDialogListener() {
                    @Override
                    public void onSelectedFilePaths(String[] filePaths) {
                        for (String file : filePaths) {
                            File patch = new File(file);
                            mPathAdapter.addItem(new PatchItem(patch.getName(), patch.getAbsolutePath()));
                        }
                        updateActionState();
                    }

                    @Override
                    public void onCanceled() {
                    }
                })
                .show();
    }

    private void updateActionState() {
        if (mStartPatch == null || mPatchCount == null || mPathAdapter == null) return;
        int count = mPathAdapter.getItemCount();
        mStartPatch.setVisibility(count > 0 ? View.VISIBLE : View.GONE);
        if (count == 0) {
            mPatchCount.setText(R.string.no_patches_added);
        } else {
            String suffix = count == 1 ? "" : "s";
            mPatchCount.setText(getString(R.string.patch_count_format, count, suffix));
        }
    }

    private void openPatchLogs() {
        FragmentUtils.add(PatchLogFragment.newInstance(), requireActivity().getSupportFragmentManager(),
                android.R.id.content, PatchLogFragment.TAG);
        mViewModel.start(mPathAdapter.getPatchData());
    }

    @Override
    public void onBackPressed() {
        FragmentUtils.remove(this);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btn_select_patch) {
            selectPatch();
        } else if (v.getId() == R.id.start_patch && mPathAdapter.getItemCount() > 0) {
            openPatchLogs();
        }
    }
}
