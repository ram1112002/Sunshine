package com.example.android.sunshine.Sync;

import android.annotation.SuppressLint;
import android.app.TaskInfo;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;

import androidx.work.WorkManager;
import androidx.work.Worker;

import com.example.android.sunshine.data.WeatherContract;

import java.util.concurrent.TimeUnit;

public class SunshineSyncUtils {

    private static final int SYNC_INTERVAL_HOURS = 3;
    private static final int SYNC_INTERVAL_SECONDS = (int) TimeUnit.HOURS.toSeconds(SYNC_INTERVAL_HOURS);
    private static final int SYNC_FLEXTIME_SECONDS = SYNC_INTERVAL_SECONDS / 3;
    private static boolean sInitialized;


    private static final String SUNSHINE_SYNC_TAG = "sunshine-sync";



    synchronized public static void scheduleWorkManger(){
        Constraints constraints = new Constraints.Builder()
                                        .setRequiredNetworkType(NetworkType.CONNECTED)
                                        .build();

        PeriodicWorkRequest periodicWorkRequest = new PeriodicWorkRequest.Builder(SunshineWorkManger.class,SYNC_FLEXTIME_SECONDS,TimeUnit.SECONDS)
                                                    .setInitialDelay(SYNC_INTERVAL_SECONDS,TimeUnit.SECONDS)
                                                    .addTag(SUNSHINE_SYNC_TAG)
                                                    .setConstraints(constraints)
                                                    .build();

        androidx.work.WorkManager.getInstance().enqueue(periodicWorkRequest);


    }

    /**
     * Creates periodic sync tasks and checks to see if an immediate sync is required. If an
     * immediate sync is required, this method will take care of making sure that sync occurs.
     *
     * @param context Context that will be passed to other methods and used to access the
     *                ContentResolver
     */
    synchronized public static void initialized(@NonNull final Context context){
        /*
         * Only perform initialization once per app lifetime. If initialization has already been
         * performed, we have nothing to do in this method.
         */
        if (sInitialized ) return;


    scheduleWorkManger();


        /*
         * We need to check to see if our ContentProvider has data to display in our forecast
         * list. However, performing a query on the main thread is a bad idea as this may
         * cause our UI to lag. Therefore, we create a thread in which we will run the query
         * to check the contents of our ContentProvider.
         */
        new AsyncTask<Void,Void,Void>(){

            @Override
            protected Void doInBackground(Void... voids) {
                /* URI for every row of weather data in our weather table*/
                Uri forecastWeatherUri = WeatherContract.WeatherEntry.CONTENT_URI;
                /*
                 * Since this query is going to be used only as a check to see if we have any
                 * data (rather than to display data), we just need to PROJECT the ID of each
                 * row. In our queries where we display data, we need to PROJECT more columns
                 * to determine what weather details need to be displayed.
                 */
                String[] projectionColumns = {WeatherContract.WeatherEntry._ID};
                String selectionStatement = WeatherContract.WeatherEntry.getSqlSelectForTodayOnwards();
                /* Here, we perform the query to check to see if we have any weather data */
                Cursor cursor = context.getContentResolver().query(
                        forecastWeatherUri,
                        projectionColumns,
                        selectionStatement,
                        null,null
                );
                /*
                 * A Cursor object can be null for various different reasons. A few are
                 * listed below.
                 *
                 *   1) Invalid URI
                 *   2) A certain ContentProvider's query method returns null
                 *   3) A RemoteException was thrown.
                 *
                 * Bottom line, it is generally a good idea to check if a Cursor returned
                 * from a ContentResolver is null.
                 *
                 * If the Cursor was null OR if it was empty, we need to sync immediately to
                 * be able to display data to the user.
                 */
                if (null == cursor || cursor.getCount() == 0){
                    startImmediateSync(context);
                }



                cursor.close();
                return null;
            }

        }.execute();
//        sInitialized = true;
    }

    /**
     * Helper method to perform a sync immediately using an IntentService for asynchronous
     * execution.
     *
     * @param context The Context used to start the IntentService for the sync.
     */
    public static void startImmediateSync(@NonNull final Context context){

        Intent IntentToSyncImmediately = new Intent(context, SunshineSyncIntentService.class);
        context.startService(IntentToSyncImmediately);

    }
}
