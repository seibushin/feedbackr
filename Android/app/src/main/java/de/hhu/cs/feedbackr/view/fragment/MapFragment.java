package de.hhu.cs.feedbackr.view.fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.databinding.DataBindingUtil;
import android.graphics.Bitmap;
import android.location.Location;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;

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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import de.hhu.cs.feedbackr.Helper;
import de.hhu.cs.feedbackr.LoadImageTask;
import de.hhu.cs.feedbackr.R;
import de.hhu.cs.feedbackr.Zoom;
import de.hhu.cs.feedbackr.databinding.FragmentMapBinding;
import de.hhu.cs.feedbackr.firebase.FirebaseHelper;
import de.hhu.cs.feedbackr.model.CategoryConverter;
import de.hhu.cs.feedbackr.model.Feedback;
import de.hhu.cs.feedbackr.view.activity.MainActivity;

/**
 * The MapFragment displays a @{@link MapView} depending on the current location of the device
 * the nearby feedback is shown. The maximal radius of the feedback circle is 3000 meter.
 * The radius is adjusted depending on the zoom level of the map.
 * <p>
 * In the top left of the view, there are 4 buttons which serve as filter, to limit the shown
 * feedback. You can display private / public feedback and positive / negative.
 * Additionally there is another button which allows to move the center of the circle to the current
 * position of the map.
 * <p>
 * The Feedback is stored in 4 hashmaps for each possible combination of filter.
 * <p>
 * If the user chooses to touch a feedback marker, a dialog appears displaying additional information
 * about the selected feedback. If the feedback has a image present, the image will be shown as a
 * small thumbnail, on touch the image will be shown by zooming it into the underlying container
 */
public class MapFragment extends Fragment implements GoogleMap.OnMarkerClickListener, GoogleMap.OnCameraIdleListener, OnMapReadyCallback {
    private static final String PRIVATE_PREFERENCE_KEY = "private_marker";
    private static final String PUBLIC_PREFERENCE_KEY = "public_marker";
    private static final String POSITIVE_PREFERENCE_KEY = "positive_marker";
    private static final String NEGATIVE_PREFERENCE_KEY = "negative_marker";
    private static final double MAX_CIRCLE_RADIUS = 3000;

    // filter
    private boolean showPrivate;
    private boolean showPublic;
    private boolean showPositive;
    private boolean showNegative;
    private Map<String, Marker> privatePosMarker;
    private Map<String, Marker> privateNegMarker;
    private Map<String, Marker> publicPosMarker;
    private Map<String, Marker> publicNegMarker;

    // map
    private MapView mMapView;
    private GoogleMap googleMap;
    private boolean mapLoaded = false;
    private Circle searchCircle;
    private GeoQuery query;

    // feedback dialog
    private Zoom zoom;
    private View expandContainer;
    private View expander;
    private FragmentMapBinding binding;
    private Feedback feedback;
    private ImageView feedbackPhoto;
    private ImageView expandedImage;
    private ValueEventListener dialogListener;
    private ProgressBar loadImg;

    public MapFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // create HashMaps
        privatePosMarker = new HashMap<>();
        privateNegMarker = new HashMap<>();
        publicPosMarker = new HashMap<>();
        publicNegMarker = new HashMap<>();

        // create a zoom object to expand the dialog and imageView
        zoom = new Zoom();
        zoom.setAnimationEndListener(() -> {
            FirebaseHelper.getFeedbackRef().child(feedback.getId()).removeEventListener(dialogListener);
            feedback = null;
        });

        getPreferences();

        initFeedbackDialogListener();
    }

    /**
     * Get the last used filter for the user from the preferences
     */
    private void getPreferences() {
        SharedPreferences sharedPref = Objects.requireNonNull(getActivity()).getPreferences(Context.MODE_PRIVATE);
        showPrivate = sharedPref.getBoolean(PRIVATE_PREFERENCE_KEY, true);
        showPublic = sharedPref.getBoolean(PUBLIC_PREFERENCE_KEY, true);
        showPositive = sharedPref.getBoolean(POSITIVE_PREFERENCE_KEY, true);
        showNegative = sharedPref.getBoolean(NEGATIVE_PREFERENCE_KEY, true);
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
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_map, container, false);
        View view = binding.getRoot();
        binding.setFeedback(feedback);

        // init floating action buttons
        FloatingActionButton privateFAB = view.findViewById(R.id.map_fab_personal);
        privateFAB.setImageResource(showPrivate ? R.drawable.ic_profile_white_24dp : R.drawable.ic_profile_disabled_white_24dp);
        privateFAB.setOnClickListener(this::showHide);

        FloatingActionButton publicFAB = view.findViewById(R.id.map_fab_public);
        publicFAB.setImageResource(showPublic ? R.drawable.ic_public_black_24dp : R.drawable.ic_public_disabled_black_24dp);
        publicFAB.setOnClickListener(this::showHide);

        FloatingActionButton negativeFAB = view.findViewById(R.id.map_fab_negative);
        negativeFAB.setImageResource(showNegative ? R.drawable.ic_thumb_down_white_24dp : R.drawable.ic_thumb_down_disabled_white_24dp);
        negativeFAB.setOnClickListener(this::showHide);

        FloatingActionButton positiveFAB = view.findViewById(R.id.map_fab_positive);
        positiveFAB.setImageResource(showPositive ? R.drawable.ic_thumb_up_white_24dp : R.drawable.ic_thumb_up_disabled_white_24dp);
        positiveFAB.setOnClickListener(this::showHide);

        FloatingActionButton centerFAB = view.findViewById(R.id.btn_center);
        centerFAB.setOnClickListener(v -> updateCenter());

        // init map
        mMapView = view.findViewById(R.id.map_map);
        mMapView.onCreate(savedInstanceState);
        mMapView.getMapAsync(this);

        // init feedback dialog
        loadImg = view.findViewById(R.id.load_img);
        expander = view.findViewById(R.id.expander);
        expandedImage = view.findViewById(R.id.expanded_image);
        expandContainer = view.findViewById(R.id.expand_container);
        ImageButton feedbackEdit = view.findViewById(R.id.edit_feedback);
        feedbackEdit.setOnClickListener(e -> ((MainActivity) Objects.requireNonNull(getActivity())).switchToFeedbackDetail(feedback));
        feedbackPhoto = view.findViewById(R.id.feedback_photo);
        feedbackPhoto.setOnClickListener(e -> zoom.zoomImageFromThumb(feedbackPhoto, expandedImage, expandContainer));

        return view;
    }

    /**
     * Show or hide the markers
     *
     * @param v View
     */
    private void showHide(View v) {
        if (mapLoaded) {
            int changed = 0; // 1 pub; 2 priv; 3 pos; 4 neg

            switch (v.getId()) {
                case R.id.map_fab_public:
                    changed = 1;
                    showPublic = !showPublic;
                    ((FloatingActionButton) v).setImageResource(showPublic ? R.drawable.ic_public_black_24dp : R.drawable.ic_public_disabled_black_24dp);
                    break;
                case R.id.map_fab_personal:
                    changed = 2;
                    showPrivate = !showPrivate;
                    ((FloatingActionButton) v).setImageResource(showPrivate ? R.drawable.ic_profile_white_24dp : R.drawable.ic_profile_disabled_white_24dp);

                    break;
                case R.id.map_fab_positive:
                    changed = 3;
                    showPositive = !showPositive;
                    ((FloatingActionButton) v).setImageResource(showPositive ? R.drawable.ic_thumb_up_white_24dp : R.drawable.ic_thumb_up_disabled_white_24dp);
                    break;
                case R.id.map_fab_negative:
                    changed = 4;
                    showNegative = !showNegative;
                    ((FloatingActionButton) v).setImageResource(showNegative ? R.drawable.ic_thumb_down_white_24dp : R.drawable.ic_thumb_down_disabled_white_24dp);
                    break;
            }
            showHideLocations(changed);
        }
    }

    /**
     * Toogle the visiblity of the marker
     *
     * @param changed indicator for which markers have changed
     */
    private void showHideLocations(int changed) {
        if (changed == 1 || changed == 3) {
            for (Marker marker : publicPosMarker.values()) {
                marker.setVisible(showPositive && showPublic);
            }
        }
        if (changed == 1 || changed == 4) {
            for (Marker marker : publicNegMarker.values()) {
                marker.setVisible(showNegative && showPublic);
            }
        }
        if (changed == 2 || changed == 3) {
            for (Marker marker : privatePosMarker.values()) {
                marker.setVisible(showPositive && showPrivate);
            }
        }
        if (changed == 2 || changed == 4) {
            for (Marker marker : privateNegMarker.values()) {
                marker.setVisible(showNegative && showPrivate);
            }
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

        Bitmap bmp = Helper.getBitmapFromVectorDrawable(CategoryConverter.tagToDrawable(feedback.getCategory()), feedback.isPositive(), getContext());
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
     * Attaches a listener to the GeoFire query. Which will return all Feedback in a certain area/radius.
     * The returned Feedback keys will be used to get the actual Feedback object.
     */
    private void addFeedbackListener() {
        query.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                // check if we found a new key
                if (!checkLocationKey(key)) {
                    // get the actual feedback for the key
                    FirebaseHelper.getFeedbackRef().child(key).addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            Feedback f = dataSnapshot.getValue(Feedback.class);

                            if (f != null) {
                                // change of existing feedback
                                // update icon
                                if (privateNegMarker.containsKey(f.getId())) {
                                    Bitmap bmp = Helper.getBitmapFromVectorDrawable(CategoryConverter.tagToDrawable(f.getCategory()), f.isPositive(), getContext());
                                    Marker marker = privateNegMarker.get(f.getId());
                                    marker.setIcon(BitmapDescriptorFactory.fromBitmap(bmp));
                                    if (f.isPositive()) {
                                        // move marker to pos hashmap
                                        privatePosMarker.put(f.getId(), privateNegMarker.remove(f.getId()));
                                    }
                                } else if (privatePosMarker.containsKey(f.getId())) {
                                    Bitmap bmp = Helper.getBitmapFromVectorDrawable(CategoryConverter.tagToDrawable(f.getCategory()), f.isPositive(), getContext());
                                    privatePosMarker.get(f.getId()).setIcon(BitmapDescriptorFactory.fromBitmap(bmp));
                                    if (!f.isPositive()) {
                                        // move marker
                                        privateNegMarker.put(f.getId(), privatePosMarker.remove(f.getId()));
                                    }
                                } else if (publicPosMarker.containsKey(f.getId())) {
                                    Bitmap bmp = Helper.getBitmapFromVectorDrawable(CategoryConverter.tagToDrawable(f.getCategory()), f.isPositive(), getContext());
                                    publicPosMarker.get(f.getId()).setIcon(BitmapDescriptorFactory.fromBitmap(bmp));
                                    if (!f.isPositive()) {
                                        // move marker
                                        publicNegMarker.put(f.getId(), publicPosMarker.remove(f.getId()));
                                    }
                                } else if (publicNegMarker.containsKey(f.getId())) {
                                    Bitmap bmp = Helper.getBitmapFromVectorDrawable(CategoryConverter.tagToDrawable(f.getCategory()), f.isPositive(), getContext());
                                    publicNegMarker.get(f.getId()).setIcon(BitmapDescriptorFactory.fromBitmap(bmp));
                                    if (f.isPositive()) {
                                        // move marker
                                        publicPosMarker.put(f.getId(), publicNegMarker.remove(f.getId()));
                                    }
                                } else {
                                    // new feedback
                                    Marker marker = createMarker(f);
                                    if (marker != null) {
                                        if (f.getOwner().equals(Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid())) {
                                            // private
                                            if (f.isPositive()) {
                                                marker.setVisible(showPrivate && showPositive);
                                                privatePosMarker.put(f.getId(), marker);
                                            } else {
                                                marker.setVisible(showPrivate && showNegative);
                                                privateNegMarker.put(f.getId(), marker);
                                            }
                                        } else if (f.isPublished()) {
                                            // public
                                            if (f.isPositive()) {
                                                marker.setVisible(showPublic && showPositive);
                                                publicPosMarker.put(f.getId(), marker);
                                            } else {
                                                marker.setVisible(showPublic && showNegative);
                                                publicNegMarker.put(f.getId(), marker);
                                            }
                                        }

                                    }
                                }
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {
                            System.out.println("ERROR READING " + key);
                        }
                    });
                }
            }

            @Override
            public void onKeyExited(String key) {
                hideLocationKey(key);
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
     * Check if the given key is already in one of the maps
     *
     * @param key LocationKey
     * @return true if key is in one of the maps
     */
    private boolean checkLocationKey(String key) {
        if (publicPosMarker.containsKey(key)) {
            publicPosMarker.get(key).setVisible(showPublic && showPositive);
            return true;
        } else if (publicNegMarker.containsKey(key)) {
            publicNegMarker.get(key).setVisible(showPublic && showNegative);
            return true;
        } else if (privatePosMarker.containsKey(key)) {
            privatePosMarker.get(key).setVisible(showPrivate && showPositive);
            return true;
        } else if (privateNegMarker.containsKey(key)) {
            privateNegMarker.get(key).setVisible(showPrivate && showNegative);
            return true;
        }

        return false;
    }

    /**
     * Hide the location for the given key
     *
     * @param key LocationKey
     */
    private void hideLocationKey(String key) {
        if (publicPosMarker.containsKey(key)) {
            publicPosMarker.get(key).setVisible(false);
            // remove the marker from the GoogleMap and
            // delete the key from the map
//            publicPosMarker.get(key).remove();
//            publicPosMarker.remove(key);
        } else if (publicNegMarker.containsKey(key)) {
            publicNegMarker.get(key).setVisible(false);
        } else if (privatePosMarker.containsKey(key)) {
            privatePosMarker.get(key).setVisible(false);
        } else if (privateNegMarker.containsKey(key)) {
            privateNegMarker.get(key).setVisible(false);
        }
    }

    /**
     * Return the radius for the given zoom level
     *
     * @param zoom googleMap zoom level
     * @return radius to fit the screen at ~96dpi
     */
    private double zoomLevelToRadius(double zoom) {
        // estimated value 2^14 * 1000
        // fills almost the screen at 96dpi
        // this is equal to 1000 meter at zoom level 15
        return 16384000 / Math.pow(2, zoom);
    }

    /**
     * Update the circles center location
     */
    private void updateCenter() {
        if (mapLoaded) {
            // update center
            LatLng center = googleMap.getCameraPosition().target;
            searchCircle.setCenter(center);
            query.setCenter(new GeoLocation(center.latitude, center.longitude));
        }
    }

    // ================================================================= //
    // ================================================================= //
    // ========================== FEEDBACK DIALOG ====================== //
    // ================================================================= //
    // ================================================================= //

    /**
     * Create the feedbackDialogListener, this listener will be attached to a feedback.
     * We need to keep the reference to remove the listener as soon as the dialog is dismissed.
     * Otherwise we want the dialog to update on changes made in the edit view of the feedback
     * This keeps the dialog in syn with the corresponding data
     */
    private void initFeedbackDialogListener() {
        dialogListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                boolean change = (feedback == null || !feedback.getId().equals(Objects.requireNonNull(dataSnapshot.getValue(Feedback.class)).getId()));
                feedback = dataSnapshot.getValue(Feedback.class);
                binding.setFeedback(feedback);

                if (feedback.hasImage()) {
                    // show indicator
                    loadImg.setVisibility(View.VISIBLE);

                    // get image file
                    String imageFileName = feedback.getImage();
                    File storageDir = Objects.requireNonNull(getActivity()).getExternalFilesDir(Environment.DIRECTORY_PICTURES);
                    File image = new File(storageDir, imageFileName + ".jpg");

                    // load image
                    LoadImageTask loadImageTask = new LoadImageTask(feedback);
                    loadImageTask.setOnBitmapCreatedListener(img -> {
                        setFeedbackPhoto(img);
                        loadImg.setVisibility(View.INVISIBLE);
                    });
                    loadImageTask.execute(image);
                } else {
                    loadImg.setVisibility(View.INVISIBLE);
                    setFeedbackPhoto(null);
                }

                // show edit button
                if (feedback.getOwner().equals(Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid())) {
                    expander.findViewById(R.id.edit_feedback).setVisibility(View.VISIBLE);
                } else {
                    expander.findViewById(R.id.edit_feedback).setVisibility(View.GONE);
                }
                // zoom the dialog if new feedback
                if (change) {
                    zoom.zoomView(expander, expandContainer);
                } else {
                    // update icon
                    Bitmap bmp = Helper.getBitmapFromVectorDrawable(CategoryConverter.tagToDrawable(feedback.getCategory()), feedback.isPositive(), getContext());
                    if (privateNegMarker.containsKey(feedback.getId())) {
                        privateNegMarker.get(feedback.getId()).setIcon(BitmapDescriptorFactory.fromBitmap(bmp));
                        if (feedback.isPositive()) {
                            // move marker to pos hashmap
                            privatePosMarker.put(feedback.getId(), privateNegMarker.remove(feedback.getId()));
                        }
                    } else if (privatePosMarker.containsKey(feedback.getId())) {
                        privatePosMarker.get(feedback.getId()).setIcon(BitmapDescriptorFactory.fromBitmap(bmp));
                        if (!feedback.isPositive()) {
                            // move marker
                            privateNegMarker.put(feedback.getId(), privatePosMarker.remove(feedback.getId()));
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        };
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

        FirebaseHelper.getFeedbackRef().child(id).addValueEventListener(dialogListener);

        return true;
    }

    /**
     * Update the feedback image
     *
     * @param image bitmap of the image
     */
    private void setFeedbackPhoto(Bitmap image) {
        feedbackPhoto.setImageBitmap(image);
        expandedImage.setImageBitmap(image);
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.googleMap = googleMap;

        Location location = ((MainActivity) Objects.requireNonNull(getActivity())).getCurrentLocation();
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

        searchCircle = this.googleMap.addCircle(new CircleOptions().center(latLng).radius(1000));
        searchCircle.setFillColor(ContextCompat.getColor(Objects.requireNonNull(getContext()), R.color.mapCircleFill));
        searchCircle.setStrokeColor(ContextCompat.getColor(getContext(), R.color.mapCircleStroke));
        this.googleMap.setOnCameraIdleListener(this);

        query = FirebaseHelper.getGeofire().queryAtLocation(new GeoLocation(location.getLatitude(), location.getLongitude()), 1.0);

        addFeedbackListener();

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
    public void onStop() {
        super.onStop();

        // save floating action buttons state in preferences
        SharedPreferences sharedPref = Objects.requireNonNull(getActivity()).getPreferences(Context.MODE_PRIVATE);
        sharedPref.edit().putBoolean(PRIVATE_PREFERENCE_KEY, showPrivate).apply();
        sharedPref.edit().putBoolean(PUBLIC_PREFERENCE_KEY, showPublic).apply();
        sharedPref.edit().putBoolean(POSITIVE_PREFERENCE_KEY, showPositive).apply();
        sharedPref.edit().putBoolean(NEGATIVE_PREFERENCE_KEY, showNegative).apply();
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
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        mMapView.onSaveInstanceState(outState);
    }
}
