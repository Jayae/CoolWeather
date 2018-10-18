package com.coolweather.android.util;

import android.app.Activity;

import java.util.ArrayList;
import java.util.List;

public class ActivityCollector {
    public static List<Activity> activityList = new ArrayList<>();

    public static void exit() {
        for (Activity activity : activityList) {
            activity.finish();
        }
        activityList.clear();
    }
    public static void addActivity(Activity activity){
        activityList.add(activity);
    }

    public static void removeActivity(Activity activity){
        activityList.remove(activity);
    }
}
