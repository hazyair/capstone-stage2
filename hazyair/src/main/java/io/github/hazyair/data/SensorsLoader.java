package io.github.hazyair.data;

import android.content.Context;
import android.support.v4.content.CursorLoader;
import android.net.Uri;

import io.github.hazyair.source.Sensor;

public class SensorsLoader extends CursorLoader {

    @SuppressWarnings("SameParameterValue")
    private SensorsLoader(Context context, Uri uri, int _id) {
        super(context, uri, Sensor.keys(), SensorsContract.COLUMN__STATION_ID + "=?",
                new String[] { String.valueOf(_id) }, HazyairProvider.Sensors.DEFAULT_SORT);
    }

    public static SensorsLoader newInstanceForAllSensorsFromStation(Context context, int _id) {
        return new SensorsLoader(context, HazyairProvider.Sensors.CONTENT_URI, _id);
    }

}