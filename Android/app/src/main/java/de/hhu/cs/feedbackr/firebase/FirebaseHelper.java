package de.hhu.cs.feedbackr.firebase;

import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.concurrent.Executors;

import de.hhu.cs.feedbackr.model.Feedback;
import de.hhu.cs.feedbackr.model.Profile;

/**
 * Created by antonborries on 21/09/16.
 */

public class FirebaseHelper {
    private static final FirebaseDatabase mDatabase = FirebaseDatabase.getInstance();

    // Static block to set persistence mode to true
    static {
        mDatabase.setPersistenceEnabled(true);
    }

    private static final DatabaseReference mRootRef = mDatabase.getReference();
    private static final DatabaseReference mFeedbackRef = mRootRef.child("feedback");
    private static final DatabaseReference mUsersRef = mRootRef.child("users");
    private static final DatabaseReference mGeofireRef = mRootRef.child("geofire");

    private static final GeoFire geoFire = new GeoFire(mGeofireRef);

    /**
     * Saves a Feedback to Firebase
     *
     * @param feedback Feedback to Save
     */
    public static void saveFeedback(Feedback feedback) {
        Executors.newSingleThreadExecutor().execute(() -> {
            System.out.println("SAVE FEEDBACK");
            System.out.println(feedback);

            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user == null) {
                Log.i("NO CURRENT USER TAG", "User is null");
                Crashlytics.log("User is null");
                return;
            }

            // set owner for the feedback
            feedback.setOwner(user.getUid());

            //Save to Feedback Section
            mFeedbackRef.child(feedback.getId()).setValue(feedback);

            // save geofire Location
            GeoFire geoFire = new GeoFire(mGeofireRef);

            geoFire.setLocation(feedback.getId(), new GeoLocation(feedback.getLatitude(), feedback.getLongitude()), (key, error) -> {
                if (error != null) {
                    Crashlytics.logException(new Throwable("There was an error saving the location to GeoFire: " + error));
                }
            });

            // upload image
            FirebaseStorageHelper.uploadImage(feedback);
        });
    }

    /**
     *
     * @param profile the users profile
     */
    public static void saveProfile(Profile profile) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null) {
            System.out.println("SAVE PROFILE for " + user.getUid());
            System.out.println(profile);

            mUsersRef.child(user.getUid()).setValue(profile);
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

        // remove geofire
        GeoFire geoFire = new GeoFire(mGeofireRef);
        // this is a fix due to removeLocation(key) not working as intended
        geoFire.removeLocation(feedback.getId(), (p1, p2) -> {});

        // delete image
        if (feedback.isHasPhoto()) {
            FirebaseStorageHelper.deleteImage(feedback.getId());
        }
    }

    /**
     * Get a Unique Feedback ID
     *
     * @return New Unique ID for a Feedback
     */
    public static String generateFeedbackID() {
        return mFeedbackRef.push().getKey();
    }

    public static GeoFire getGeofire() {
        return geoFire;
    }

    public static DatabaseReference getFeedbackRef() {
        return mFeedbackRef;
    }

    public static DatabaseReference getUserRef() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Log.i("NO CURRENT USER TAG", "User is null");
            return null;
        }
        return mUsersRef.child(user.getUid());
    }
}
