package de.hhu.cs.feedbackr.view;


import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
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
import de.hhu.cs.feedbackr.model.FirebaseHelper;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;


/**
 * A Fragment to Display A Map in the MainActivity
 */
public class MapFragment extends Fragment implements GoogleMap.OnMarkerClickListener {

    private static final String PERSONAL_PREFERENCE_KEY = "personal_marker";
    private static final String PUBLIC_PREFERENCE_KEY = "public_marker";

    private MapView mMapView;
    private View mView;
    private HashMap<String, Marker> mPersonalMarkers;
    private HashMap<String, Marker> mPublicMarkers;

    private FloatingActionButton mPersonalFAB;
    private FloatingActionButton mPublicFAB;

    private boolean mShowPersonal;
    private boolean mShowPublic;

    private ChildEventListener mPersonalListener;
    private ChildEventListener mPublicListener;

    private GoogleMap mGoogleMap;

    /**
     * Use MapFragment.newInstance() Instead
     */
    public MapFragment() {
        // Required empty public constructor
    }

    /**
     * Creates a new MapFragment
     *
     * @return MapFragment
     */
    public static MapFragment newInstance() {
        return new MapFragment();
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
        mView = inflater.inflate(R.layout.fragment_map, container, false);


        mPersonalMarkers = new HashMap<>();
        mPublicMarkers = new HashMap<>();

        initFABs();

        mMapView = (MapView) mView.findViewById(R.id.map_map);
        mMapView.onCreate(savedInstanceState);
        mMapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(final GoogleMap googleMap) {
                mGoogleMap = googleMap;

                if (mShowPersonal) {
                    addPersonalFeedbackListener();
                }
                if (mShowPublic) {
                    addPublishedFeedbackListener();
                }

                //Show FABs Here to prevent Errors resulting from trying to add Markers on not Loaded Map
                mPersonalFAB.setVisibility(View.VISIBLE);
                mPublicFAB.setVisibility(View.VISIBLE);

                if (ActivityCompat.checkSelfPermission(getActivity(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getActivity(), android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                    return;
                }
                googleMap.setMyLocationEnabled(true);
                Location location = ((MainActivity) getActivity()).getCurrentLocation();
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), 15));
                mMapView.onResume();
            }
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
        mShowPublic = sharedPref.getBoolean(PUBLIC_PREFERENCE_KEY, false);

        mPersonalFAB = (FloatingActionButton) mView.findViewById(R.id.map_fab_personal);
        mPersonalFAB.setImageResource(mShowPersonal ? R.drawable.ic_profile_black_24dp : R.drawable.ic_profile_disabled_black_24dp);
        mPersonalFAB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String showHide;
                mShowPersonal = !mShowPersonal;
                mPersonalFAB.setImageResource(mShowPersonal ? R.drawable.ic_profile_black_24dp : R.drawable.ic_profile_disabled_black_24dp);
                editor.putBoolean(PERSONAL_PREFERENCE_KEY, mShowPersonal).apply();
                if (mShowPersonal) {
                    addPersonalFeedbackListener();
                    showHide = getString(R.string.show);
                } else {
                    stopPersonalListener();
                    showHide = getString(R.string.hide);
                }
                ((MainActivity) getActivity()).createToast(String.format(getString(R.string.show_hide_private), showHide), Toast.LENGTH_SHORT);
            }
        });
        mPersonalFAB.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                ((MainActivity) getActivity()).createToast(String.format(getString(R.string.show_hide_private), getString(R.string.show_hide)), Toast.LENGTH_SHORT);
                return true;
            }
        });

        mPublicFAB = (FloatingActionButton) mView.findViewById(R.id.map_fab_public);
        mPublicFAB.setImageResource(mShowPublic ? R.drawable.ic_public_black_24dp : R.drawable.ic_public_disabled_black_24dp);
        mPublicFAB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
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
            }
        });
        mPublicFAB.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                ((MainActivity) getActivity()).createToast(String.format(getString(R.string.show_hide_public), getString(R.string.show_hide)), Toast.LENGTH_SHORT);
                return true;
            }
        });
    }

    /**
     * Stops Personal Listener
     */
    private void stopPersonalListener() {
        removeMarkers(mPersonalMarkers);
        mPersonalMarkers = new HashMap<>();
        DatabaseReference userRef = FirebaseHelper.getUserRef();
        if (userRef != null) {
            userRef.removeEventListener(mPersonalListener);
        }
    }

    /**
     * Stops Published Listener
     */
    private void stopPublishedListener() {
        removeMarkers(mPublicMarkers);
        mPublicMarkers = new HashMap<>();
        FirebaseHelper.getPublished().removeEventListener(mPublicListener);
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
                            if (feedback.getCity().equals("")) {

                                // todo NPE for MainActivity.getCityName
                                feedback.setCity(((MainActivity) getContext()).getCityName(feedback.getLatitude(), feedback.getLongitude()));
                                // todo check why do we save here?
                                FirebaseHelper.saveFeedback(feedback);
                            }
                            Marker marker = createMarker(feedback);
                            if (marker != null) {
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
        mPublicListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                Log.i("Child Added Published", dataSnapshot.getKey());
                FirebaseHelper.getFeedback().child(dataSnapshot.getKey()).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        Feedback feedback = dataSnapshot.getValue(Feedback.class);
                        if (feedback != null) {
                            Marker marker = createMarker(feedback);
                            if (marker != null) {
                                mPublicMarkers.put(feedback.getId(), marker);
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
        FirebaseHelper.getPublished().addChildEventListener(mPublicListener);
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
}
