package de.hhu.cs.feedbackr.view.activity;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatSpinner;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Switch;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Objects;

import de.hhu.cs.feedbackr.R;
import de.hhu.cs.feedbackr.databinding.ActivityFeedbackEditBinding;
import de.hhu.cs.feedbackr.databinding.DialogSwitchBinding;
import de.hhu.cs.feedbackr.firebase.FirebaseHelper;
import de.hhu.cs.feedbackr.firebase.FirebaseStorageHelper;
import de.hhu.cs.feedbackr.model.CategoryConverter;
import de.hhu.cs.feedbackr.model.Feedback;
import de.hhu.cs.feedbackr.model.Profile;
import de.hhu.cs.feedbackr.Helper;
import de.hhu.cs.feedbackr.LoadImageTask;

/**
 * Activity for Displaying a Feedback and Editing it
 */
public class FeedbackEditActivity extends AppCompatActivity implements OnMapReadyCallback {
    static final int REQUEST_TAKE_PHOTO = 105;
    private String mCurrentPhotoPath;

    private Feedback feedback;

    private MapView mMapView;
    private Marker mMarker;

    private ArrayAdapter<CharSequence> mAdapter;
    private AppCompatSpinner mSpinner;

    private ImageView feedback_photo;
    private ImageView expanded_image;
    private Animator animator;

    private ProgressBar loadImg;

    /**
     * Creates the Activity
     *
     * @param savedInstanceState Saved Instance of the Activity
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityFeedbackEditBinding binding = DataBindingUtil.setContentView(this, R.layout.activity_feedback_edit);

        feedback = (Feedback) Objects.requireNonNull(getIntent().getExtras()).get("Feedback");
        binding.setFeedback(feedback);

        Toolbar toolbar = findViewById(R.id.detail_toolbar);
        setSupportActionBar(toolbar);
        assert getSupportActionBar() != null;
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Get The MapView and put a GoogleMap inside
        mMapView = binding.mapViewFeedbackDet;
        mMapView.onCreate(savedInstanceState);
        // the initialization of the service takes a decent amount of time
        // no fix available, this must be executed on the mainThread
        mMapView.getMapAsync(this);

        mAdapter = ArrayAdapter.createFromResource(Objects.requireNonNull(this),
                feedback.isPositive() ? R.array.positive_array : R.array.negative_array, android.R.layout.simple_spinner_item);
        mAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        mSpinner = binding.spinnerCategory;
        mSpinner.setAdapter(mAdapter);

        if (feedback.getCategory() != null) {
            mSpinner.setSelection(mAdapter.getPosition(getString(CategoryConverter.tagToString(feedback.getCategory()))));
        }

        mSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int pos, long l) {
                feedback.setCategory(CategoryConverter.stringToTag(feedback.isPositive(), getApplicationContext(), (String) adapterView.getAdapter().getItem(pos)));
                updateMarker();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        ((Switch) findViewById(R.id.switchAttach)).setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked && Profile.getInstance() == null) {
                new AlertDialog.Builder(Objects.requireNonNull(this))
                        .setView(LayoutInflater.from(this).inflate(R.layout.dialog_no_profile, null))
                        .setPositiveButton(R.string.ok, (dialog, which) -> buttonView.setChecked(false))
                        .create()
                        .show();
            }
        });

        loadImg = findViewById(R.id.load_img);
        expanded_image = findViewById(R.id.expanded_image);
        feedback_photo = findViewById(R.id.feedback_photo);
        if (feedback.isHasPhoto()) {
            System.out.println("HAS PHOTO");
            // show indicator
            loadImg.setVisibility(View.VISIBLE);

            // get image file
            String imageFileName = feedback.getId();
            File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            File image = new File(storageDir, imageFileName + ".jpg");

            // load image
            LoadImageTask loadImageTask = new LoadImageTask(feedback);
            loadImageTask.setOnBitmapCreatedListener(bitmap -> {
                setFeedbackPhoto(bitmap);
                // hide load indicator
                loadImg.setVisibility(View.INVISIBLE);
            });
            loadImageTask.execute(image);
        } else {
            loadImg.setVisibility(View.INVISIBLE);
        }
    }

    public Feedback getFeedback() {
        boolean attach = ((Switch) findViewById(R.id.switchAttach)).isChecked();
        if (attach) {
            if (feedback.getProfile() == null) {
                // no previous profile
                // -> attach profile of user
                feedback.setProfile(Profile.getInstance());
            }
        } else {
            feedback.setProfile(null);
        }

        return feedback;
    }

    /**
     * Handles Switching the Kind of the Feedback
     */
    public void switchKind() {
        feedback.switchKind();
        mAdapter = ArrayAdapter.createFromResource(this, feedback.isPositive() ? R.array.positive_array : R.array.negative_array, android.R.layout.simple_spinner_item);
        mAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinner.setAdapter(mAdapter);
    }

    public void setFeedbackPhoto(Bitmap image) {
        feedback.setPhoto(image);
        feedback_photo.setImageBitmap(image);
        expanded_image.setImageBitmap(image);
    }

    /**
     * Update the Marker Icon
     */
    public void updateMarker() {
        mMarker.setIcon(BitmapDescriptorFactory.fromBitmap(Helper.getBitmap(this, feedback.isPositive(), feedback.getCategory())));
    }

    /**
     * Switch Feedback between Positive and Negative
     */
    private void switchFeedback() {
        DialogSwitchBinding binding = DataBindingUtil.inflate(getLayoutInflater(), R.layout.dialog_switch, null, false);
        binding.setFeedback(feedback);
        //Create a warning Dialog
        AlertDialog switchDialog = new AlertDialog.Builder(this)
                .setView(binding.getRoot())
                .setPositiveButton(R.string.switch_word, (dialogInterface, i) -> {
                    switchKind();
                    updateMarker();
                })
                .setNegativeButton(R.string.cancel, (dialogInterface, i) -> dialogInterface.cancel()).create();
        switchDialog.show();
    }

    /**
     * Handles the deletion of a Feedback
     */
    private void deleteAlarm() {
        AlertDialog deleteDialog = new AlertDialog.Builder(this)
                //set message, title, and icon
                .setTitle(R.string.delete)
                .setMessage(R.string.delete_confirmation)
                .setPositiveButton(R.string.delete, (dialog, whichButton) -> {
                    FirebaseHelper.deleteFeedback(getFeedback());
                    dialog.dismiss();
                    finish();
                })
                .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss())
                .create();
        deleteDialog.show();
    }

    /**
     * https://developer.android.com/training/animation/zoom
     *
     * @param thumbView the image thumbnail
     */
    private void zoomImageFromThumb(final ImageView thumbView) {
        // If there's an animation in progress, cancel it
        // immediately and proceed with this one.
        if (animator != null) {
            animator.cancel();
        }

        // Load the high-resolution "zoomed-in" image.
        final ConstraintLayout expander = findViewById(R.id.expander);
        expanded_image = findViewById(R.id.expanded_image);
        expanded_image.setImageDrawable(thumbView.getDrawable());

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
        findViewById(R.id.feedback_detail_frame).getGlobalVisibleRect(finalBounds, globalOffset);
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
        set.start();
        animator = set;

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
            set1.start();
            animator = set1;
        });
    }

    /**
     * Display a Dialog showing the image for the feedback
     *
     * @param view unused
     */
    public void showImage(View view) {
        if (getFeedback().isHasPhoto()) {
            zoomImageFromThumb((ImageView) view);
        } else {
            takePicture(null);
        }
    }

    /**
     * Remove the image from the object and delete it from the database
     *
     * @param view unused
     */
    public void removePicture(View view) {
        AlertDialog removeHint = new AlertDialog.Builder(this)
                .setTitle(R.string.remove_image)
                .setPositiveButton(R.string.remove_image_pos, (dialog, which) -> {
                    // if the call is unsuccessful the image will remain in the database
                    File image = createImageFile();
                    if (image.delete()) {
                        FirebaseStorageHelper.deleteImage(getFeedback().getId());
                    }
                    setFeedbackPhoto(null);

                    // hide expander and show thumbnail
                    findViewById(R.id.expander).setVisibility(View.INVISIBLE);
                    feedback_photo.setAlpha(1f);

                    // close the dialogs for the image
                    dialog.dismiss();
                })
                .setNegativeButton(R.string.remove_image_neg, (dialog, which) -> dialog.dismiss())
                .create();

        removeHint.show();
    }

    public void takePicture(View view) {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = createImageFile();
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this, "de.hhu.cs.feedbackr.fileprovider", photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
            }
        }

        //todo picture will also be saved in the gallery -> 2 times
    }

    private File createImageFile() {
        String imageFileName = getFeedback().getId();
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);

        File image = new File(storageDir, imageFileName + ".jpg");

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.getAbsolutePath();

        return image;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_TAKE_PHOTO && resultCode == RESULT_OK) {
            System.out.println("PICTURE RESULT ");

            BitmapFactory.Options opt = new BitmapFactory.Options();
            opt.inSampleSize = 4;
            System.out.println(mCurrentPhotoPath);

            Bitmap bitmap = BitmapFactory.decodeFile(mCurrentPhotoPath, opt);

            // save file to local storage
            try (FileOutputStream fos = new FileOutputStream(createImageFile())) {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            } catch (Exception e) {
                e.printStackTrace();
            }

            setFeedbackPhoto(bitmap);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        // Puts a Marker on the Map where the Feedback was Send Add
        LatLng coordinates = new LatLng(feedback.getLatitude(), feedback.getLongitude());
        MarkerOptions marker = new MarkerOptions().position(coordinates)
                .icon(BitmapDescriptorFactory.fromBitmap(Helper.getBitmap(this, feedback.isPositive(), feedback.getCategory())));
        mMarker = googleMap.addMarker(marker);

        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(coordinates, 15));
    }

    /**
     * Inflates the Menu
     *
     * @param menu Menu of the Activity
     * @return true
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_edit, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.menu_edit_switch:
                switchFeedback();
                return true;
            case R.id.menu_edit_delete:
                deleteAlarm();
                return true;
        }
        return super.onOptionsItemSelected(item);
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
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        mMapView.onSaveInstanceState(outState);
    }

    @Override
    public void onBackPressed() {
        // save change on back
        Feedback feedback = getFeedback();

        // todo dont save image again
        // todo new image flag
        FirebaseHelper.saveFeedback(feedback);

        super.onBackPressed();
    }
}