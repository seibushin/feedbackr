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
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;


/**
 * A Fragment to Display A Map in the MainActivity
 */
public class MapFragment extends Fragment implements GoogleMap.OnMarkerClickListener, GoogleMap.OnCameraIdleListener, OnMapReadyCallback {

    private static final String PERSONAL_PREFERENCE_KEY = "personal_marker";
    private static final String PUBLIC_PREFERENCE_KEY = "public_marker";
    private static final String POSTIVE_PREFERENCE_KEY = "positive_marker";
    private static final String NEGATIVE_PREFERENCE_KEY = "positive_marker";

    private static final double MAX_CIRCLE_RADIUS = 3000;

    private MapView mMapView;
    private View view;
    private HashMap<String, Marker> mPersonalMarkers;
    private HashMap<String, Marker> publicMarker;

    // todo think more
    private HashMap<String, Marker> mNegPersonalMarkers;
    private HashMap<String, Marker> mPosPersonalMarkers;
    private HashMap<String, Marker> mNegPublicMarkers;
    private HashMap<String, Marker> mPosPublicMarkers;

    private FloatingActionButton privateFAB;
    private FloatingActionButton publicFAB;
    private FloatingActionButton positiveFAB;
    private FloatingActionButton negativeFAB;
    private FloatingActionButton centerFAB;

    private boolean showPrivate;
    private boolean showPublic;
    private boolean mShowPositive;
    private boolean showNegative;

    private ChildEventListener mPersonalListener;
    private ChildEventListener mPublicListener;

    private GoogleMap googleMap;

    private SharedPreferences.Editor editor;
    private boolean mapLoaded = false;

    public MapFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        System.out.println("CREATE MAP FRAGMENT");
    }

    private Circle searchCircle;
    private GeoQuery query;

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
        view = inflater.inflate(R.layout.fragment_map, container, false);

        mPersonalMarkers = new HashMap<>();
        publicMarker = new HashMap<>();

        getPreferences();
        initFABs();

        mMapView = view.findViewById(R.id.map_map);
        mMapView.onCreate(savedInstanceState);
        mMapView.getMapAsync(this);

        return view;
    }

    private void getPreferences() {
        SharedPreferences sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);
        editor = sharedPref.edit();
        showPrivate = sharedPref.getBoolean(PERSONAL_PREFERENCE_KEY, true);
        showPublic = sharedPref.getBoolean(PUBLIC_PREFERENCE_KEY, true);
        mShowPositive = sharedPref.getBoolean(POSTIVE_PREFERENCE_KEY, true);
        showNegative = sharedPref.getBoolean(NEGATIVE_PREFERENCE_KEY, true);
    }

    /**
     * Initializes the FABs to declare which Feedback should be displayed on the Map
     */
    private void initFABs() {
        privateFAB = view.findViewById(R.id.map_fab_personal);
        privateFAB.setImageResource(showPrivate ? R.drawable.ic_profile_white_24dp : R.drawable.ic_profile_disabled_white_24dp);
        privateFAB.setOnClickListener(v -> showPrivate());

        publicFAB = view.findViewById(R.id.map_fab_public);
        publicFAB.setImageResource(showPublic ? R.drawable.ic_public_black_24dp : R.drawable.ic_public_disabled_black_24dp);
        publicFAB.setOnClickListener(v -> showPublic());

        negativeFAB = view.findViewById(R.id.map_fab_negative);
        positiveFAB = view.findViewById(R.id.map_fab_positive);

        centerFAB = view.findViewById(R.id.btn_center);
        centerFAB.setOnClickListener(v -> updateCenter());
    }

    public void showPublic() {
        if (mapLoaded) {
            String showHide;
            showPublic = !showPublic;
            publicFAB.setImageResource(showPublic ? R.drawable.ic_public_black_24dp : R.drawable.ic_public_disabled_black_24dp);
            editor.putBoolean(PUBLIC_PREFERENCE_KEY, showPublic).apply();
            if (showPublic) {
                // todo show available public feedback
                showHide = getString(R.string.show);
            } else {
                // todo hide available public feedback
                showHide = getString(R.string.hide);
            }
            ((MainActivity) getActivity()).createToast(String.format(getString(R.string.show_hide_public), showHide), Toast.LENGTH_SHORT);
        }
    }

    public void showPrivate() {
        if (mapLoaded) {
            String showHide;
            showPrivate = !showPrivate;
            privateFAB.setImageResource(showPrivate ? R.drawable.ic_profile_white_24dp : R.drawable.ic_profile_disabled_white_24dp);
            editor.putBoolean(PERSONAL_PREFERENCE_KEY, showPrivate).apply();
            if (showPrivate) {
                // todo show available public feedback
                showHide = getString(R.string.show);
            } else {
                // todo show available public feedback
                showHide = getString(R.string.hide);
            }
            ((MainActivity) getActivity()).createToast(String.format(getString(R.string.show_hide_private), showHide), Toast.LENGTH_SHORT);
        }
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
            Marker actMarker = googleMap.addMarker(marker);
            actMarker.setTag(feedback.getId());
            googleMap.setOnMarkerClickListener(this);
            return actMarker;
        }
        return null;
    }

    /**
     * Attaches a listener to the GeoFire query. Which will give all Feedback in a certain area/radius.
     * The returned Feedback keys will be used to get the Feedback.
     */
    private void addFeedbackListener() {
        query.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                // get the actual feedback for the key
                if (publicMarker.containsKey(key)) {
                    publicMarker.get(key).setVisible(true);
                } else {
                    FirebaseHelper.getFeedback().child(key).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            Feedback f = dataSnapshot.getValue(Feedback.class);

                            // check if published or owned
                            if (f != null && (f.isPublished() || f.getOwner().equals(FirebaseAuth.getInstance().getCurrentUser().getUid()))) {
                                Log.d("Nearby-Feedback", f.toString());

                                // todo check for public or personal feedback
                                Marker marker = createMarker(f);
                                if (marker != null) {
                                    publicMarker.put(f.getId(), marker);
                                }
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {
                            System.out.println("ERROR READING " + key);
                        }
                    });
                }
            }

            @Override
            public void onKeyExited(String key) {
                System.out.println("HIDE MARKER " + key);
                try {
                    publicMarker.get(key).setVisible(false);
//                    publicMarker.get(key).remove();
//                    publicMarker.remove(id);
                } catch (NullPointerException npe) {
                    Log.e("MAP", "Marker was null");
                }
            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {
            }

            @Override
            public void onGeoQueryReady() {
            }

            @Override
            public void onGeoQueryError(DatabaseError error) {
                Log.e("GeoFire", error.toString());
            }
        });
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
                System.out.println(feedback.getOwner() + FirebaseAuth.getInstance().getCurrentUser().getUid());
                DialogFragment dialog = FeedbackDialog.newInstance(feedback, feedback.getOwner().equals(FirebaseAuth.getInstance().getCurrentUser().getUid()));
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

    private double zoomLevelToRadius(double zoom) {
        // estimated value 2^14 * 1000
        // fills almost the screen at 96dpi
        // this is equal to 1000 meter at zoom level 15
        return 16384000 / Math.pow(2, zoom);
    }

    private void updateCenter() {
        if (mapLoaded) {
            // update center
            LatLng center = googleMap.getCameraPosition().target;
            searchCircle.setCenter(center);
            query.setCenter(new GeoLocation(center.latitude, center.longitude));
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.googleMap = googleMap;

        Location location = ((MainActivity) getActivity()).getCurrentLocation();
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

        searchCircle = this.googleMap.addCircle(new CircleOptions().center(latLng).radius(1000));
        searchCircle.setFillColor(ContextCompat.getColor(getContext(), R.color.mapCircleFill));
        searchCircle.setStrokeColor(ContextCompat.getColor(getContext(), R.color.mapCircleStroke));
        this.googleMap.setOnCameraIdleListener(this);

        GeoFire geoFire = new GeoFire(FirebaseHelper.getGeofire());
        query = geoFire.queryAtLocation(new GeoLocation(location.getLatitude(), location.getLongitude()), 1.0);

        addFeedbackListener();

        // todo think more
        if (showPrivate) {

        }
        if (showPublic) {

        }
        if (showNegative) {
            // todo
        }
        if (mShowPositive) {
            // todo
        }

        if (ActivityCompat.checkSelfPermission(getActivity(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getActivity(), android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        googleMap.setMyLocationEnabled(true);

        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), 15));
        mMapView.onResume();

        mapLoaded = true;
    }

    @Override
    public void onCameraIdle() {
        double radius = zoomLevelToRadius(googleMap.getCameraPosition().zoom);
        if (radius > MAX_CIRCLE_RADIUS) radius = MAX_CIRCLE_RADIUS;
        searchCircle.setRadius(radius);
        // radius in kilometer
        query.setRadius(radius / 1000);
    }
}
