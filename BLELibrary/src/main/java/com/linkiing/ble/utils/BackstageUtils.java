package com.linkiing.ble.utils;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * app是前后台监听
 */
public class BackstageUtils implements Application.ActivityLifecycleCallbacks {
    private static BackstageUtils instance;
    private int activityCount = 0;
    private final List<BackstageListener> listeners = new ArrayList<>();
    private boolean isBackstage = true;

    public static BackstageUtils getInstance() {
        if (instance == null) {
            instance = new BackstageUtils();
        }
        return instance;
    }

    /**
     * Application 中初始化
     */
    public void init(Application application) {
        if (application != null) {
            application.registerActivityLifecycleCallbacks(this);
        }
    }

    public boolean isBackstage() {
        return isBackstage;
    }

    public void addBackstageListener(BackstageListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeBackstageListener(BackstageListener listener) {
        listeners.remove(listener);
    }

    private BackstageUtils() {}

    private void postListeners(boolean isBg) {
        if (!listeners.isEmpty()) {
            for (BackstageListener listener : listeners) {
                listener.onBackstageChange(isBg);
            }
        }
    }

    @Override
    public void onActivityStarted(@NonNull Activity activity) {
        activityCount++;
        if (activityCount == 1) {
            //app回到前台
            if (isBackstage){
                isBackstage = false;
                postListeners(false);
            }
        }
    }

    @Override
    public void onActivityStopped(@NonNull Activity activity) {
        activityCount--;
        if (activityCount == 0) {
            //app退到后台
            if (!isBackstage){
                isBackstage = true;
                postListeners(true);
            }
        }
    }

    @Override
    public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {

    }

    @Override
    public void onActivityResumed(@NonNull Activity activity) {

    }

    @Override
    public void onActivityPaused(@NonNull Activity activity) {

    }

    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {

    }

    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {

    }

    public interface BackstageListener {
        void onBackstageChange(boolean isBackstage);
    }
}
