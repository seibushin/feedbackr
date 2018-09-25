package de.hhu.cs.feedbackr.view.activity;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import de.hhu.cs.feedbackr.R;
import de.hhu.cs.feedbackr.firebase.FirebaseHelper;
import de.hhu.cs.feedbackr.model.Feedback;
import de.hhu.cs.feedbackr.model.Profile;
import de.hhu.cs.feedbackr.view.fragment.FeedbackSendFragment;
import de.hhu.cs.feedbackr.view.fragment.FeedbacksFragment;
import de.hhu.cs.feedbackr.view.fragment.LocationErrorFragment;
import de.hhu.cs.feedbackr.view.fragment.MapFragment;

public class MainActivity extends AppCompatActivity implements BottomNavigationView.OnNavigationItemSelectedListener {
    private static final int PERMISSION_LOCATION_REQUEST_CODE = 0;
    private static final String REQUESTING_LOCATION_UPDATES_KEY = "Requesting Location";
    private static final String LOCATION_KEY = "Location";
    private static final int REQUEST_CHECK_SETTINGS = 1;
    private static final String CURRENT_BOTTOM_TAB_SELECTED_KEY = "Current Bottom Bar Tab";

    private int mBottomBarPosition;
    private BottomNavigationView bottomBar;

    private LocationRequest locationRequest;
    private LocationCallback locationCallback;
    private Location currentLocation;

    private String mCurrentCity = "";
    private boolean mRequestingLocationUpdates;

    /**
     * Setting Up the Activity
     *
     * @param savedInstanceState Saved Instance
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Sets Up View
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //Initializes the Bottom Bar
        bottomBar = findViewById(R.id.bottom_navigation);
        bottomBar.setOnNavigationItemSelectedListener(this);

        makeLocationInit(false);
        getSupportFragmentManager().beginTransaction().add(R.id.main_frame, new FeedbackSendFragment(), "SEND").commit();
        updateValuesFromBundle(savedInstanceState);
    }

    /**
     * Try to authenticate the user for firebase
     */
    private void authFirebase() {
        // authenticates the user anonymously for firebase
        final FirebaseAuth auth = FirebaseAuth.getInstance();
        auth.signInAnonymously()
                .addOnCompleteListener(this, task -> {
                    if (!task.isSuccessful()) {
                        Objects.requireNonNull(task.getException()).printStackTrace();
                        // sign in failed
                        createToast(getString(R.string.auth_failed), Toast.LENGTH_LONG);
                        Crashlytics.logException(new Throwable("Authentication failed - " + task.getException()));
                    } else {
                        Profile.setAuth(true);
                        getProfile();
                    }
                });
    }

    /**
     * Get Last Location Saved
     *
     * @return Last Saved Location
     */
    public Location getCurrentLocation() {
        return currentLocation;
    }

    private void getProfile() {
        if (Profile.getInstance() == null) {
            Objects.requireNonNull(FirebaseHelper.getUserRef()).addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    Profile.setInstanceProfile(dataSnapshot.getValue(Profile.class));
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    System.out.println("The read failed: " + databaseError.getCode());
                }
            });
        }
    }

    /**
     * Sends Feedback
     *
     * @param kind Decides whether Feedback is Positive or Negative
     */
    public void sendFeedback(boolean kind) {
        String feedbackId = FirebaseHelper.generateFeedbackID();

        //Creates the Feedback
        if (currentLocation == null) {
            createToast(getString(R.string.noLocation), Toast.LENGTH_LONG);
            Crashlytics.logException(new Throwable("Try to sendFeedback but currentLocation is null"));
        } else if (!Profile.isAuth()) {
            createToast(getString(R.string.noAuth), Toast.LENGTH_SHORT);
        } else {
            Feedback feedback = new Feedback(currentLocation, mCurrentCity, kind, feedbackId);

            // Saves it in Firebase
            FirebaseHelper.saveFeedback(feedback);

            // Show Success Toast
            createToast(String.format(getString(R.string.feedback_send),
                    feedback.isPositive() ? getString(R.string.positive) : getString(R.string.negative)), Toast.LENGTH_LONG);

            //Switch to Edit View
            switchToFeedbackDetail(feedback);
        }
    }

    /**
     * Switches to View to Edit Feedback
     *
     * @param feedback Feedback to be Edited
     */
    public void switchToFeedbackDetail(Feedback feedback) {
        Intent intent = new Intent(this, FeedbackEditActivity.class);
        intent.putExtra("Feedback", feedback);
        startActivity(intent);
    }

    /**
     * Creates and shows a Simple Toast
     *
     * @param msg Message to be shown
     */
    public void createToast(String msg, int duration) {
        Toast.makeText(this, msg, duration).show();
    }

    /**
     * Get the Current City
     */
    public void getCityName() {
        Geocoder gcd = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = gcd.getFromLocation(currentLocation.getLatitude(), currentLocation.getLongitude(), 1);
            if (addresses != null) {
                mCurrentCity = addresses.get(0).getLocality();
            }
        } catch (IOException ignored) {
        }
    }

    /**
     * Initialises the Location listening
     */
    public void makeLocationInit(boolean showFeedback) {
        locationRequest = new LocationRequest();
        locationRequest.setInterval(20000);
        locationRequest.setFastestInterval(10000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(locationRequest);
        builder.setAlwaysShow(true);
        LocationSettingsRequest locationSettingsRequest = builder.build();

        // location callback
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                onLocationChanged(locationResult.getLastLocation(), false);
            }
        };

        // check whether location settings are satisfied
        SettingsClient settingsClient = LocationServices.getSettingsClient(this);
        settingsClient.checkLocationSettings(locationSettingsRequest).addOnCompleteListener(task -> {
            try {
                task.getResult(ApiException.class);
                // Location settings are satisfied
                try {
                    // Gets Last Known Location and Time
                    LocationServices.getFusedLocationProviderClient(this).getLastLocation().addOnSuccessListener(l -> onLocationChanged(l, showFeedback));
                    //Start the Location Listening
                    startLocationUpdates();
                } catch (SecurityException ignored) {
                }
            } catch (ApiException ex) {
                switch (ex.getStatusCode()) {
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        // Location settings are not satisfied. But could be fixed by showing the user a dialog
                        try {
                            // Show the dialog by calling startResolutionForResult(),
                            // and check the result in onActivityResult().
                            // Cast to resolvable
                            ResolvableApiException resolvable = (ResolvableApiException) ex;
                            resolvable.startResolutionForResult(MainActivity.this, REQUEST_CHECK_SETTINGS);
                        } catch (IntentSender.SendIntentException | ClassCastException e) {
                            // Ignore the error.
                            e.printStackTrace();
                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        // Location settings are not satisfied. However, we have no way to fix
                        // the settings so we wont show the dialog
                        break;
                }
            }
        });
    }

    /**
     * Starts the Location Updates
     */
    public void startLocationUpdates() {
        // Check if app has all permissions needed and ask if not
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION, android.Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_LOCATION_REQUEST_CODE);
            return;
        }
        LocationServices.getFusedLocationProviderClient(this).requestLocationUpdates(locationRequest, locationCallback, null);
    }

    /**
     * Stop the Location Updates
     */
    private void stopLocationUpdates() {
        LocationServices.getFusedLocationProviderClient(this).removeLocationUpdates(locationCallback);
    }

    /**
     * Update the current Location, LastUpdate Time and try to resolve the City
     *
     * @param location New Location
     */
    private void onLocationChanged(Location location, boolean showFeedback) {
        if (isBetterLocation(location)) {
            currentLocation = location;
            getCityName();

            if (showFeedback) {
                bottomBar.setSelectedItemId(R.id.bottom_feedback);
            }
        }
    }

    /**
     * Determines whether one Location reading is better than the current Location fix
     * <p>
     * https://developer.android.com/guide/topics/location/strategies
     *
     * @param location The new Location that you want to evaluate
     */
    protected boolean isBetterLocation(Location location) {
        if (currentLocation == null) {
            // A new location is always better than no location
            return true;
        }

        // Check whether the new location fix is newer or older
        long timeDelta = location.getTime() - currentLocation.getTime();
        int twoMinutes = 1000 * 60 * 2;
        boolean isSignificantlyNewer = timeDelta > twoMinutes;
        boolean isSignificantlyOlder = timeDelta < -twoMinutes;
        boolean isNewer = timeDelta > 0;

        // If it's been more than two minutes since the current location, use the new location
        // because the user has likely moved
        if (isSignificantlyNewer) {
            return true;
            // If the new location is more than two minutes older, it must be worse
        } else if (isSignificantlyOlder) {
            return false;
        }

        // Check whether the new location fix is more or less accurate
        int accuracyDelta = (int) (location.getAccuracy() - currentLocation.getAccuracy());
        boolean isLessAccurate = accuracyDelta > 0;
        boolean isMoreAccurate = accuracyDelta < 0;
        boolean isSignificantlyLessAccurate = accuracyDelta > 50;

        // Determine location quality using a combination of timeliness and accuracy
        if (isMoreAccurate) {
            return true;
        } else if (isNewer && !isLessAccurate) {
            return true;
        } else if (isNewer && !isSignificantlyLessAccurate) {
            return true;
        }
        return false;
    }

    /**
     * Switch to Error Fragment if there is an Error with Location Access
     */
    private void switchToErrorFragment() {
        FragmentTransaction rm = getSupportFragmentManager().beginTransaction();
        for (Fragment old : getSupportFragmentManager().getFragments()) {
            rm.hide(old);
        }

        Fragment fragment = getSupportFragmentManager().findFragmentByTag("ERROR");
        if (fragment == null) {
            rm.add(R.id.main_frame, new LocationErrorFragment(), "ERROR");
        } else {
            rm.show(fragment);
        }
        rm.commit();
    }

    /**
     * Get Saved Values
     *
     * @param savedInstanceState Saved Variable States
     */
    private void updateValuesFromBundle(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            // Update the value of mRequestingLocationUpdates from the Bundle
            if (savedInstanceState.keySet().contains(REQUESTING_LOCATION_UPDATES_KEY)) {
                mRequestingLocationUpdates = savedInstanceState.getBoolean(
                        REQUESTING_LOCATION_UPDATES_KEY);
            }

            // Update the value of location from the Bundle and update the
            // UI to show the correct latitude and longitude.
            if (savedInstanceState.keySet().contains(LOCATION_KEY)) {
                currentLocation = savedInstanceState.getParcelable(LOCATION_KEY);
            }

            if (savedInstanceState.keySet().contains(CURRENT_BOTTOM_TAB_SELECTED_KEY)) {
                mBottomBarPosition = savedInstanceState.getInt(CURRENT_BOTTOM_TAB_SELECTED_KEY);
            }
        }
    }

    /**
     * Override of onRequestPermission
     *
     * @param requestCode  Request of Permission
     * @param permissions  Requested Permissions
     * @param grantResults Results
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSION_LOCATION_REQUEST_CODE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //Check if App has Hardware Permissions
                    if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION, android.Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_LOCATION_REQUEST_CODE);
                        return;
                    }
                    //Start Location Listening
                    makeLocationInit(false);
                } else {
                    //Switches To Error Fragment because Permission for Location was Denied
                    switchToErrorFragment();
                }
            }
        }
    }

    /**
     * Override of onActivityResult
     *
     * @param requestCode Request Code
     * @param resultCode  Result Code
     * @param data        Extra Data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CHECK_SETTINGS:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        //Start Location Listening
                        makeLocationInit(true);
                        break;
                    case Activity.RESULT_CANCELED:
                        //Switch to Error
                        stopLocationUpdates();
                        switchToErrorFragment();
                        break;
                    default:
                        break;
                }
                break;
        }
    }

    /**
     * Bottom Bar Select Listener
     *
     * @param item Selected
     * @return Success of Selection
     */
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        Fragment fragment;

        FragmentTransaction rm = getSupportFragmentManager().beginTransaction();
        for (Fragment old : getSupportFragmentManager().getFragments()) {
            rm.hide(old);
        }

        switch (item.getItemId()) {
            case R.id.bottom_feedback:
                fragment = getSupportFragmentManager().findFragmentByTag("SEND");
                if (fragment == null) {
                    rm.add(R.id.main_frame, new FeedbackSendFragment(), "SEND");
                } else {
                    rm.show(fragment);
                }
                break;
            case R.id.bottom_map:
                fragment = getSupportFragmentManager().findFragmentByTag("MAP");
                if (fragment == null) {
                    rm.add(R.id.main_frame, new MapFragment(), "MAP");
                } else {
                    rm.show(fragment);
                }
                break;
            case R.id.bottom_profile:
                fragment = getSupportFragmentManager().findFragmentByTag("FEEDBACKS");
                if (fragment == null) {
                    rm.add(R.id.main_frame, new FeedbacksFragment(), "FEEDBACKS");
                } else {
                    rm.show(fragment);
                }
                break;
        }
        if (currentLocation == null) {
            createToast(getString(R.string.noLocation), Toast.LENGTH_SHORT);
        } else if (!Profile.isAuth()) {
            createToast(getString(R.string.noAuth), Toast.LENGTH_SHORT);
        } else {
            rm.commit();
            item.setChecked(true);
        }

        return false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.appbar, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //Resets the Toolbar to Expanded
        //((AppBarLayout) findViewById(R.id.main_appbar)).setExpanded(true);

        Intent intent = null;

        // Handle navigation view item clicks here.
        switch (item.getItemId()) {
            case R.id.profile:
                if (!Profile.isAuth()) {
                    createToast(getString(R.string.noAuth), Toast.LENGTH_SHORT);
                } else {
                    intent = new Intent(this, ProfileActivity.class);
                }
                break;
            case R.id.about:
                intent = new Intent(this, AboutActivity.class);
                break;
        }

        if (intent != null) {
            startActivity(intent);
        }

        return false;
    }

    /**
     * Save Values to Bundle
     *
     * @param savedInstanceState Bundle to Save To
     */
    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putBoolean(REQUESTING_LOCATION_UPDATES_KEY, mRequestingLocationUpdates);
        savedInstanceState.putParcelable(LOCATION_KEY, currentLocation);
        savedInstanceState.putInt(CURRENT_BOTTOM_TAB_SELECTED_KEY, mBottomBarPosition);
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!Profile.isAuth()) {
            authFirebase();
        }
    }
}
