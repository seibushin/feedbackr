package de.hhu.cs.feedbackr.model;

import android.util.Log;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.crash.FirebaseCrash;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

/**
 * Created by antonborries on 21/09/16.
 */

public class FirebaseHelper {
    private static final FirebaseDatabase mDatabase = FirebaseDatabase.getInstance();

    // Static block so set persistence mode to true
    static {
        mDatabase.setPersistenceEnabled(true);
    }

    private static final DatabaseReference mRootRef = mDatabase.getReference();
    private static final DatabaseReference mFeedbackRef = mRootRef.child("feedback");
    private static final DatabaseReference mPublishedRef = mRootRef.child("published");
    private static final DatabaseReference mUsersRef = mRootRef.child("users");
    private static final DatabaseReference mGeofireRef = mRootRef.child("geofire");

    /**
     * Saves a Feedback to Firebase
     *
     * @param feedback Feedback to Save
     */
    public static void saveFeedback(Feedback feedback) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Log.i("NO CURRENT USER TAG", "User is null");
            FirebaseCrash.log("User is null");
            return;
        }

        //Save to User Section
        mUsersRef.child(user.getUid()).child("feedback").child(feedback.getId()).setValue(feedback.getCategory());
        //Save to Feedback Section
        mFeedbackRef.child(feedback.getId()).setValue(feedback);

        // save geofire Location
        GeoFire geoFire = new GeoFire(mGeofireRef);

        geoFire.setLocation(feedback.getId(), new GeoLocation(feedback.getLatitude(), feedback.getLongitude()), new GeoFire.CompletionListener() {
            @Override
            public void onComplete(String key, DatabaseError error) {
                if (error != null) {
                    Log.e("", "There was an error saving the location to GeoFire: " + error);
                    FirebaseCrash.log("There was an error saving the location to GeoFire: " + error);
                }
            }
        });

        if (feedback.isPublished()) {
            //If Feedback is Published Save it in Public Reference with Category to get a ChangeEvent when the Category is changed
            mPublishedRef.child(feedback.getId()).setValue(feedback.getCategory());
        } else {
            mPublishedRef.child(feedback.getId()).removeValue();
        }
    }

    /**
     * Deletes Feedback from all Firebase Sections
     *
     * @param feedback Feedback to delete
     */
    public static void deleteFeedback(Feedback feedback) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Log.i("NO CURRENT USER TAG", "User is null");
            return;
        }
        mFeedbackRef.child(feedback.getId()).removeValue();
        if (feedback.isPublished()) {
            mPublishedRef.child(feedback.getId()).removeValue();
        }
        mUsersRef.child(user.getUid()).child("feedback").child(feedback.getId()).removeValue();

        // remove geofire
        GeoFire geoFire = new GeoFire(mGeofireRef);
        geoFire.removeLocation(feedback.getId());
    }

    /**
     * Get a Unique Feedback ID
     *
     * @return New Unique ID for a Feedback
     */
    public static String generateFeedbackID() {
        return mFeedbackRef.push().getKey();
    }

    public static DatabaseReference getFeedback() {
        return mFeedbackRef;
    }

    public static DatabaseReference getPublished() {
        return mPublishedRef;
    }

    public static DatabaseReference getUserRef() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Log.i("NO CURRENT USER TAG", "User is null");
            return null;
        }
        return mUsersRef.child(user.getUid());
    }

    public static DatabaseReference getGeofire() {
        return mGeofireRef;
    }
}
