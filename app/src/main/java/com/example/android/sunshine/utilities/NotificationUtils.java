package com.example.android.sunshine.utilities;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ProxyInfo;
import android.net.Uri;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.TaskStackBuilder;
import androidx.core.content.ContextCompat;

import com.example.android.sunshine.DetailActivity;
import com.example.android.sunshine.R;
import com.example.android.sunshine.data.SunshinePreferences;
import com.example.android.sunshine.data.WeatherContract;

public class NotificationUtils {

public static final String[] WEATHER_NOTIFICATION_PROJECTION = {
        WeatherContract.WeatherEntry.COLUMN_WEATHER_ID,
        WeatherContract.WeatherEntry.COLUMN_MAX_TEMP,
        WeatherContract.WeatherEntry.COLUMN_MIN_TEMP
};
public static final int INDEX_WEATHER_ID = 0;
public static final int INDEX_WEATHER_MAX = 1;
public static final int INDEX_WEATHER_MIN = 2;

public static final int WEATHER_NOTIFICATION_ID = 3004;

public static final String WEATHER_NOTIFICATION_CHANNEL_ID = "reminder_notification_channel";

private static Cursor Cursor (Context context){
    /* Build the URI for today's weather in order to show up to date data in notification */
    Uri todayWeatherUri = WeatherContract.WeatherEntry.buildWeatherUriWithDate(SunshineDateUtils.normalizeDate(System.currentTimeMillis()));

    Cursor WeatherCursor = context.getContentResolver().query(
            todayWeatherUri,
            WEATHER_NOTIFICATION_PROJECTION,
            null,null,null
    );
    return WeatherCursor;


}

private static int getSmallIcon(Context context) {
    Cursor getCursor = Cursor(context);
    if (getCursor.moveToFirst()) {
        /* Weather ID as returned by API, used to identify the icon to be used */
        int weatherID = getCursor.getInt(INDEX_WEATHER_ID);




        /* getSmallArtResourceIdForWeatherCondition returns the proper art to show given an ID */
        int smallArtResourcesId = SunshineWeatherUtils.getSmallArtResourceIdForWeatherCondition(weatherID);

        return smallArtResourcesId;
    }
        return getSmallIcon(context);
}
private static Bitmap largeIcon(Context context) {
    Cursor getCursor = Cursor(context);
    if (getCursor.moveToFirst()) {
        /* Weather ID as returned by API, used to identify the icon to be used */
        int weatherID = getCursor.getInt(INDEX_WEATHER_ID);

        Resources resources = context.getResources();
        int largeAryResourcesId = SunshineWeatherUtils.getLargeArtResourceIdForWeatherCondition(weatherID);

        Bitmap largeIcon = BitmapFactory.decodeResource(resources, largeAryResourcesId);
        return largeIcon;

    }
   return largeIcon(context);
}

private static String NotificationText(Context context) {
    Cursor getCursor = Cursor(context);
    if (getCursor.moveToFirst()) {
        /* Weather ID as returned by API, used to identify the icon to be used */
        int weatherID = getCursor.getInt(INDEX_WEATHER_ID);
        double high = getCursor.getDouble(INDEX_WEATHER_MAX);
        double low = getCursor.getDouble(INDEX_WEATHER_MIN);
        String notificationText = getNotificationText(context, weatherID, high, low);
            return notificationText;
    }
    return NotificationText(context);
}
public static void notifyUserOfNewWeather(Context context){



    /*
     * If todayWeatherCursor is empty, moveToFirst will return false. If our cursor is not
     * empty, we want to show the notification.
     */


        NotificationManager notificationManager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel mChannel  = new NotificationChannel(
                    WEATHER_NOTIFICATION_CHANNEL_ID,
                    context.getString(R.string.notification_channel_name),
                    NotificationManager.IMPORTANCE_HIGH
            );
            notificationManager.createNotificationChannel(mChannel);
        }


        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context, WEATHER_NOTIFICATION_CHANNEL_ID)
                .setColor(ContextCompat.getColor(context, R.color.colorPrimary))
                .setSmallIcon(getSmallIcon(context))
                .setLargeIcon(largeIcon(context))
                .setContentTitle(context.getString(R.string.app_name))
                .setContentText(NotificationText(context))
                .setContentIntent(contentIntent(context))
                .setAutoCancel(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN && Build.VERSION.SDK_INT <Build.VERSION_CODES.O){
            notificationBuilder.setPriority(NotificationCompat.PRIORITY_HIGH);
        }

        /*
         * This Intent will be triggered when the user clicks the notification. In our case,
         * we want to open Sunshine to the DetailActivity to display the newly updated weather.
         */





        notificationManager.notify(WEATHER_NOTIFICATION_ID, notificationBuilder.build());
        /*
         * Since we just showed a notification, save the current time. That way, we can check
         * next time the weather is refreshed if we should show another notification.
         */
//        SunshinePreferences.saveLastNotificationTime(context,System.currentTimeMillis());
    }



private static PendingIntent contentIntent(Context context){
    Uri todayWeatherUri = WeatherContract.WeatherEntry.buildWeatherUriWithDate(SunshineDateUtils.normalizeDate(System.currentTimeMillis()));
    Intent detailIntentForToday = new Intent(context, DetailActivity.class);
    detailIntentForToday.setData(todayWeatherUri);

    //Use TaskStackBuilder to create the proper PendingIntent
    TaskStackBuilder taskStackBuilder = TaskStackBuilder.create(context);
    taskStackBuilder.addNextIntentWithParentStack(detailIntentForToday);
    PendingIntent ResultPendingIntent = taskStackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
    return  ResultPendingIntent;
}

private static String getNotificationText(Context context,int weatherId, double high, double low){

    String shortDiscription = SunshineWeatherUtils.getStringForWeatherCondition(context,weatherId);
    String notificationFormat = context.getString(R.string.format_notification);

    String notificationText = String.format(notificationFormat,
            shortDiscription,
            SunshineWeatherUtils.formatTemperature(context, high),
            SunshineWeatherUtils.formatTemperature(context,low));

    return notificationText;
}
}
