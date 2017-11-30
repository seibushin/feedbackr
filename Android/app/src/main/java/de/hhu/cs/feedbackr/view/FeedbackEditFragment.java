package de.hhu.cs.feedbackr.view;


import android.content.Context;
import android.databinding.DataBindingUtil;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.DrawableRes;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.widget.AppCompatSpinner;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import de.hhu.cs.feedbackr.R;
import de.hhu.cs.feedbackr.databinding.FragmentFeedbackEditBinding;
import de.hhu.cs.feedbackr.model.CategoryConverter;
import de.hhu.cs.feedbackr.model.Feedback;
import de.hhu.cs.feedbackr.model.FirebaseHelper;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;


/**
 * A Fragment used To Display a Feedback in Detail and Edit it
 */
public class FeedbackEditFragment extends Fragment {

    private Feedback mFeedback;

    private MapView mMapView;
    private Marker mMarker;

    private ArrayAdapter<CharSequence> mAdapter;
    private AppCompatSpinner mSpinner;

    public FeedbackEditFragment() {
        // Required empty public constructor
    }

    /**
     * @param feedback Feedback the Fragment Displays
     * @return Instance of the Fragment
     */
    public static FeedbackEditFragment newInstance(Feedback feedback) {
        FeedbackEditFragment alarmDetail = new FeedbackEditFragment();
        Bundle args = new Bundle();
        args.putSerializable("Feedback", feedback);
        alarmDetail.setArguments(args);

        return alarmDetail;
    }

    /**
     * @param context    Context for getting Resources
     * @param isPositive Kind of Feedback
     * @param tag        Category of Feedback
     * @return Bitmap to Display
     */
    public static Bitmap getBitmap(Context context, boolean isPositive, String tag) {
        @DrawableRes int drawableId;
        drawableId = CategoryConverter.tagToDrawable(tag);
        Drawable drawable = ContextCompat.getDrawable(context, drawableId);
        DrawableCompat.setTint(drawable, isPositive ? ContextCompat.getColor(context, R.color.green) : ContextCompat.getColor(context, R.color.red));
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
    }

    /**
     * Creates the View
     *
     * @param inflater           Inflater
     * @param container          Container
     * @param savedInstanceState Saved Instance
     * @return The View of the Fragment
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        FragmentFeedbackEditBinding binding = DataBindingUtil.inflate(inflater, R.layout.fragment_feedback_edit, container, false);

        mFeedback = (Feedback) getArguments().getSerializable("Feedback");
        binding.setFeedback(mFeedback);

        View view = binding.getRoot();

        //Get The MapView and Put a GoogleMap inside
        mMapView = binding.mapViewFeedbackDet;
        mMapView.onCreate(savedInstanceState);
        mMapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap googleMap) {
                //Puts a Marker on the Map where the Feedback was Send Add
                LatLng coordinates = new LatLng(mFeedback.getLatitude(), mFeedback.getLongitude());
                MarkerOptions marker = new MarkerOptions().position(coordinates)
                        .icon(BitmapDescriptorFactory.fromBitmap(getBitmap(getContext(), mFeedback.isPositive(), mFeedback.getCategory())));
                mMarker = googleMap.addMarker(marker);

                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(coordinates, 15));
                mMapView.onResume();
            }
        });

        mAdapter = ArrayAdapter.createFromResource(getActivity(),
                mFeedback.isPositive() ? R.array.positive_array : R.array.negative_array, android.R.layout.simple_spinner_item);
        mAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        mSpinner = binding.spinnerCategory;
        mSpinner.setAdapter(mAdapter);

        if (mFeedback.getCategory() != null) {
            mSpinner.setSelection(mAdapter.getPosition(getString(
                    CategoryConverter.tagToString(mFeedback.getCategory()))));
        }

        mSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int pos, long l) {
                mFeedback.setCategory(CategoryConverter.stringToTag(mFeedback.isPositive(), getContext(),
                        (String) adapterView.getAdapter().getItem(pos)));
                updateMarker();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        //updateNearby();

        return view;
    }

    /**
     * Update the relevant nearby feedbacks
     *
     * todo create Adapter
     * todo display list of relevant feedback
     */
    private void updateNearby() {
        GeoFire geoFire = new GeoFire(FirebaseHelper.getGeofire());
        final List<Feedback> list = new ArrayList<>();

        // get all Feedback in the given radius of the position
        double radius = 1.0;

        final GeoQuery geoQuery = geoFire.queryAtLocation(new GeoLocation(mFeedback.getLatitude(), mFeedback.getLongitude()), radius);

        // listeners are async
        // returns all nearby feedback keys
        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                // get the actual feedback for the key
                FirebaseHelper.getFeedback().child(key).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        list.add(dataSnapshot.getValue(Feedback.class));
                        // check if published
                        // validate if relevant
                        // type match, title match, desc match ?
                        Log.d("Nearby-Feedback", dataSnapshot.getValue(Feedback.class).toString());
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
                geoQuery.removeAllListeners();
            }

            @Override
            public void onGeoQueryError(DatabaseError error) {
                Log.e("GeoFire", error.toString());
            }
        });
    }

    /**
     * Update the Marker Icon
     */
    public void updateMarker() {
        mMarker.setIcon(BitmapDescriptorFactory.fromBitmap(getBitmap(getContext(), mFeedback.isPositive(), mFeedback.getCategory())));
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

    public Feedback getFeedback() {
        return mFeedback;
    }

    /**
     * Handles Switching the Kind of the Feedback
     */
    public void switchKind() {
        mFeedback.switchKind();
        mAdapter = ArrayAdapter.createFromResource(getActivity(),
                mFeedback.isPositive() ? R.array.positive_array : R.array.negative_array, android.R.layout.simple_spinner_item);
        mAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinner.setAdapter(mAdapter);
    }
}
