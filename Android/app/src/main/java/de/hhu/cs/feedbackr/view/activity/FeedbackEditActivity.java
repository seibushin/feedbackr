package de.hhu.cs.feedbackr.view.activity;

import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatSpinner;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
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

import de.hhu.cs.feedbackr.Helper;
import de.hhu.cs.feedbackr.LoadImageTask;
import de.hhu.cs.feedbackr.R;
import de.hhu.cs.feedbackr.Zoom;
import de.hhu.cs.feedbackr.databinding.ActivityFeedbackEditBinding;
import de.hhu.cs.feedbackr.databinding.DialogSwitchBinding;
import de.hhu.cs.feedbackr.firebase.FirebaseHelper;
import de.hhu.cs.feedbackr.firebase.FirebaseStorageHelper;
import de.hhu.cs.feedbackr.model.CategoryConverter;
import de.hhu.cs.feedbackr.model.Feedback;
import de.hhu.cs.feedbackr.model.Profile;

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

    // image zoom / image dialog (remove/retake)
    private View expander;
    private View expandContainer;
    private ImageView feedback_photo;
    private ImageView expanded_image;
    private Zoom zoom;
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

        // create a new zoom object and get the needed views
        zoom = new Zoom();
        expander = findViewById(R.id.expander);
        expandContainer = findViewById(R.id.feedback_detail_frame);
        loadImg = findViewById(R.id.load_img);
        expanded_image = findViewById(R.id.expanded_image);
        feedback_photo = findViewById(R.id.feedback_photo);

        // check if the feedback has an image
        if (feedback.hasImage()) {
            // show indicator
            loadImg.setVisibility(View.VISIBLE);

            // get image file
            String imageFileName = feedback.getImage();
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

    /**
     * This method updates the feedback by attaching the profile or removing it. If this information
     * is not needed one can simple use {@link #feedback}
     *
     * @return the updated feedback
     */
    private Feedback getFeedback() {
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
    private void switchKind() {
        feedback.switchKind();
        mAdapter = ArrayAdapter.createFromResource(this, feedback.isPositive() ? R.array.positive_array : R.array.negative_array, android.R.layout.simple_spinner_item);
        mAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinner.setAdapter(mAdapter);
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
     * Update the feedback image. We also set the bitmap to the feedback object. This is needed
     * to be able to save the image to firebase storage
     *
     * @param image bitmap of the image
     */
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
     * Display a Dialog showing the image for the feedback
     *
     * @param view unused
     */
    public void showImage(View view) {
        if (getFeedback().hasImage()) {
            zoom.zoomImageFromThumb(feedback_photo, expander, expandContainer);
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
                    File image = getImageFile();
                    if (image.delete()) {
                        FirebaseStorageHelper.deleteImage(getFeedback().getImage());
                    }
                    feedback.setImage("");
                    feedback.setNewImage(false);
                    setFeedbackPhoto(null);

                    // hide expander and show thumbnail
                    expander.setVisibility(View.INVISIBLE);
                    feedback_photo.setAlpha(1f);

                    // close the dialogs for the image
                    dialog.dismiss();
                })
                .setNegativeButton(R.string.remove_image_neg, (dialog, which) -> dialog.dismiss())
                .create();

        removeHint.show();
    }

    /**
     * Initiates an image capture intent. This will result in an camera being started to take a picture
     * the resulting picture will be saved under a created filename which consists of the feedbacks id
     * and the current time as long timestamp for unique filenames
     * <p>
     * Depending in the used camera app the image might also be saved to the camera apps configured gallery!
     *
     * @param view unused
     */
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
    }

    /**
     * Get the file that points to the image name of the feedback
     *
     * @return image file
     */
    private File getImageFile() {
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = new File(storageDir, feedback.getImage() + ".jpg");
        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.getAbsolutePath();

        return image;
    }

    /**
     * Create a new unique image name and return the file
     *
     * @return the image file
     */
    private File createImageFile() {
        feedback.setOldImage(feedback.getImage());
        feedback.setImage(feedback.getId() + "_" + System.currentTimeMillis());

        return getImageFile();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // react on the result of the capture image intent
        if (requestCode == REQUEST_TAKE_PHOTO) {
            if (resultCode == RESULT_OK) {
                BitmapFactory.Options opt = new BitmapFactory.Options();
                opt.inSampleSize = 4;
                Bitmap bitmap = BitmapFactory.decodeFile(mCurrentPhotoPath, opt);

                // save file to local storage
                try (FileOutputStream fos = new FileOutputStream(getImageFile())) {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                feedback.setNewImage(true);
                setFeedbackPhoto(bitmap);
            } else {
                // result was not ok -> reset the oldImage
                feedback.setImage(feedback.getOldImage());
                feedback.setOldImage("");
            }
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

        FirebaseHelper.saveFeedback(feedback, this);

        super.onBackPressed();
    }
}