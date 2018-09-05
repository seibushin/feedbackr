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
import android.support.annotation.Nullable;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.crash.FirebaseCrash;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.io.IOException;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import de.hhu.cs.feedbackr.R;
import de.hhu.cs.feedbackr.firebase.FirebaseHelper;
import de.hhu.cs.feedbackr.model.Feedback;
import de.hhu.cs.feedbackr.model.Profile;
import de.hhu.cs.feedbackr.view.fragment.FeedbackSendFragment;
import de.hhu.cs.feedbackr.view.fragment.FeedbacksFragment;
import de.hhu.cs.feedbackr.view.fragment.LocationErrorFragment;
import de.hhu.cs.feedbackr.view.fragment.MapFragment;

public class MainActivity extends AppCompatActivity

        implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener,
        BottomNavigationView.OnNavigationItemSelectedListener {

    private static final int PERMISSION_LOCATION_REQUEST_CODE = 0;
    private static final String REQUESTING_LOCATION_UPDATES_KEY = "Requesting Location";
    private static final String LOCATION_KEY = "Location";
    private static final String LAST_UPDATED_TIME_STRING_KEY = "Last Updated Time";
    private static final int REQUEST_CHECK_SETTINGS = 1;
    private static final String CURRENT_BOTTOM_TAB_SELECTED_KEY = "Current Bottom Bar Tab";

    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;

    private int mBottomBarPosition;

    private Location mCurrentLocation;
    private Calendar mLastUpdateTime;
    private String mCurrentCity = "";
    private boolean mRequestingLocationUpdates;

    private boolean mLocationError;

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

        //Authenticates User Anonymously for Firebase Rules
        final FirebaseAuth auth = FirebaseAuth.getInstance();

        //Initializes the Bottom Bar
        bottomBar = findViewById(R.id.bottom_navigation);
        bottomBar.setOnNavigationItemSelectedListener(this);

        getSupportFragmentManager().beginTransaction().add(R.id.main_frame, new FeedbackSendFragment(), "SEND").commit();

        auth.signInAnonymously()
                .addOnCompleteListener(this, task -> {
                    if (!task.isSuccessful()) {
                        System.out.println("LOGIN FAILED");
                        // sign in failed
                        createToast(getString(R.string.auth_failed), Toast.LENGTH_LONG);
                        FirebaseCrash.report(new Throwable("Authentication failed - " + task.getException()));
                        //todo disable send feedback as long as the user is not authenificated
                    } else {
                        getProfile();
                    }
                });

        buildGoogleApiClient();

        updateValuesFromBundle(savedInstanceState);
    }

    private BottomNavigationView bottomBar;

    private void getProfile() {
        // todo create isNew boolean instead and use empty profile for the initial object
        if (Profile.getInstance() == null) {
            FirebaseHelper.getProfileRef().addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    Profile.setInstanceProfile(dataSnapshot.getValue(Profile.class));
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    System.out.println("The read failed: " + databaseError.getCode());
                }
            });
        }
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
                intent = new Intent(this, ProfileActivity.class);
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
     * Sets up the Google Api Client
     */
    private synchronized void buildGoogleApiClient() {
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
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
        if (mCurrentLocation == null) {
            createToast(getString(R.string.noLocation), Toast.LENGTH_LONG);
            FirebaseCrash.report(new Throwable("Try to sendFeedback but currentLocation is null"));
        } else {
            // todo change to -> disable feedback button till location available?
            Feedback feedback = new Feedback(mCurrentLocation, mLastUpdateTime, mCurrentCity, kind, feedbackId);

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
            List<Address> addresses = gcd.getFromLocation(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude(), 1);
            if (addresses != null) {
                mCurrentCity = addresses.get(0).getLocality();
            }
        } catch (IOException ignored) {
        }
    }

    /**
     * Gets the City Name of a Location
     *
     * @param latitude  Latitude of Place
     * @param longitude Longitude of Place
     * @return String of City Name if Successful, Empty String Otherwise
     */
    public String getCityName(double latitude, double longitude) {
        Geocoder gcd = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = gcd.getFromLocation(latitude, longitude, 1);
            if (addresses != null) {
                return addresses.get(0).getLocality();
            }
        } catch (IOException ignored) {
            ignored.printStackTrace();
        }
        return "";
    }

    /**
     * Override of onStart
     * Connects GoogleApiClient
     */
    @Override
    protected void onStart() {
        mGoogleApiClient.connect();
        super.onStart();
    }

    /**
     * Override of onStop
     * Disconnects GoogleApiClient
     */
    @Override
    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    /**
     * Override of onConnected
     * Handles when GoogleApiClient is Connected
     *
     * @param bundle bundle
     */
    @Override
    public void onConnected(@Nullable Bundle bundle) {
        //Set Up Location Listening
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(LocationRequest.create().setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY));
        Task<LocationSettingsResponse> result = LocationServices.getSettingsClient(this).checkLocationSettings(builder.build());

        result.addOnCompleteListener(task -> {
            try {
                task.getResult(ApiException.class);
                // Location settings are satisfied
                makeLocationInit();
            } catch (ApiException ex) {
                switch (ex.getStatusCode()) {
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        // Location settings are not satisfied. But could be fixed by showing the user
                        // a dialog
                        try {
                            // Show the dialog by calling startResolutionForResult(),
                            // and check the result in onActivityResult().
                            // Cast to resolvable
                            ResolvableApiException resolvable = (ResolvableApiException) ex;
                            resolvable.startResolutionForResult(MainActivity.this, REQUEST_CHECK_SETTINGS);
                        } catch (IntentSender.SendIntentException | ClassCastException e) {
                            // Ignore the error.
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
     * Initialises the Location listening
     */
    private void makeLocationInit() {
        try {
            // Gets Last Known Location and Time
            // todo switch to FusedLocationProviderClient with Google Play Services 12.0.0 as stated in the docs
            mCurrentLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            mLastUpdateTime = Calendar.getInstance();
            //Start the Location Listening
            createLocationRequest();
            if (mRequestingLocationUpdates) {
                startLocationUpdates();
            }
        } catch (SecurityException ignored) {
        }
    }

    /**
     * Creates the Location Request
     */
    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        //get new Location every 5-10 Seconds
        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(5000);
        //Use highest Accuracy Possible
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(mLocationRequest).setAlwaysShow(true);
        builder.build();
        mRequestingLocationUpdates = true;
    }

    /**
     * Starts the Location Updates
     */
    protected void startLocationUpdates() {
        //Check if App has all Permissions needed and ask if not
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION, android.Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_LOCATION_REQUEST_CODE);
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }

    /**
     * Stop the Location Updates
     */
    protected void stopLocationUpdates() {
        // Check if client is connected
        if (mGoogleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(
                    mGoogleApiClient, this);
        }
        mRequestingLocationUpdates = false;
    }

    /**
     * Update the current Location, LastUpdate Time and try to resolve the City
     *
     * @param location New Location
     */
    @Override
    public void onLocationChanged(Location location) {
        mCurrentLocation = location;
        mLastUpdateTime = Calendar.getInstance();
        getCityName();
    }

    /**
     * Switch to Error Fragment if there is an Error with Location Access
     */
    private void switchToErrorFragment() {
        mLocationError = true;

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
     * Override of onConnectionSuspended
     *
     * @param i Overridden Int
     */
    @Override
    public void onConnectionSuspended(int i) {
        //Reconnect the GoogleApiClient
        mGoogleApiClient.connect();
    }

    /**
     * Override of onConnectionFailed
     *
     * @param connectionResult Connection Result
     */
    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.e("Connection error", connectionResult.getErrorMessage());
    }

    /**
     * Override of onPause
     */
    @Override
    protected void onPause() {
        super.onPause();
        //Stops Location Updates when App is not running
        if (mGoogleApiClient.isConnected()) {
            stopLocationUpdates();
        }
    }

    /**
     * Override of onResume
     */
    @Override
    public void onResume() {
        super.onResume();
        //If ApiClient is not Connected
        if (mGoogleApiClient.isConnected() && !mRequestingLocationUpdates) {
            mRequestingLocationUpdates = true;
            startLocationUpdates();
        }
    }

    /**
     * Save Values to Bundle
     *
     * @param savedInstanceState Bundle to Save To
     */
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putBoolean(REQUESTING_LOCATION_UPDATES_KEY,
                mRequestingLocationUpdates);
        savedInstanceState.putParcelable(LOCATION_KEY, mCurrentLocation);
        savedInstanceState.putSerializable(LAST_UPDATED_TIME_STRING_KEY, mLastUpdateTime);
        savedInstanceState.putInt(CURRENT_BOTTOM_TAB_SELECTED_KEY, mBottomBarPosition);
        super.onSaveInstanceState(savedInstanceState);
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

            // Update the value of mCurrentLocation from the Bundle and update the
            // UI to show the correct latitude and longitude.
            if (savedInstanceState.keySet().contains(LOCATION_KEY)) {
                mCurrentLocation = savedInstanceState.getParcelable(LOCATION_KEY);
            }

            // Update the value of mLastUpdateTime from the Bundle
            if (savedInstanceState.keySet().contains(LAST_UPDATED_TIME_STRING_KEY)) {
                mLastUpdateTime = (Calendar) savedInstanceState.getSerializable(
                        LAST_UPDATED_TIME_STRING_KEY);
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
                    mCurrentLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
                    createLocationRequest();
                    if (mRequestingLocationUpdates) {
                        startLocationUpdates();
                    }
                } else {
                    //Switches To Error Fragment because Permission for Location was Denied
                    switchToErrorFragment();
                    Log.i("PERMISSION DENIED", "Location Permission was denied");
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
                        makeLocationInit();

                        if (mLocationError) {
                            mLocationError = false;

                            // switch to Feedback buttons
                            bottomBar.setSelectedItemId(R.id.bottom_feedback);
                        }
                        break;
                    case Activity.RESULT_CANCELED:
                        //Switch to Error
                        stopLocationUpdates();
                        mGoogleApiClient.disconnect();
                        switchToErrorFragment();
                        break;
                    default:
                        break;
                }
                break;
        }
    }

    /**
     * Retry to Connect the GoogleApiClient
     */
    public void retryConnection() {
        if (mGoogleApiClient != null) {
            mGoogleApiClient.connect();
        }
    }

    /**
     * Get Last Location Saved
     *
     * @return Last Saved Location
     */
    public Location getCurrentLocation() {
        return mCurrentLocation;
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
        rm.commit();

        item.setChecked(true);
        return false;
    }
}
