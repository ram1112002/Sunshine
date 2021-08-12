package com.example.android.sunshine.Sync;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class SunshineWorkManger extends Worker {


    public SunshineWorkManger(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = SunshineWorkManger.super.getApplicationContext();
        SunshineSyncTask.syncWeather(context);
        return Result.success();
    }


}
