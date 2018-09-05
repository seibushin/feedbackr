package de.hhu.cs.feedbackr.firebase;

import android.util.Log;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.crash.FirebaseCrash;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import de.hhu.cs.feedbackr.model.Feedback;
import de.hhu.cs.feedbackr.model.Profile;

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
    private static final DatabaseReference mUsersRef = mRootRef.child("users");
    private static final DatabaseReference mGeofireRef = mRootRef.child("geofire");

    /**
     * Saves a Feedback to Firebase
     *
     * @param feedback Feedback to Save
     */
    public static void saveFeedback(Feedback feedback) {
        System.out.println("SAVE FEEDBACK");
        System.out.println(feedback);

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

        geoFire.setLocation(feedback.getId(), new GeoLocation(feedback.getLatitude(), feedback.getLongitude()), (key, error) -> {
            if (error != null) {
                FirebaseCrash.report(new Throwable("There was an error saving the location to GeoFire: " + error));
            }
        });

        FirebaseStorageHelper.uploadImage(feedback);
    }

    /**
     *
     * @param profile
     */
    public static void saveProfile(Profile profile) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null) {
            System.out.println("SAVE PROFILE for " + user.getUid());
            System.out.println(profile);

            mUsersRef.child(user.getUid()).child("profile").setValue(profile);
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

    public static DatabaseReference getProfileRef() {
        DatabaseReference user = getUserRef();
        if (user != null) {
            System.out.println(user.child("profile").toString());
            return user.child("profile");
        }
        return null;
    }
}
