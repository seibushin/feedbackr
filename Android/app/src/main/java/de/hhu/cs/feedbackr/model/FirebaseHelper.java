package de.hhu.cs.feedbackr.model;

import android.util.Log;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.location.Geofence;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Created by antonborries on 21/09/16.
 */

public class FirebaseHelper {
    private static final DatabaseReference mRootRef = FirebaseDatabase.getInstance().getReference();
    private static final DatabaseReference mFeedbackRef = mRootRef.child("feedback");
    private static final DatabaseReference mPublishedRef = mRootRef.child("published");
    private static final DatabaseReference mUsersRef = mRootRef.child("users");
    private static final DatabaseReference mGeofireRef = mRootRef.child("geofire");

    /**
     * Saves a Feedback to Firebase
     * @param feedback Feedback to Save
     */
    public static void saveFeedback(Feedback feedback) {
        // check if similar feedback is nearby
        // todo

        // ask the user if it is basically the same
        // todo

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Log.i("NO CURRENT USER TAG", "User is null");
            return;
        }
        //Save to User Section
        mUsersRef.child(user.getUid()).child("feedback").child(feedback.getId()).setValue(feedback.getCategory());
        //Save to Feedback Section
        mFeedbackRef.child(feedback.getId()).setValue(feedback);

        // save geofire Location
        GeoFire geoFire = new GeoFire(mGeofireRef);
        //geoFire.setLocation(feedback.getId(), new GeoLocation(feedback.getLatitude(), feedback.getLongitude()));

        geoFire.setLocation(feedback.getId(), new GeoLocation(feedback.getLatitude(), feedback.getLongitude()), new GeoFire.CompletionListener() {
            @Override
            public void onComplete(String key, DatabaseError error) {
                if (error != null) {
                    System.err.println("There was an error saving the location to GeoFire: " + error);
                } else {
                    System.out.println("Location saved on server successfully!");
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
     * @param feedback Feedback to delete
     */
    public static void deleteFeedback(Feedback feedback) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Log.i("NO CURRENT USER TAG", "User is null");
            return;
        }
        mFeedbackRef.child(feedback.getId()).removeValue();
        if(feedback.isPublished()){
            mPublishedRef.child(feedback.getId()).removeValue();
        }
        mUsersRef.child(user.getUid()).child("feedback").child(feedback.getId()).removeValue();

        // remove geofire
        GeoFire geoFire = new GeoFire(mGeofireRef);
        geoFire.removeLocation(feedback.getId());
    }

    /**
     * Get a Unique Feedback ID
     * @return New Unique ID for a Feedback
     */
    public static String generateFeedbackID() {
        return mFeedbackRef.push().getKey();
    }

    public static DatabaseReference getFeedback() {
        return mFeedbackRef;
    }

    public static List<Feedback> getNearbyFeedback(double lat, double lon) {
        GeoFire geoFire = new GeoFire(mGeofireRef);
        final List<Feedback> list = new ArrayList<>();

        double radius = 1.0;

        GeoQuery geoQuery = geoFire.queryAtLocation(new GeoLocation(lat, lon), radius);
        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                mFeedbackRef.child(key).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        list.add(dataSnapshot.getValue(Feedback.class));

                        System.out.println("Nearby: "  + dataSnapshot.getValue(Feedback.class));
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
            }

            @Override
            public void onKeyExited(String key) {

            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {

            }

            @Override
            public void onGeoQueryReady() {

            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });

        return list;
    }

    public static List<Feedback> getAllFeedback() {
        GeoFire geoFire = new GeoFire(mGeofireRef);

        List<Feedback> list = new ArrayList<>();

        mFeedbackRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Iterator<DataSnapshot> iterator = dataSnapshot.getChildren().iterator();

                // todo try geofire https://github.com/firebase/geofire-java

                while (iterator.hasNext()) {
                    Feedback f = iterator.next().getValue(Feedback.class);
                    System.out.println(f);
                }
                //System.out.println(dataSnapshot.getValue());
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });


        return list;
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
