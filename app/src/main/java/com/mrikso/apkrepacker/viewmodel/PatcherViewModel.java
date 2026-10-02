package com.mrikso.apkrepacker.viewmodel;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.mrikso.apkrepacker.adapter.PatchItem;
import com.mrikso.apkrepacker.utils.ProjectUtils;
import com.mrikso.apkrepacker.utils.common.DLog;
import com.mrikso.apkrepacker.utils.manifestparser.SdkConstants;
import com.mrikso.apkrepacker.utils.manifestparser.xml.AndroidManifestParser;
import com.mrikso.apkrepacker.utils.manifestparser.xml.ManifestData;
import com.mrikso.patchengine.PatchExecutor;
import com.mrikso.patchengine.ProjectHelper;
import com.mrikso.patchengine.interfaces.IPatchContext;
import com.mrikso.patchengine.interfaces.IRulesInfo;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class PatcherViewModel extends AndroidViewModel implements IRulesInfo, IPatchContext {

    private final Map<String, String> mGlobalVariables = new HashMap<>();
    private final StringBuilder mLogText = new StringBuilder();
    private PatchExecutor mPatchExecutor;
    private final ProjectHelper mProjectHelper;
    private final MutableLiveData<String> mLog = new MutableLiveData<>("");
    private final MutableLiveData<Integer> mPatchCount = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> mPatchSize = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> mPatchCurrentCountRules = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> mPatchAllRulesSize = new MutableLiveData<>(0);
    @SuppressLint("StaticFieldLeak")
    private final Context mContext;

    public PatcherViewModel(@NonNull Application application) {
        super(application);
        mContext = application.getApplicationContext();
        mProjectHelper = new ProjectHelper();
        mProjectHelper.mContext = mContext;
        mProjectHelper.mProject = ProjectUtils.getProjectPath();
        mProjectHelper.mDataPath = mContext.getFilesDir().getPath();
        mProjectHelper.mCache = mContext.getExternalCacheDir();
    }

    public LiveData<String> getLogLiveData() { return mLog; }
    public LiveData<Integer> getPathSize() { return mPatchSize; }
    public LiveData<Integer> getPatchCount() { return mPatchCount; }
    public LiveData<Integer> getPatchRulesSize() { return mPatchAllRulesSize; }
    public LiveData<Integer> getPatchCurrentRules() { return mPatchCurrentCountRules; }

    public void clearLog() {
        synchronized (mLogText) {
            mLogText.setLength(0);
            mLog.postValue("");
        }
    }

    private void runPatch(String path) {
        mPatchExecutor = new PatchExecutor(mProjectHelper, path, this, this);
        mPatchExecutor.applyPatch();
    }

    public void start(List<PatchItem> patchItemList) {
        clearLog();
        mPatchSize.postValue(patchItemList.size());
        mPatchCurrentCountRules.postValue(0);
        int count = 0;
        for (PatchItem item : patchItemList) {
            count++;
            runPatch(item.mPath);
        }
        mPatchCount.postValue(count);
    }

    @Nullable
    @Override
    public List<String> getActivities() {
        try {
            List<String> act = new ArrayList<>();
            ManifestData manifestData = AndroidManifestParser.parse(new File(getDecodeRootPath() + "/" + SdkConstants.FN_ANDROID_MANIFEST_XML));
            for (ManifestData.Activity activity : manifestData.getActivities()) act.add(activity.getName());
            return act;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Nullable
    @Override
    public String getApplicationManifest() {
        try {
            ManifestData manifestData = AndroidManifestParser.parse(new File(getDecodeRootPath() + "/" + SdkConstants.FN_ANDROID_MANIFEST_XML));
            return manifestData.getPackage();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Nullable
    @Override
    public String getDecodeRootPath() { return ProjectUtils.getProjectPath(); }

    @Nullable
    @Override
    public List<String> getLauncherActivities() {
        try {
            List<String> act = new ArrayList<>();
            ManifestData manifestData = AndroidManifestParser.parse(new File(getDecodeRootPath() + "/" + SdkConstants.FN_ANDROID_MANIFEST_XML));
            DLog.d("LauncherActivity : " + manifestData.getLauncherActivity().getName());
            act.add(manifestData.getLauncherActivity().getName());
            return act;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Nullable
    @Override
    public List<String> getPatchNames() {
        return mPatchExecutor == null ? null : mPatchExecutor.getRuleNames();
    }

    @Nullable
    @Override
    public List<String> getSmaliFolders() {
        List<String> folders = new ArrayList<>();
        folders.add("smali");
        try {
            ZipFile zipfile = new ZipFile(mProjectHelper.getApkPath());
            Enumeration<? extends ZipEntry> entries = zipfile.entries();
            while (entries.hasMoreElements()) {
                String name = entries.nextElement().getName();
                if (name.endsWith(".dex") && !name.contains("/") && !name.equals("classes.dex")) {
                    folders.add("smali_" + name.substring(0, name.length() - 4));
                }
            }
            zipfile.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return folders;
    }

    @Nullable
    @Override
    public String getVariableValue(@Nullable String key) { return mGlobalVariables.get(key); }

    @Override
    public void error(int resourceId, Object... args) {
        String txt = mContext.getString(resourceId);
        if (args != null) txt = String.format(txt, args);
        appendText(txt + "\n");
    }

    private void appendText(String txt) {
        DLog.d("PatcherViewModel", txt);
        synchronized (mLogText) {
            mLogText.append(txt);
            mLog.postValue(mLogText.toString());
        }
    }

    @Override
    public void info(int resourceId, boolean bold, Object... args) {
        String txt = mContext.getString(resourceId);
        if (args != null) txt = String.format(txt, args);
        appendText((bold ? "\n" : "") + txt + "\n");
    }

    @Override
    public void info(String format, boolean bold, Object... args) {
        String txt = format;
        if (args != null) txt = String.format(txt, args);
        appendText((bold ? "\n" : "") + txt + "\n");
    }

    @Override
    public void patchFinished() { Log.d("patch", "done!"); }

    @Override
    public void setVariableValue(@Nullable String key, @Nullable String value) {
        mGlobalVariables.put(key, value);
    }

    @Override
    public void allRules(int count) { mPatchAllRulesSize.postValue(count); }

    @Override
    public void currentRules(int count) { mPatchCurrentCountRules.postValue(count); }
}
