package de.hhu.cs.feedbackr.view.fragment;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.databinding.DataBindingUtil;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Rect;
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
import android.view.animation.DecelerateInterpolator;
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

import de.hhu.cs.feedbackr.R;
import de.hhu.cs.feedbackr.databinding.FragmentMapBinding;
import de.hhu.cs.feedbackr.firebase.FirebaseHelper;
import de.hhu.cs.feedbackr.model.CategoryConverter;
import de.hhu.cs.feedbackr.model.Feedback;
import de.hhu.cs.feedbackr.Helper;
import de.hhu.cs.feedbackr.LoadImageTask;
import de.hhu.cs.feedbackr.view.activity.MainActivity;

/**
 * A Fragment to Display A Map in the MainActivity
 */
public class MapFragment extends Fragment implements GoogleMap.OnMarkerClickListener, GoogleMap.OnCameraIdleListener, OnMapReadyCallback {
    private static final String PERSONAL_PREFERENCE_KEY = "personal_marker";
    private static final String PUBLIC_PREFERENCE_KEY = "public_marker";
    private static final String POSITIVE_PREFERENCE_KEY = "positive_marker";
    private static final String NEGATIVE_PREFERENCE_KEY = "positive_marker";
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
    private FragmentMapBinding binding;
    private Feedback feedback;
    private View expander;
    private Animator animator;
    private ImageView feedbackPhoto;
    private ImageView expandedPhoto;
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

        getPreferences();

        initFeedbackDialogListener();
    }

    /**
     * Get the last used filter for the user
     */
    private void getPreferences() {
        SharedPreferences sharedPref = Objects.requireNonNull(getActivity()).getPreferences(Context.MODE_PRIVATE);
        showPrivate = sharedPref.getBoolean(PERSONAL_PREFERENCE_KEY, true);
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
        expander = view.findViewById(R.id.dialog_feedback);
        expandedPhoto = view.findViewById(R.id.expanded_image);
        ImageButton feedbackEdit = view.findViewById(R.id.edit_feedback);
        feedbackEdit.setOnClickListener(e -> ((MainActivity) Objects.requireNonNull(getActivity())).switchToFeedbackDetail(feedback));
        feedbackPhoto = view.findViewById(R.id.feedback_photo);
        feedbackPhoto.setOnClickListener(e -> zoomImageFromThumb(feedbackPhoto, expandedPhoto));

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
     * Attaches a listener to the GeoFire query. Which will give all Feedback in a certain area/radius.
     * The returned Feedback keys will be used to get the Feedback.
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
                                    privateNegMarker.get(f.getId()).setIcon(BitmapDescriptorFactory.fromBitmap(bmp));
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

    private void initFeedbackDialogListener() {
        dialogListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                boolean change = (feedback == null || !feedback.getId().equals(Objects.requireNonNull(dataSnapshot.getValue(Feedback.class)).getId()));
                feedback = dataSnapshot.getValue(Feedback.class);
                binding.setFeedback(feedback);

                System.out.println("hasImage:" + feedback.hasImage() + " " + feedback.getImage());
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
                    zoomView(expander);
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

    private void setFeedbackPhoto(Bitmap image) {
        feedbackPhoto.setImageBitmap(image);
        expandedPhoto.setImageBitmap(image);
    }

    /**
     * https://developer.android.com/training/animation/zoom
     *
     * @param thumbView the image thumbnail
     * @param expander  the wrapper in which the thumbnail should be expanded
     */
    private void zoomImageFromThumb(final ImageView thumbView, View expander) {
        // If there's an animation in progress, cancel it
        // immediately and proceed with this one.
        if (animator != null) {
            animator.cancel();
        }

        // Calculate the starting and ending bounds for the zoomed-in image.
        // This step involves lots of math. Yay, math.
        final Rect startBounds = new Rect();
        final Rect finalBounds = new Rect();
        final Point globalOffset = new Point();

        // The start bounds are the global visible rectangle of the thumbnail,
        // and the final bounds are the global visible rectangle of the container
        // view. Also set the container view's offset as the origin for the
        // bounds, since that's the origin for the positioning animation
        // properties (X, Y).
        thumbView.getGlobalVisibleRect(startBounds);
        Objects.requireNonNull(getView()).findViewById(R.id.expand_container).getGlobalVisibleRect(finalBounds, globalOffset);
        startBounds.offset(-globalOffset.x, -globalOffset.y);
        finalBounds.offset(-globalOffset.x, -globalOffset.y);

        // Adjust the start bounds to be the same aspect ratio as the final
        // bounds using the "center crop" technique. This prevents undesirable
        // stretching during the animation. Also calculate the start scaling
        // factor (the end scaling factor is always 1.0).
        float startScale;
        if ((float) finalBounds.width() / finalBounds.height() > (float) startBounds.width() / startBounds.height()) {
            // Extend start bounds horizontally
            startScale = (float) startBounds.height() / finalBounds.height();
            float startWidth = startScale * finalBounds.width();
            float deltaWidth = (startWidth - startBounds.width()) / 2;
            startBounds.left -= deltaWidth;
            startBounds.right += deltaWidth;
        } else {
            // Extend start bounds vertically
            startScale = (float) startBounds.width() / finalBounds.width();
            float startHeight = startScale * finalBounds.height();
            float deltaHeight = (startHeight - startBounds.height()) / 2;
            startBounds.top -= deltaHeight;
            startBounds.bottom += deltaHeight;
        }

        // Hide the thumbnail and show the zoomed-in view. When the animation
        // begins, it will position the zoomed-in view in the place of the
        // thumbnail.
        thumbView.setAlpha(0f);
        expander.setVisibility(View.VISIBLE);

        // Set the pivot point for SCALE_X and SCALE_Y transformations
        // to the top-left corner of the zoomed-in view (the default
        // is the center of the view).
        expander.setPivotX(1f);
        expander.setPivotY(0f);

        // Construct and run the parallel animation of the four translation and
        // scale properties (X, Y, SCALE_X, and SCALE_Y).
        AnimatorSet set = new AnimatorSet();
        set
                .play(ObjectAnimator.ofFloat(expander, View.X, startBounds.left, finalBounds.left))
                .with(ObjectAnimator.ofFloat(expander, View.Y, startBounds.top, finalBounds.top))
                .with(ObjectAnimator.ofFloat(expander, View.SCALE_X, startScale, 1f))
                .with(ObjectAnimator.ofFloat(expander, View.SCALE_Y, startScale, 1f));
        set.setDuration(500);
        set.setInterpolator(new DecelerateInterpolator());
        set.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                animator = null;
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                animator = null;
            }
        });
        animator = set;
        set.start();

        // Upon clicking the zoomed-in image, it should zoom back down
        // to the original bounds and show the thumbnail instead of
        // the expanded image.
        final float startScaleFinal = startScale;
        expander.setOnClickListener(view -> {
            if (animator != null) {
                animator.cancel();
            }

            // Animate the four positioning/sizing properties in parallel,
            // back to their original values.
            AnimatorSet set1 = new AnimatorSet();
            set1.play(ObjectAnimator
                    .ofFloat(expander, View.X, startBounds.left))
                    .with(ObjectAnimator.ofFloat(expander, View.Y, startBounds.top))
                    .with(ObjectAnimator.ofFloat(expander, View.SCALE_X, startScaleFinal))
                    .with(ObjectAnimator.ofFloat(expander, View.SCALE_Y, startScaleFinal));
            set1.setDuration(500);
            set1.setInterpolator(new DecelerateInterpolator());
            set1.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    thumbView.setAlpha(1f);
                    expander.setVisibility(View.GONE);
                    animator = null;
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                    thumbView.setAlpha(1f);
                    expander.setVisibility(View.GONE);
                    animator = null;
                }
            });
            animator = set1;
            set1.start();
        });
    }

    /**
     * https://developer.android.com/training/animation/zoom
     * <p>
     * Edited by Sebastian Meyer
     */
    private void zoomView(View expander) {
        // If there's an animation in progress, cancel it
        // immediately and proceed with this one.
        if (animator != null) {
            animator.cancel();
        }

        // Calculate the starting and ending bounds for the zoomed-in image.
        // This step involves lots of math. Yay, math.
        final Rect startBounds = new Rect();
        final Rect finalBounds = new Rect();
        final Point globalOffset = new Point();

        // we want the view to start from the center of the screen and end there as well
        // The final bounds are the global visible rectangle of the container view
        Objects.requireNonNull(getView()).findViewById(R.id.expand_container).getGlobalVisibleRect(finalBounds, globalOffset);
        // set the offset for the final bounds
        finalBounds.offset(-globalOffset.x, -globalOffset.y);

        // Show the expander
        expander.setVisibility(View.VISIBLE);

        System.out.println(startBounds.left + " " + finalBounds.left);
        System.out.println(startBounds.top + " " + finalBounds.top);

        // Construct and run the parallel animation of the four translation and
        // scale properties (X, Y, SCALE_X, and SCALE_Y).
        AnimatorSet set = new AnimatorSet();
        set
                .play(ObjectAnimator.ofFloat(expander, View.X, startBounds.left, finalBounds.left))
                .with(ObjectAnimator.ofFloat(expander, View.Y, startBounds.top, finalBounds.top))
                // The StartScale is always 0 and the end scaling factor is always 1.0
                .with(ObjectAnimator.ofFloat(expander, View.SCALE_X, 0f, 1f))
                .with(ObjectAnimator.ofFloat(expander, View.SCALE_Y, 0f, 1f));
        set.setDuration(200);
        set.setInterpolator(new DecelerateInterpolator());
        set.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                animator = null;
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                animator = null;
            }
        });
        animator = set;
        set.start();

        // Upon clicking the zoomed-in expander, it should zoom back down
        // to the original bounds and disappear
        expander.setOnClickListener(view -> {
            if (animator != null) {
                animator.cancel();
            }

            // Animate the four positioning/sizing properties in parallel,
            // back to their original values.
            AnimatorSet set1 = new AnimatorSet();
            set1.play(ObjectAnimator
                    .ofFloat(expander, View.X, startBounds.left))
                    .with(ObjectAnimator.ofFloat(expander, View.Y, startBounds.top))
                    .with(ObjectAnimator.ofFloat(expander, View.SCALE_X, 0F))
                    .with(ObjectAnimator.ofFloat(expander, View.SCALE_Y, 0F));
            set1.setDuration(200);
            set1.setInterpolator(new DecelerateInterpolator());
            set1.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    expander.setVisibility(View.GONE);
                    animator = null;

                    // remove listener for the feedback
                    FirebaseHelper.getFeedbackRef().child(feedback.getId()).removeEventListener(dialogListener);
                    feedback = null;
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                    expander.setVisibility(View.GONE);
                    animator = null;

                    // remove listener for the feedback
                    FirebaseHelper.getFeedbackRef().child(feedback.getId()).removeEventListener(dialogListener);
                    feedback = null;
                }
            });
            animator = set1;
            set1.start();
        });
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
        sharedPref.edit().putBoolean(PERSONAL_PREFERENCE_KEY, showPrivate).apply();
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
