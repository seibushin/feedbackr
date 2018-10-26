package de.hhu.cs.feedbackr.firebase;

import android.content.Context;
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
 * This class provides methods to save and delete the feedback, as well as save profile.
 * To access the content one can get the DatabaseReferences to the different locations
 * and access them as desired.
 * This class also provides a method to create a unique feedback ID.
 * <p>
 * The data is managed in a Firebase database.
 * <p>
 * The structure of the database is as follows:
 * feedbackr-[ID]       (the root node)
 * - feedback           (contains all feedbacks)
 * - - [feedback ID]    (a single feedback)
 * - geofire            (contains geo information for every feedback)
 * - - [feedback ID]    (geo information for the specific feedback ID)
 * - users              (holds the profile information, if a user decides to use a profile)
 * - - [user ID]        (profile for user with the ID)
 * <p>
 * To enforce certain security rules, firebase allows to define rules which allow or restrict
 * access to the information stored in the database. The rules are located in the firebase console
 * <p>
 * The configuration can be found under /app/google-services.json, this file can be downloaded via the
 * Firebase Console (https://console.firebase.google.com/).
 * <p>
 * To allow geo queries Geofire (https://github.com/firebase/geofire-java) is used. Geofire utilizes
 * geohashs to allow queries for a specific location and radius. Further information can be found
 * in the projects github repository.
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
    public static void saveFeedback(Feedback feedback, Context context) {
        Executors.newSingleThreadExecutor().execute(() -> {
            Log.d(FirebaseHelper.class.getName(), "Save Feedback: " + feedback);

            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user == null) {
                Log.e(FirebaseHelper.class.getName(), "User is null");
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
                    Log.e(FirebaseHelper.class.getName(), "There was an error saving the location to GeoFire: " + error.getMessage());
                    Crashlytics.logException(new Throwable("There was an error saving the location to GeoFire: " + error));
                }
            });

            // upload image
            FirebaseStorageHelper.uploadImage(feedback, context);
        });
    }

    /**
     * Saves the profile to Firebase
     *
     * @param profile the users profile
     */
    public static void saveProfile(Profile profile) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null) {
            Log.d(FirebaseHelper.class.getName(), "Save profile for " + user.getUid() + ": " + profile);

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
            Log.e(FirebaseHelper.class.getName(), "User is null");
            return;
        }

        mFeedbackRef.child(feedback.getId()).removeValue();

        // remove geofire
        GeoFire geoFire = new GeoFire(mGeofireRef);
        // this is a fix due to removeLocation(key) not working as intended
        geoFire.removeLocation(feedback.getId(), (p1, p2) -> {
        });

        // delete image
        if (feedback.hasImage()) {
            FirebaseStorageHelper.deleteImage(feedback.getImage());
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

    /**
     * Get the reference to geofire
     *
     * @return firebase geofire reference
     */
    public static GeoFire getGeofire() {
        return geoFire;
    }

    /**
     * get the firebase feedbacks reference
     *
     * @return firebase feedbacks reference
     */
    public static DatabaseReference getFeedbackRef() {
        return mFeedbackRef;
    }

    /**
     * Get the user reference. The child is only available if the user has edited his profile.
     * Otherwise the local installation of the app provides an anonymous user authentication
     * which is used to identify the user
     *
     * @return firebase user reference
     */
    public static DatabaseReference getUserRef() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Log.e(FirebaseHelper.class.getName(), "User is null");
            return null;
        }
        return mUsersRef.child(user.getUid());
    }
}
