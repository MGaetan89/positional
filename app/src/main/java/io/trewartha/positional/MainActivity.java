package io.trewartha.positional;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatDelegate;
import android.view.View;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.util.LinkedList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.trewartha.positional.coordinates.CoordinatesFragment;
import io.trewartha.positional.coordinates.CoordinatesFragmentPagerAdapter;

public class MainActivity extends FragmentActivity implements CompoundButton.OnCheckedChangeListener {

    private static final long LOCATION_UPDATE_INTERVAL = 1000; // ms
    private static final int LOCATION_UPDATE_PRIORITY = LocationRequest.PRIORITY_HIGH_ACCURACY;
    private static final int REQUEST_CODE_LOCATION_PERMISSIONS = 1;
    private static final String TAG = MainActivity.class.getSimpleName();

    @BindView(R.id.coordinates_view_pager) ViewPager coordinatesViewPager;
    @BindView(R.id.accuracy_value_text_view) TextView accuracyValueTextView;
    @BindView(R.id.accuracy_unit_text_view) TextView accuracyUnitTextView;
    @BindView(R.id.elevation_value_text_view) TextView elevationValueTextView;
    @BindView(R.id.elevation_unit_text_view) TextView elevationUnitTextView;
    @BindView(R.id.speed_value_text_view) TextView speedValueTextView;
    @BindView(R.id.speed_unit_text_view) TextView speedUnitTextView;
    @BindView(R.id.bearing_value_text_view) TextView bearingValueTextView;
    @BindView(R.id.bearing_unit_text_view) TextView bearingUnitTextView;

    @BindView(R.id.progress_bar) ProgressBar progressBar;
    @BindView(R.id.screen_lock_switch) ImageView screenLockSwitch;

    @NonNull private List<CoordinatesFragment> coordinatesFragments = new LinkedList<>();
    @NonNull private LocationFormatter locationFormatter = new LocationFormatter(this);
    @NonNull private LocationListener locationListener = new FusedLocationListener();

    private CoordinatesFormat coordinatesFormat;
    private GoogleApiClient googleAPIClient;
    private Location location;
    private boolean screenLock;
    private SharedPreferences sharedPreferences;
    private boolean useMetricUnits;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);
        initializeNightMode();
        ButterKnife.bind(this);

        // Uncomment these lines to force night mode for development/testing
        // final UiModeManager uiModeManager = (UiModeManager) getSystemService(Context.UI_MODE_SERVICE);
        // uiModeManager.setNightMode(UiModeManager.MODE_NIGHT_YES);

        final GoogleConnectionCallbacks googleConnectionCallbacks = new GoogleConnectionCallbacks();
        googleAPIClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, googleConnectionCallbacks)
                .addConnectionCallbacks(googleConnectionCallbacks)
                .addApi(LocationServices.API)
                .build();

        sharedPreferences = getSharedPreferences(getString(R.string.settings_filename), Context.MODE_PRIVATE);
        useMetricUnits = sharedPreferences.getBoolean(getString(R.string.settings_metric_units_key), false);
        coordinatesFormat = CoordinatesFormat.valueOf(
                sharedPreferences.getString(getString(R.string.settings_coordinates_format_key), CoordinatesFormat.DMS.name())
        );
        screenLock = sharedPreferences.getBoolean(getString(R.string.settings_screen_lock_key), false);
        screenLockSwitch.setSelected(screenLock);

        final CoordinatesFragmentPagerAdapter coordinatesPagerAdapter = new CoordinatesFragmentPagerAdapter(
                getSupportFragmentManager()
        );
        coordinatesViewPager.setAdapter(coordinatesPagerAdapter);
        coordinatesViewPager.setOffscreenPageLimit(coordinatesPagerAdapter.getCount());
        coordinatesViewPager.setCurrentItem(getCoordinatesFragmentIndex(coordinatesFormat), true);
        coordinatesViewPager.addOnPageChangeListener(new CoordinatesPageChangeListener());

        updateLocationViews(null);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        initializeNightMode();
    }

    @Override
    public void onAttachFragment(Fragment fragment) {
        if (fragment instanceof CoordinatesFragment) {
            coordinatesFragments.add((CoordinatesFragment) fragment);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_LOCATION_PERMISSIONS) {
            if (permissions.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                Log.info(TAG, "Location permissions granted");
                requestLocationUpdates();
            } else {
                Log.info(TAG, "Location permissions request cancelled");
                new AlertDialog.Builder(this)
                        .setTitle(R.string.access_fine_location_permission_explanation_title)
                        .setMessage(R.string.access_fine_location_permission_explanation_message)
                        .setCancelable(false)
                        .setPositiveButton(R.string.try_again, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                requestLocationPermissions();
                            }
                        })
                        .show();
            }
        }
    }

    @OnClick({R.id.elevation_unit_text_view, R.id.speed_unit_text_view, R.id.accuracy_unit_text_view})
    public void onDistanceUnitClicked() {
        useMetricUnits = !useMetricUnits;
        updateLocationViews(location);
        setBooleanPreference(getString(R.string.settings_metric_units_key), useMetricUnits);
    }

    @OnClick(R.id.screen_lock_switch)
    public void onScreenLockClicked() {
        screenLock = !screenLock;
        screenLockSwitch.setSelected(screenLock);
        setBooleanPreference(getString(R.string.settings_screen_lock_key), screenLock);
        final int textRes = screenLock ? R.string.screen_lock_on : R.string.screen_lock_off;
        Toast.makeText(this, textRes, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
        if (compoundButton.getId() == R.id.screen_lock_switch) {
            setBooleanPreference(getString(R.string.settings_screen_lock_key), checked);
            lockScreen(checked);
        }
    }

    private int getCoordinatesFragmentIndex(@NonNull CoordinatesFormat coordinatesFormat) {
        switch (coordinatesFormat) {
            case DECIMAL:
                return 0;
            case DMS:
                return 1;
            case UTM:
                return 2;
            case MGRS:
                return 3;
            default:
                return 0;
        }
    }

    private boolean haveLocationPermissions() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void initializeNightMode() {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO);
    }

    private void lockScreen(boolean lock) {
        if (lock) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    private void logLocation(@NonNull Location location) {
        Log.debug(TAG, "Location received:");
        Log.debug(TAG, "       Latitude: " + location.getLatitude());
        Log.debug(TAG, "      Longitude: " + location.getLongitude());
        Log.debug(TAG, "       Accuracy: " + (location.hasAccuracy() ? location.getAccuracy() + " m" : "none"));
        Log.debug(TAG, "        Bearing: " + (location.hasBearing() ? location.getBearing() + "°" : "none"));
        Log.debug(TAG, "      Elevation: " + (location.hasAltitude() ? location.getAltitude() + " m/s" : "none"));
        Log.debug(TAG, "          Speed: " + (location.hasSpeed() ? location.getSpeed() + " m/s" : "none"));
    }

    private void updateLocationViews(@Nullable Location location) {
        updateCoordinatesFragments(location);
        accuracyValueTextView.setText(locationFormatter.getAccuracy(location, useMetricUnits));
        accuracyUnitTextView.setText(locationFormatter.getDistanceUnit(useMetricUnits));
        bearingValueTextView.setText(locationFormatter.getBearing(location));
        elevationValueTextView.setText(locationFormatter.getElevation(location, useMetricUnits));
        elevationUnitTextView.setText(locationFormatter.getDistanceUnit(useMetricUnits));
        speedValueTextView.setText(locationFormatter.getSpeed(location, useMetricUnits));
        speedUnitTextView.setText(locationFormatter.getSpeedUnit(useMetricUnits));
    }

    private void requestLocationPermissions() {
        Log.info(TAG, "Requesting permission for ACCESS_COARSE_LOCATION and ACCESS_FINE_LOCATION");
        ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
        }, REQUEST_CODE_LOCATION_PERMISSIONS);
    }

    private void requestLocationUpdates() {
        if (haveLocationPermissions()) {
            Log.info(TAG, "Requesting location updates");
            final LocationRequest locationRequest = LocationRequest.create()
                    .setPriority(LOCATION_UPDATE_PRIORITY)
                    .setInterval(LOCATION_UPDATE_INTERVAL);
            //noinspection MissingPermission
            LocationServices.FusedLocationApi.requestLocationUpdates(googleAPIClient, locationRequest, locationListener);
        } else {
            Log.info(TAG, "Location permissions are needed");
            requestLocationPermissions();
        }
    }

    private void setBooleanPreference(@NonNull String key, boolean value) {
        Log.info(TAG, "Saving " + key + " preference as " + value);
        sharedPreferences.edit().putBoolean(key, value).apply();
    }

    private void setStringPreference(@NonNull String key, @Nullable String value) {
        Log.info(TAG, "Saving " + key + " preference as " + value);
        sharedPreferences.edit().putString(key, value).apply();
    }

    private void suspendLocationUpdates() {
        Log.info(TAG, "Suspending location updates");
        LocationServices.FusedLocationApi.removeLocationUpdates(googleAPIClient, locationListener);
    }

    private void updateCoordinatesFragments(@Nullable Location location) {
        final double latitude, longitude;
        if (location == null) {
            latitude = 0.0;
            longitude = 0.0;
        } else {
            latitude = location.getLatitude();
            longitude = location.getLongitude();
        }
        for (CoordinatesFragment coordinatesFragment : coordinatesFragments) {
            coordinatesFragment.setCoordinates(latitude, longitude);
        }
    }

    private class CoordinatesPageChangeListener implements ViewPager.OnPageChangeListener {

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            // Don't do anything here
        }

        @Override
        public void onPageSelected(int position) {
            switch (position) {
                case 0:
                    coordinatesFormat = CoordinatesFormat.DECIMAL;
                    break;
                case 1:
                    coordinatesFormat = CoordinatesFormat.DMS;
                    break;
                case 2:
                    coordinatesFormat = CoordinatesFormat.UTM;
                    break;
                case 3:
                    coordinatesFormat = CoordinatesFormat.MGRS;
                    break;
                default:
                    coordinatesFormat = CoordinatesFormat.DECIMAL;
                    break;
            }
            setStringPreference(getString(R.string.settings_coordinates_format_key), coordinatesFormat.name());
        }

        @Override
        public void onPageScrollStateChanged(int state) {
            // Don't do anything here
        }
    }

    private class FusedLocationListener implements LocationListener {

        @Override
        public void onLocationChanged(@Nullable Location location) {
            MainActivity.this.location = location;
            if (location == null) {
                progressBar.setVisibility(View.VISIBLE);
            } else {
                logLocation(location);
                if (progressBar.getVisibility() == View.VISIBLE) {
                    progressBar.setVisibility(View.INVISIBLE);
                }
                updateLocationViews(location);
            }
        }
    }

    private class GoogleConnectionCallbacks implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

        @Override
        public void onConnected(@Nullable Bundle bundle) {
            Log.info(TAG, "Google Play Services connection established");
            requestLocationUpdates();
        }

        @Override
        public void onConnectionSuspended(int i) {
            Log.info(TAG, "Google Play Services connection suspended");
            suspendLocationUpdates();
        }

        @Override
        public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
            Log.error(TAG, "Google Play Services connection failed");
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle(R.string.google_play_services_connection_failed_title)
                    .setMessage(R.string.google_play_services_connection_failed_message)
                    .setPositiveButton(R.string.exit, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            MainActivity.this.finish();
                        }
                    })
                    .show();
        }
    }
}
