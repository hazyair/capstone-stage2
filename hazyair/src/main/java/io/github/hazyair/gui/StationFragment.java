package io.github.hazyair.gui;

import android.app.Activity;
//import android.content.BroadcastReceiver;
import android.content.Context;
//import android.content.Intent;
//import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Rect;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.util.LongSparseArray;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.github.hazyair.R;
import io.github.hazyair.data.DataContract;
import io.github.hazyair.data.DataLoader;
import io.github.hazyair.data.SensorsContract;
import io.github.hazyair.data.SensorsLoader;
import io.github.hazyair.data.StationsContract;
import io.github.hazyair.source.Base;
import io.github.hazyair.source.Data;
import io.github.hazyair.source.Sensor;
import io.github.hazyair.source.Station;

//import android.support.v4.app.DatabaseService;

import static io.github.hazyair.util.Location.PERMISSION_REQUEST_FINE_LOCATION;

public class StationFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>,
        LocationListener {

    // Final definitions
    private static final String PARAM_SELECTED = "io.github.hazyair.PARAM_SELECTED";

    // Nested class definitions
    class ViewHolder extends RecyclerView.ViewHolder {

        @Nullable @BindView(R.id.cardview)
        CardView cardView;

        @Nullable @BindView(R.id.map)
        FrameLayout frameLayout;

        @Nullable @BindView(R.id.expand_collapse)
        Button expandCollapse;

        ViewHolder(View itemView) {
            super(itemView);
        }
    }

    class MapViewHolder extends ViewHolder implements OnMapReadyCallback {

        @Nullable @BindView(R.id.place)
        TextView place;

        @Nullable @BindView(R.id.distance)
        TextView distance;

        @Nullable @BindView(R.id.map)
        FrameLayout frameLayout;

        private final Bundle mStation;
        private final SupportMapFragment mSupportMapFragment;

        MapViewHolder(View itemView, Bundle station) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            mStation = station;
            if (place != null)
                place.setText(String.format("%s %s %s",
                        mStation.getString(StationsContract.COLUMN_COUNTRY),
                        mStation.getString(StationsContract.COLUMN_LOCALITY),
                        mStation.getString(StationsContract.COLUMN_ADDRESS)));
            mSupportMapFragment = StationMapFragment.newInstance();
            getChildFragmentManager().beginTransaction()
                    .replace(R.id.map, mSupportMapFragment).commitAllowingStateLoss();
            mSupportMapFragment.getMapAsync(this);

        }

        @Override
        public void onMapReady(GoogleMap googleMap) {
            LatLng latLng = new LatLng(mStation.getDouble(StationsContract.COLUMN_LATITUDE),
                    mStation.getDouble(StationsContract.COLUMN_LONGITUDE));
            googleMap.addMarker(new MarkerOptions().position(latLng)
                    .title(String.format("%s %s",
                            getString(R.string.text_station_by),
                            mStation.getString(StationsContract.COLUMN_SOURCE))));
            googleMap.moveCamera(CameraUpdateFactory.zoomTo(10));
            googleMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        }
    }

    class SensorViewHolder extends ViewHolder {

        @Nullable @BindView(R.id.placeholder)
        ConstraintLayout constraintLayout;

        @Nullable @BindView(R.id.parameter)
        TextView parameter;

        @Nullable @BindView(R.id.result)
        TextView result;

        @Nullable @BindView(R.id.expand_collapse)
        Button expandCollapse;

        @Nullable @BindView(R.id.chart)
        LineChart chart;

        SensorViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }

    class SensorsAdapter extends RecyclerView.Adapter {

        static final int VIEW_TYPE_MAP = 0;
        static final int VIEW_TYPE_SENSOR = 1;

        private Cursor mCursor;
        private final SparseArray<Bundle> mData = new SparseArray<>();
        private final SparseArray<LongSparseArray<Double>> mChart = new SparseArray<>();
        private Location mLocation;
        private boolean mDistance;

        private final Bundle mStation;

        private Bundle mSelectedItem;

        SensorsAdapter(Bundle station, Bundle selectedItem, boolean distance) {
            super();
            mStation = station;
            mSelectedItem = selectedItem;
            mDistance = distance;
        }

        void setCursor(Cursor cursor) {
            if (cursor == null || cursor.getCount() == 0) return;
            for (int i = 0; i < cursor.getCount(); i++) {
                cursor.moveToPosition(i);
                Bundle bundle = Sensor.toBundleFromCursor(cursor);
                getLoaderManager().initLoader(bundle.getInt(SensorsContract.COLUMN__ID), bundle,
                        StationFragment.this);
                getLoaderManager().initLoader(-bundle.getInt(SensorsContract.COLUMN__ID), bundle,
                        StationFragment.this);
            }
            mCursor = cursor;
            notifyDataSetChanged();
        }

        void setData(Cursor cursor) {
            if (cursor == null || cursor.getCount() == 0) return;
            cursor.moveToFirst();
            Bundle bundle = Data.toBundleFromCursor(cursor);
            mData.put(bundle.getInt(DataContract.COLUMN__SENSOR_ID), bundle);
            notifyDataSetChanged();
        }

        void setChart(Cursor cursor) {
            if (cursor == null || cursor.getCount() == 0) return;
            LongSparseArray<Double> data = new LongSparseArray<>();
            Bundle bundle = null;
            for (int position = 0; position < cursor.getCount(); position++) {
                cursor.moveToPosition(position);
                bundle = Data.toBundleFromCursor(cursor);
                data.put(bundle.getLong(DataContract.COLUMN_TIMESTAMP),
                        bundle.getDouble(DataContract.COLUMN_VALUE));
            }
            if (bundle != null) mChart.put(bundle.getInt(DataContract.COLUMN__SENSOR_ID), data);
            notifyDataSetChanged();
        }

        void setLocation(Location location) {
            mLocation = location;
            notifyDataSetChanged();
        }

        void setDistance(boolean distance) {
            mDistance = distance;
            notifyDataSetChanged();
        }

        @Override
        public int getItemViewType(int position) {
            if (position == 0) {
                return VIEW_TYPE_MAP;
            } else {
                return VIEW_TYPE_SENSOR;
            }
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            switch (viewType) {
                case VIEW_TYPE_MAP:
                    return new MapViewHolder(LayoutInflater.from(parent.getContext())
                            .inflate(R.layout.location, parent, false), mStation);
                case VIEW_TYPE_SENSOR:
                default:
                    return new SensorViewHolder(LayoutInflater.from(parent.getContext())
                            .inflate(R.layout.sensor, parent, false));
            }
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            switch (holder.getItemViewType()) {
                case VIEW_TYPE_MAP: {
                    Context context = getContext();
                    if (context == null) return;
                    MapViewHolder mapViewHolder = (MapViewHolder) holder;
                    if (Base.equals(mSelectedItem, mStation)) expand(context, mapViewHolder, mStation);
                    else collapse(context, mapViewHolder);
                    if (mapViewHolder.cardView != null)
                        mapViewHolder.cardView.setOnClickListener((v) ->
                                OnClickListener(context, mapViewHolder, mStation));
                    if (mapViewHolder.expandCollapse != null)
                        mapViewHolder.expandCollapse.setOnClickListener((v) ->
                                OnClickListener(context, mapViewHolder, mStation));
                    if (mapViewHolder.distance != null) {
                        mapViewHolder.distance.setVisibility(mDistance ? View.VISIBLE : View.GONE);
                        if (mDistance && mLocation != null) {
                            Location location = new Location(
                                    mStation.getString(StationsContract.COLUMN_SOURCE));
                            location.setLongitude(
                                    mStation.getDouble(StationsContract.COLUMN_LONGITUDE));
                            location.setLatitude(
                                    mStation.getDouble(StationsContract.COLUMN_LATITUDE));
                            mapViewHolder.distance.setText(String.format("%s %s",
                                    String.valueOf((int) (location.distanceTo(mLocation) / 1000)),
                                    getString(R.string.text_km)));
                        }
                    }
                    break;
                }
                case VIEW_TYPE_SENSOR:
                    position--;
                    SensorViewHolder sensorViewHolder = (SensorViewHolder) holder;
                    mCursor.moveToPosition(position);
                    Context context = getContext();
                    if (context == null) return;
                    Bundle sensor = Sensor.toBundleFromCursor(mCursor);
                    if (sensorViewHolder.parameter != null)
                        sensorViewHolder.parameter.setText(
                                sensor.getString(SensorsContract.COLUMN_PARAMETER));
                    Bundle data = mData.get(sensor.getInt(SensorsContract.COLUMN__ID));
                    if (data == null) {
                        if (sensorViewHolder.cardView != null)
                            sensorViewHolder.cardView
                                    .setCardBackgroundColor(context.getColor(R.color.textLight));
                        //sensorViewHolder.itemView.setVisibility(View.GONE);
                    } else {
                        if (sensorViewHolder.cardView != null)
                            sensorViewHolder.cardView
                                    .setCardBackgroundColor(context.getColor(android.R.color.white));
                        //sensorViewHolder.itemView.setVisibility(View.VISIBLE);
                        if (sensorViewHolder.result != null)
                            sensorViewHolder.result.setText(String.format("%s %s",
                                    String.valueOf(data.getDouble(DataContract.COLUMN_VALUE)),
                                    sensor.getString(SensorsContract.COLUMN_UNIT)));
                    }
                    LongSparseArray<Double> chart =
                            mChart.get(sensor.getInt(SensorsContract.COLUMN__ID));
                    if (chart != null && sensorViewHolder.chart != null) {
                        ArrayList<Entry> values = new ArrayList<>();
                        for (int i = 0; i < chart.size(); i++) {
                            values.add(new Entry(chart.keyAt(i), chart.valueAt(i).floatValue()));
                        }
                        XAxis xAxis = sensorViewHolder.chart.getXAxis();
                        xAxis.setLabelCount(3, true);
                        xAxis.setValueFormatter((value, axis) ->
                                new SimpleDateFormat("E HH:mm",
                                        Locale.getDefault()).format(new Date((long) value))
                        );
                        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
                        LineDataSet lineDataSet = new LineDataSet(values,
                                sensor.getString(SensorsContract.COLUMN_PARAMETER));
                        lineDataSet.setDrawCircles(false);
                        lineDataSet.setDrawValues(false);
                        lineDataSet.setColor(context.getColor(R.color.accent));
                        Description description = new Description();
                        description.setText(sensor.getString(SensorsContract.COLUMN_UNIT));
                        sensorViewHolder.chart.setDescription(description);
                        sensorViewHolder.chart.setData(new LineData(lineDataSet));
                    }
                    if (Base.equals(mSelectedItem, sensor))
                        expand(context, sensorViewHolder, sensor);
                    else collapse(context, sensorViewHolder);
                    if (sensorViewHolder.expandCollapse != null && data != null)
                        sensorViewHolder.expandCollapse.setOnClickListener((v) ->
                                OnClickListener(context, sensorViewHolder, sensor));
                    if (sensorViewHolder.cardView != null && data != null)
                        sensorViewHolder.cardView.setOnClickListener((v) ->
                                OnClickListener(context, sensorViewHolder, sensor));
            }
        }

        private void collapse(Context context, ViewHolder vh) {
            if (vh instanceof MapViewHolder && vh.frameLayout != null) {
                vh.frameLayout.setVisibility(View.GONE);
            }
            if (vh instanceof SensorViewHolder) {
                SensorViewHolder svh = (SensorViewHolder) vh;
                if (svh.chart != null)
                    svh.chart.setVisibility(View.GONE);
            }
            if (vh != null && vh.expandCollapse != null)
                vh.expandCollapse.setBackground(context
                        .getDrawable(R.drawable.ic_keyboard_arrow_down_black_24dp));
        }

        private void expand(Context context, ViewHolder viewHolder, Bundle bundle) {

            if (viewHolder.expandCollapse != null)
                viewHolder.expandCollapse.setBackground(
                        context.getDrawable(R.drawable.ic_keyboard_arrow_up_black_24dp));
            if (viewHolder instanceof MapViewHolder && viewHolder.frameLayout != null) {
                viewHolder.frameLayout.setVisibility(View.VISIBLE);
                mSelectedItem = bundle;
            }
            if (viewHolder instanceof  SensorViewHolder) {
                SensorViewHolder sensorViewHolder = (SensorViewHolder) viewHolder;
                if (sensorViewHolder.chart != null) {
                    sensorViewHolder.chart.setVisibility(View.VISIBLE);
                    mSelectedItem = bundle;
                }
            }

        }

        private void OnClickListener(Context context, ViewHolder viewHolder, Bundle bundle) {
            boolean visibility = false;
            if (viewHolder instanceof MapViewHolder && viewHolder.frameLayout != null) {
                visibility = viewHolder.frameLayout.getVisibility() == View.VISIBLE;
            }
            if (viewHolder instanceof SensorViewHolder) {
                SensorViewHolder sensorViewHolder = (SensorViewHolder) viewHolder;
                if (sensorViewHolder.chart == null) return;
                visibility = sensorViewHolder.chart.getVisibility() == View.VISIBLE;
            }
            if (visibility) {
                if (viewHolder.expandCollapse != null)
                    viewHolder.expandCollapse.setBackground(
                            context.getDrawable(R.drawable.ic_keyboard_arrow_down_black_24dp));
                if (viewHolder instanceof MapViewHolder)
                    viewHolder.frameLayout.setVisibility(View.GONE);
                if (viewHolder instanceof SensorViewHolder) {
                    SensorViewHolder sensorViewHolder = (SensorViewHolder) viewHolder;
                    if (sensorViewHolder.chart != null)
                        sensorViewHolder.chart.setVisibility(View.GONE);
                }
            } else {
                for (int i = 0; i < getItemCount(); i++) {
                    collapse(context,
                            (ViewHolder) mRecyclerView.findViewHolderForLayoutPosition(i));
                }
                expand(context, viewHolder, bundle);
            }
        }

        @Override
        public int getItemCount() {
            return mCursor == null ? 1 : mCursor.getCount() + 1;
        }

    }

    private SensorsAdapter mSensorsAdapter;
    private LocationManager mLocationManager;

    // ButterKnife
    @SuppressWarnings("WeakerAccess")
    @BindView(R.id.sensors)
    RecyclerView mRecyclerView;

    // Fragment initialization
    public StationFragment() {
    }

    public static StationFragment newInstance(Cursor cursor) {
        StationFragment fragment = new StationFragment();
        fragment.setArguments(Station.toBundleFromCursor(cursor));
        return fragment;
    }

    // Fragment lifecycle
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Context context = getContext();
        if (context != null)
            mLocationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);
        ButterKnife.bind(this, rootView);
        Bundle station = getArguments();
        Bundle selected = null;
        if (savedInstanceState != null) selected = savedInstanceState.getBundle(PARAM_SELECTED);
        mSensorsAdapter = new SensorsAdapter(station,
                selected,
                io.github.hazyair.util.Location.checkPermission(getActivity()));
        mRecyclerView.setAdapter(mSensorsAdapter);
        mRecyclerView.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(Rect outRect, View view, RecyclerView parent,
                                       RecyclerView.State state) {
                super.getItemOffsets(outRect, view, parent, state);
                int itemCount = state.getItemCount();

                final int itemPosition = parent.getChildLayoutPosition(view);

                if (itemPosition == RecyclerView.NO_POSITION) {
                    return;
                }

                int fab = 0;
                Activity activity = getActivity();
                if (activity != null)
                    fab = activity.findViewById(R.id.fab_add_station).getMeasuredHeight();

                Resources resources = getResources();
                if (itemCount > 0 && itemPosition == itemCount - 1) {
                    outRect.set(0, 0, 0,
                            resources.getDimensionPixelSize(R.dimen.edge) +
                                    resources.getDimensionPixelSize(R.dimen.fab_margin) + fab);
                }
            }
        });

        getLoaderManager().initLoader(0, station, this);

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        io.github.hazyair.util.Location.requestUpdates(getContext(), mLocationManager,
                this);
    }

    @Override
    public void onPause() {
        super.onPause();
        io.github.hazyair.util.Location.removeUpdates(getContext(), mLocationManager,
                this);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mSensorsAdapter != null)
            outState.putParcelable(PARAM_SELECTED, mSensorsAdapter.mSelectedItem);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSION_REQUEST_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 &&
                        grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    io.github.hazyair.util.Location.requestUpdates(getContext(),
                            mLocationManager, this);

                }
            }

        }
    }

    // Loader handlers
    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int id, @Nullable Bundle args) {
        if (id == 0) {
            return SensorsLoader.newInstanceForAllSensorsFromStation(getContext(),
                    args == null ? 0 : args.getInt(StationsContract.COLUMN__ID));
        } else if (id > 0) {
            return DataLoader.newInstanceForLastDataFromSensor(getContext(),
                    args == null ? 0 : args.getInt(SensorsContract.COLUMN__ID));
        } else {
            return DataLoader.newInstanceForAllDataFromSensor(getContext(),
                    args == null ? 0 : args.getInt(SensorsContract.COLUMN__ID));
        }
    }

    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor cursor) {
        int id = loader.getId();
        if (id == 0) {
            mSensorsAdapter.setCursor(cursor);
        } else if (id > 0) {
            mSensorsAdapter.setData(cursor);
        } else {
            mSensorsAdapter.setChart(cursor);
        }
    }

    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> loader) {
        mSensorsAdapter.setCursor(null);
    }

    // Location handlers
    @Override
    public void onLocationChanged(Location location) {
        if (mSensorsAdapter != null) mSensorsAdapter.setLocation(location);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    @Override
    public void onProviderEnabled(String provider) {
        if (mSensorsAdapter != null) mSensorsAdapter.setDistance(true);
    }

    @Override
    public void onProviderDisabled(String provider) {
        if (mSensorsAdapter != null) mSensorsAdapter.setDistance(false);
    }


}