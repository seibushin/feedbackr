package de.hhu.cs.feedbackr.view.fragment;


import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import de.hhu.cs.feedbackr.R;
import de.hhu.cs.feedbackr.model.CategoryConverter;
import de.hhu.cs.feedbackr.model.Feedback;
import de.hhu.cs.feedbackr.firebase.FirebaseHelper;
import de.hhu.cs.feedbackr.view.activity.MainActivity;
import de.hhu.cs.feedbackr.view.dialog.FeedbackDialog;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.crash.FirebaseCrash;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Executors;


/**
 * A Fragment to Display A Map in the MainActivity
 */
public class MapFragment extends Fragment implements GoogleMap.OnMarkerClickListener {

    private static final String PERSONAL_PREFERENCE_KEY = "personal_marker";
    private static final String PUBLIC_PREFERENCE_KEY = "public_marker";
    private static final String POSTIVE_PREFERENCE_KEY = "positive_marker";
    private static final String NEGATIVE_PREFERENCE_KEY = "positive_marker";

    private MapView mMapView;
    private View mView;
    private HashMap<String, Marker> mPersonalMarkers;
    private HashMap<String, Marker> mPublicMarkers;

    // todo think more
    private HashMap<String, Marker> mNegPersonalMarkers;
    private HashMap<String, Marker> mPosPersonalMarkers;
    private HashMap<String, Marker> mNegPublicMarkers;
    private HashMap<String, Marker> mPosPublicMarkers;

    private FloatingActionButton mPersonalFAB;
    private FloatingActionButton mPublicFAB;
    private FloatingActionButton mPositiveFAB;
    private FloatingActionButton mNegativeFAB;

    private boolean mShowPersonal;
    private boolean mShowPublic;
    private boolean mShowPositive;
    private boolean mShowNegative;

    private ChildEventListener mPersonalListener;
    private ChildEventListener mPublicListener;

    private GoogleMap mGoogleMap;

    public MapFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        System.out.println("CREATE MAP FRAGMENT");
    }

    /**
     * Creates The View
     *
     * @param inflater           Inflater
     * @param container          Container
     * @param savedInstanceState Saved Instance
     * @return View of the Fragment
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        System.out.println("CREATE MAP FRAGMENT VIEW");
        mView = inflater.inflate(R.layout.fragment_map, container, false);

        mPersonalMarkers = new HashMap<>();
        mPublicMarkers = new HashMap<>();

        initFABs();

        mMapView = mView.findViewById(R.id.map_map);
        mMapView.onCreate(savedInstanceState);
        mMapView.getMapAsync(googleMap -> {
            mGoogleMap = googleMap;

            // todo think more
            if (mShowPersonal) {
                addPersonalFeedbackListener();
            }
            if (mShowPublic) {
                addPublishedFeedbackListener();
            }
            if (mShowNegative) {
                // todo
            }
            if (mShowPositive) {
                // todo
            }

            //Show FABs Here to prevent Errors resulting from trying to add Markers on not Loaded Map
            mPersonalFAB.setVisibility(View.VISIBLE);
            mPublicFAB.setVisibility(View.VISIBLE);
            mNegativeFAB.setVisibility(View.VISIBLE);
            mPositiveFAB.setVisibility(View.VISIBLE);

            if (ActivityCompat.checkSelfPermission(getActivity(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getActivity(), android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            googleMap.setMyLocationEnabled(true);
            Location location = ((MainActivity) getActivity()).getCurrentLocation();
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), 15));
            mMapView.onResume();
        });

        return mView;
    }

    /**
     * Initializes the FABs to declare which Feedback should be displayed on the Map
     */
    private void initFABs() {
        SharedPreferences sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);
        final SharedPreferences.Editor editor = sharedPref.edit();
        mShowPersonal = sharedPref.getBoolean(PERSONAL_PREFERENCE_KEY, true);
        mShowPublic = sharedPref.getBoolean(PUBLIC_PREFERENCE_KEY, true);
        mShowPositive = sharedPref.getBoolean(POSTIVE_PREFERENCE_KEY, true);
        mShowNegative = sharedPref.getBoolean(NEGATIVE_PREFERENCE_KEY, true);

        mPersonalFAB = mView.findViewById(R.id.map_fab_personal);
        mPersonalFAB.setImageResource(mShowPersonal ? R.drawable.ic_profile_white_24dp : R.drawable.ic_profile_disabled_white_24dp);
        mPersonalFAB.setOnClickListener(view -> {
            String showHide;
            mShowPersonal = !mShowPersonal;
            mPersonalFAB.setImageResource(mShowPersonal ? R.drawable.ic_profile_white_24dp : R.drawable.ic_profile_disabled_white_24dp);
            editor.putBoolean(PERSONAL_PREFERENCE_KEY, mShowPersonal).apply();
            if (mShowPersonal) {
                addPersonalFeedbackListener();
                showHide = getString(R.string.show);
            } else {
                stopPersonalListener();
                showHide = getString(R.string.hide);
            }
            ((MainActivity) getActivity()).createToast(String.format(getString(R.string.show_hide_private), showHide), Toast.LENGTH_SHORT);
        });
        mPersonalFAB.setOnLongClickListener(view -> {
            ((MainActivity) getActivity()).createToast(String.format(getString(R.string.show_hide_private), getString(R.string.show_hide)), Toast.LENGTH_SHORT);
            return true;
        });

        mPublicFAB = mView.findViewById(R.id.map_fab_public);
        mPublicFAB.setImageResource(mShowPublic ? R.drawable.ic_public_black_24dp : R.drawable.ic_public_disabled_black_24dp);
        mPublicFAB.setOnClickListener(view -> {
            String showHide;
            mShowPublic = !mShowPublic;
            mPublicFAB.setImageResource(mShowPublic ? R.drawable.ic_public_black_24dp : R.drawable.ic_public_disabled_black_24dp);
            editor.putBoolean(PUBLIC_PREFERENCE_KEY, mShowPublic).apply();
            if (mShowPublic) {
                addPublishedFeedbackListener();
                showHide = getString(R.string.show);
            } else {
                stopPublishedListener();
                showHide = getString(R.string.hide);
            }
            ((MainActivity) getActivity()).createToast(String.format(getString(R.string.show_hide_public), showHide), Toast.LENGTH_SHORT);
        });
        mPublicFAB.setOnLongClickListener(view -> {
            ((MainActivity) getActivity()).createToast(String.format(getString(R.string.show_hide_public), getString(R.string.show_hide)), Toast.LENGTH_SHORT);
            return true;
        });

        mPositiveFAB = mView.findViewById(R.id.map_fab_positive);
        mPositiveFAB.setImageResource(mShowPositive ? R.drawable.ic_thumb_up_white_24dp : R.drawable.ic_thumb_up_disabled_white_24dp);
        mPositiveFAB.setOnClickListener(v -> {
            String showHide;
            mShowPositive = !mShowPositive;
            mPositiveFAB.setImageResource(mShowPositive ? R.drawable.ic_thumb_up_white_24dp : R.drawable.ic_thumb_up_disabled_white_24dp);
            if (mShowNegative) {
                showHide = getString(R.string.show);
                // todo show positive
            } else {
                showHide = getString(R.string.hide);
                // todo hide positive
            }

            // toast
            ((MainActivity) getActivity()).createToast(String.format(getString(R.string.show_hide_postive), showHide), Toast.LENGTH_SHORT);
        });
        // todo long hold#

        // todo pretend we are working on sets

        mNegativeFAB = mView.findViewById(R.id.map_fab_negative);
        mNegativeFAB.setImageResource(mShowNegative ? R.drawable.ic_thumb_down_white_24dp : R.drawable.ic_thumb_down_disabled_white_24dp);
        mNegativeFAB.setOnClickListener(v -> {
            String showHide;
            mShowNegative = !mShowNegative;
            mNegativeFAB.setImageResource(mShowNegative ? R.drawable.ic_thumb_down_white_24dp : R.drawable.ic_thumb_down_disabled_white_24dp);
            if (mShowNegative) {
                showHide = getString(R.string.show);
                // todo show negative
            } else {
                showHide = getString(R.string.hide);
                // todo hide negative
            }

            // show toast
            ((MainActivity) getActivity()).createToast(String.format(getString(R.string.show_hide_negative), showHide), Toast.LENGTH_SHORT);
        });
        // todo long hold
    }

    /**
     * Stops Personal Listener
     */
    private void stopPersonalListener() {
        removeMarkers(mPersonalMarkers);
        mPersonalMarkers = new HashMap<>();
        DatabaseReference userRef = FirebaseHelper.getUserRef();
        if (userRef != null) {
            // todo check dont remove?
            userRef.removeEventListener(mPersonalListener);
        }
    }

    /**
     * Stops Published Listener
     */
    private void stopPublishedListener() {
        removeMarkers(mPublicMarkers);
        mPublicMarkers = new HashMap<>();
        // todo check dont remove?
        FirebaseHelper.getFeedback().removeEventListener(mPublicListener);
    }

    /**
     * Removes a List of Markers on the Map
     *
     * @param map Map of FeedbackID/Markers to Remove
     */
    private void removeMarkers(HashMap<String, Marker> map) {
        for (Marker marker : map.values()) {
            marker.remove();
        }
    }

    /**
     * Creates A Marker for a Feedback
     *
     * @param feedback Feedback the Marker should be for
     * @return Created Marker
     */
    public Marker createMarker(Feedback feedback) {
        LatLng pos = new LatLng(feedback.getLatitude(), feedback.getLongitude());

        Bitmap bmp = getBitmapFromVectorDrawable(CategoryConverter.tagToDrawable(feedback.getCategory()), feedback.isPositive());
        if (bmp != null) {
            MarkerOptions marker = new MarkerOptions().position(pos).icon(BitmapDescriptorFactory.fromBitmap(bmp));
            Marker actMarker = mGoogleMap.addMarker(marker);
            actMarker.setTag(feedback.getId());
            mGoogleMap.setOnMarkerClickListener(this);
            return actMarker;
        }
        return null;
    }

    /**
     * Attaches Listener for User Feedback to Firebase
     */
    private void addPersonalFeedbackListener() {
        mPersonalListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                Log.i("Child Added Tag", dataSnapshot.getKey());
                FirebaseHelper.getFeedback().child(dataSnapshot.getKey()).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        Feedback feedback = dataSnapshot.getValue(Feedback.class);
                        if (feedback != null) {
                            // the feedback has no resolved cityname
                            if (feedback.getCity().equals("")) {
                                // try to get the cityname
                                try {
                                    feedback.setCity(((MainActivity) getContext()).getCityName(feedback.getLatitude(), feedback.getLongitude()));
                                    // save the cityname
                                    FirebaseHelper.saveFeedback(feedback);
                                } catch (NullPointerException npe) {
                                    // unable to get cityname
                                    FirebaseCrash.log("unable to resolve unresolved cityname for " + feedback.getId());
                                    FirebaseCrash.report(npe);
                                }
                            }
                            Marker marker = createMarker(feedback);
                            if (marker != null) {
                                // todo switch for pos neg
                                // check if positive is showing?!
                                mPersonalMarkers.put(feedback.getId(), marker);
                            }
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                String id = dataSnapshot.getKey();
                try {
                    mPersonalMarkers.get(id).remove();
                    mPersonalMarkers.remove(id);
                } catch (NullPointerException npe) {
                    Log.e("MAP", "Marker was null");
                }
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };
        DatabaseReference userRef = FirebaseHelper.getUserRef();
        if (userRef != null) {
            userRef.child("feedback").addChildEventListener(mPersonalListener);
        }
    }

    /**
     * Attaches Listeners For Public Feedback
     */
    private void addPublishedFeedbackListener() {
        GeoFire geoFire = new GeoFire(FirebaseHelper.getGeofire());

        Location l = ((MainActivity) getActivity()).getCurrentLocation();
        GeoQuery query = geoFire.queryAtLocation(new GeoLocation(l.getLatitude(), l.getLongitude()), 1.0);

        mGoogleMap.setOnCameraMoveListener(() -> {
            float f = 15F - mGoogleMap.getCameraPosition().zoom;

            if (f > 0) {
                query.setRadius(1.0*(1+f));
            } else {
                query.setRadius(1.0/(1+f));
            }

            System.out.println(query.getRadius());
        });

        query.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                // get the actual feedback for the key
                FirebaseHelper.getFeedback().child(key).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        // check if published
                        // validate if relevant
                        // type match, title match, desc match ?
                        Feedback f = dataSnapshot.getValue(Feedback.class);
                        if (f != null) {
                            Log.d("Nearby-Feedback", f.toString());
                        }
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
                // initial data has been loaded and all events have been triggered
//                query.removeAllListeners();
            }

            @Override
            public void onGeoQueryError(DatabaseError error) {
                Log.e("GeoFire", error.toString());
            }
        });

        mPublicListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                System.out.println("ADD MARKER " + dataSnapshot.getKey());

                Feedback feedback = dataSnapshot.getValue(Feedback.class);
                if (feedback != null) {
                    Marker marker = createMarker(feedback);
                    if (marker != null) {
                        mPublicMarkers.put(feedback.getId(), marker);
                    }
                }
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                String id = dataSnapshot.getKey();
                System.out.println("REMOVED MARKER " + id);
                try {
                    mPublicMarkers.get(id).remove();
                    mPublicMarkers.remove(id);
                } catch (NullPointerException npe) {
                    Log.e("MAP", "Marker was null");
                }
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };

        FirebaseHelper.getFeedback().orderByChild("published").equalTo(true).addChildEventListener(mPublicListener);
    }

    private List<Feedback> all = new ArrayList<>();
    private List<Feedback> published = new ArrayList<>();


    /**
     * When clicking on a Marker a Dialog should be shown
     *
     * @param marker Marker clicked on
     * @return True when Click Was Successful False otherwise
     */
    @Override
    public boolean onMarkerClick(Marker marker) {
        final String id = (String) marker.getTag();

        if (id == null) {
            return false;
        }
        FirebaseHelper.getFeedback().child(id).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Feedback feedback = dataSnapshot.getValue(Feedback.class);
                DialogFragment dialog = FeedbackDialog.newInstance(feedback, mPersonalMarkers.containsKey(id));
                dialog.show(getFragmentManager(), "FeedbackDialog");
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
        return true;
    }

    /**
     * Mostly From:
     * http://stackoverflow.com/questions/33696488/getting-bitmap-from-vector-drawable
     * 21.12.2016
     *
     * @param drawableId Resource ID
     * @return Bitmap
     */
    public Bitmap getBitmapFromVectorDrawable(int drawableId, boolean isPositive) {
        try {
            Drawable drawable = ContextCompat.getDrawable(getContext(), drawableId);
            DrawableCompat.setTint(drawable, isPositive ? ContextCompat.getColor(getContext(), R.color.green) : ContextCompat.getColor(getContext(), R.color.red));
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                drawable = (DrawableCompat.wrap(drawable)).mutate();
            }

            Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
                    drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            drawable.draw(canvas);

            double scaleFactor = 0.9;
            return Bitmap.createScaledBitmap(bitmap, (int) (bitmap.getWidth() * scaleFactor), (int) (bitmap.getHeight() * scaleFactor), false);
        } catch (NullPointerException npe) {
            Log.e("MAP_FRAGMENT", "Tried to load Image even though this View is not Active");
            return null;
        }
    }

    @Override
    public void onResume() {
        mMapView.onResume();
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mMapView.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mMapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mMapView.onLowMemory();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mMapView.onSaveInstanceState(outState);
    }
}
