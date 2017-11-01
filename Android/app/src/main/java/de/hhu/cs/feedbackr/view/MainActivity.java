package de.hhu.cs.feedbackr.view;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import de.hhu.cs.feedbackr.R;
import de.hhu.cs.feedbackr.model.Feedback;
import de.hhu.cs.feedbackr.model.FirebaseHelper;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.io.IOException;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

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
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //Authenticates User Anonymously for Firebase Rules
        final FirebaseAuth auth = FirebaseAuth.getInstance();

        // Todo: Problems with the network connection might result in a failure trying to auth...
        if (auth.getCurrentUser() == null) {
            auth.signInAnonymously();
        }

        // todo replace above with following snipet from the docs
        /*
        FirebaseUser currentUser = auth.getCurrentUser();

        auth.signInAnonymously()
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d("AUTH", "signInAnonymously:success");
                            FirebaseUser user = auth.getCurrentUser();
                            System.out.println(user);

                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w("AUTH", "signInAnonymously:failure", task.getException());
                            Toast.makeText(MainActivity.this, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();


                        }

                        // ...
                    }
                });

         */

        //Coloring the StatusBar if Version is New Enough
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.colorPrimaryDark));
        }

        //Shows the Button Fragment
        getSupportFragmentManager().beginTransaction().add(R.id.main_frame, FeedbackSendFragment.newInstance()).commit();

        buildGoogleApiClient();

        updateValuesFromBundle(savedInstanceState);

        //Initializes the Bottom Bar
        BottomNavigationView bottomBar = (BottomNavigationView) findViewById(R.id.bottom_navigation);
        bottomBar.setOnNavigationItemSelectedListener(this);
        MenuItem selectItem = bottomBar.getMenu().findItem(mBottomBarPosition);
        onNavigationItemSelected(selectItem != null ? selectItem : bottomBar.getMenu().getItem(0));
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
        Feedback feedback = new Feedback(mCurrentLocation, mLastUpdateTime, mCurrentCity, kind, feedbackId);
        // Saves it in Firebase
        FirebaseHelper.saveFeedback(feedback);
        //Show Success Toast
        createToast(String.format(getString(R.string.feedback_send),
                feedback.isPositive() ? getString(R.string.positive) : getString(R.string.negative)), Toast.LENGTH_LONG);
        //Switch to Edit View
        switchToFeedbackDetail(feedback);
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
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(LocationRequest.create().setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY));
        PendingResult<LocationSettingsResult> result = LocationServices.SettingsApi.checkLocationSettings(mGoogleApiClient, builder.build());
        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(@NonNull LocationSettingsResult result) {
                final Status status = result.getStatus();
                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.SUCCESS:
                        //Can Start Location Initialisation
                        makeLocationInit();
                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        // Location settings are not satisfied. But could be fixed by showing the user
                        // a dialog.
                        try {
                            // Show the dialog by calling startResolutionForResult(),
                            // and check the result in onActivityResult().
                            status.startResolutionForResult(
                                    MainActivity.this,
                                    REQUEST_CHECK_SETTINGS);
                        } catch (IntentSender.SendIntentException e) {
                            // Ignore the error.
                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:

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
            //Gets Last Known Location and Time
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
        LocationServices.FusedLocationApi.removeLocationUpdates(
                mGoogleApiClient, this);
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
        Fragment fragment = LocationErrorFragment.newInstance();
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.main_frame, fragment);
        ft.commit();
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
                        mLocationError = false;
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
        //Resets the Toolbar to Expanded
        ((AppBarLayout) findViewById(R.id.main_appbar)).setExpanded(true);
        mBottomBarPosition = item.getItemId();
        if (!mLocationError) {
            Fragment fragment = null;
            switch (item.getItemId()) {
                case R.id.bottom_feedback:
                    //Switch to Feedback Buttons
                    fragment = FeedbackSendFragment.newInstance();
                    break;
                case R.id.bottom_map:
                    //Switch to Map Fragment
                    fragment = MapFragment.newInstance();
                    break;
                case R.id.bottom_profile:
                    //Switch to Profile Fragment
                    fragment = ProfileFragment.newInstance();
                    break;
            }
            if (fragment != null) {
                //Switch to Wanted Fragment
                FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                ft.replace(R.id.main_frame, fragment);
                ft.commit();
                return true;
            }
        }
        return false;
    }

}