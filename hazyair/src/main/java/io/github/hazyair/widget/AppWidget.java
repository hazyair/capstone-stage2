package io.github.hazyair.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DatabaseService;
import android.widget.RemoteViews;

import io.github.hazyair.R;
import io.github.hazyair.data.StationsContract;
import io.github.hazyair.gui.MainActivity;
import io.github.hazyair.util.Text;

public class AppWidget extends AppWidgetProvider {

    private static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                        int appWidgetId) {

        Bundle station = DatabaseService.selectedStation(context);
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(),
                R.layout.appwidget);
        if (station == null) {
            remoteViews.setTextViewText(R.id.place,
                    context.getString(R.string.app_name));
            remoteViews.setTextViewText(R.id.address,
                    context.getString(R.string.text_no_station_selected));
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.sensors);
        } else {
            Intent intent = new Intent(context, MainActivity.class);
            intent.putExtra(MainActivity.PARAM_STATION, station);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0,
                    intent, PendingIntent.FLAG_UPDATE_CURRENT);
            remoteViews.setOnClickPendingIntent(R.id.appwidget, pendingIntent);
            remoteViews.setOnClickPendingIntent(R.id.place, pendingIntent);
            remoteViews.setOnClickPendingIntent(R.id.address, pendingIntent);
            remoteViews.setPendingIntentTemplate(R.id.sensors, pendingIntent);
            remoteViews.setTextViewText(R.id.place, Text.truncateSting(String.format("%s %s",
                    station.getString(StationsContract.COLUMN_COUNTRY),
                    station.getString(StationsContract.COLUMN_LOCALITY)), 32));
            remoteViews.setTextViewText(R.id.address,
                    Text.truncateSting(station.getString(StationsContract.COLUMN_ADDRESS),
                            32));
            remoteViews.setRemoteAdapter(R.id.sensors,
                    new Intent(context, AppWidgetService.class));
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.sensors);
        }
        appWidgetManager.updateAppWidget(appWidgetId, remoteViews);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    public static void update(Context context) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        int [] appWidgetIds = appWidgetManager.getAppWidgetIds(
                new ComponentName(context, AppWidget.class));
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }
}
