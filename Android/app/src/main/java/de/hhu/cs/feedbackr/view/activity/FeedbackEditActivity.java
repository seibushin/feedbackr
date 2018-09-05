package de.hhu.cs.feedbackr.view.activity;

import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;

import java.io.File;

import de.hhu.cs.feedbackr.R;
import de.hhu.cs.feedbackr.databinding.DialogSwitchBinding;
import de.hhu.cs.feedbackr.model.Feedback;
import de.hhu.cs.feedbackr.firebase.FirebaseHelper;
import de.hhu.cs.feedbackr.firebase.FirebaseStorageHelper;
import de.hhu.cs.feedbackr.view.fragment.FeedbackEditFragment;

/**
 * Activity for Displaying a Feedback and Editing it
 */
public class FeedbackEditActivity extends AppCompatActivity {
    static final int REQUEST_TAKE_PHOTO = 105;
    private String mCurrentPhotoPath;

    private AlertDialog imageDialog;

    /**
     * The Fragment The Activity is Hosting
     */
    FeedbackEditFragment mFeedbackEditFragment;

    /**
     * Creates the Activity
     *
     * @param savedInstanceState Saved Instance of the Activity
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feedback_edit);

        Toolbar toolbar = findViewById(R.id.detail_toolbar);
        setSupportActionBar(toolbar);
        assert getSupportActionBar() != null;
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mFeedbackEditFragment = FeedbackEditFragment.newInstance((Feedback) getIntent().getExtras().get("Feedback"));
        getSupportFragmentManager().beginTransaction().add(R.id.feedback_detail_frame, mFeedbackEditFragment).commit();

        imageDialog = new AlertDialog.Builder(this).create();
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

    /**
     * Switch Feedback between Positive and Negative
     */
    private void switchFeedback() {
        DialogSwitchBinding binding = DataBindingUtil.inflate(getLayoutInflater(), R.layout.dialog_switch, null, false);
        binding.setFeedback(mFeedbackEditFragment.getFeedback());
        //Create a warning Dialog
        AlertDialog switchDialog = new AlertDialog.Builder(this)
                .setView(binding.getRoot())
                .setPositiveButton(R.string.switch_word, (dialogInterface, i) -> {
                    mFeedbackEditFragment.switchKind();
                    mFeedbackEditFragment.updateMarker();
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
                    FirebaseHelper.deleteFeedback(mFeedbackEditFragment.getFeedback());
                    dialog.dismiss();
                    finish();
                })
                .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss())
                .create();
        deleteDialog.show();
    }

    @Override
    public void onBackPressed() {
        // save change on back
        Feedback feedback = mFeedbackEditFragment.getFeedback();

        // todo dont save image again
        // todo new image flag
        FirebaseHelper.saveFeedback(feedback);

        super.onBackPressed();
    }

    /**
     * Display a Dialog showing the image for the feedback
     *
     * @param view
     */
    public void showImage(View view) {
        if (mFeedbackEditFragment.getFeedback().isHasPhoto()) {
            if (imageDialog.isShowing()) {
                ((ImageView) imageDialog.findViewById(R.id.feedback_image)).setImageBitmap(mFeedbackEditFragment.getFeedback().getPhoto());
            } else {
                View root = LayoutInflater.from(this).inflate(R.layout.dialog_image, null);

                ((ImageView) root.findViewById(R.id.feedback_image)).setImageBitmap(mFeedbackEditFragment.getFeedback().getPhoto());

                System.out.println("Image Width:" + mFeedbackEditFragment.getFeedback().getPhoto().getWidth());

                imageDialog = new AlertDialog.Builder(this)
                        .setView(root)
                        .create();

                root.findViewById(R.id.back).setOnClickListener(v -> {
                    imageDialog.cancel();
                });

                imageDialog.show();
            }
        } else {
            takePicture(null);
        }
    }

    /**
     * Remove the image from the object and delete it from the database
     *
     * @param view
     */
    public void removePicture(View view) {
        AlertDialog removeHint = new AlertDialog.Builder(this)
                .setTitle(R.string.remove_image)
                .setPositiveButton(R.string.remove_image_pos, (dialog, which) -> {
                    // if the call is unsuccessful the image will remain in the database
                    // todo check better solution
                    // check feedback object @hasImage
                    // todo delete local image
                    File image = createImageFile();
                    if (image.delete()) {
                        FirebaseStorageHelper.deleteImage(mFeedbackEditFragment.getFeedback());
                        mFeedbackEditFragment.setFeedbackPhoto(null);
                        ((ImageView) imageDialog.findViewById(R.id.feedback_image)).setImageBitmap(null);
                    }

                    // close the dialogs for the image
                    dialog.dismiss();
                    imageDialog.dismiss();
                })
                .setNegativeButton(R.string.remove_image_neg, (dialog, which) -> {
                    dialog.dismiss();
                })
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
                Uri photoURI = FileProvider.getUriForFile(this,
                        "de.hhu.cs.feedbackr.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
            }
        }

        //todo picture will also be saved in the gallery -> 2 times
        // todo: remove Image -> close App -> feedback will not save noPhoto = false -> open App -> show Feedback -> return/save/back -> original unchanged image will be saved
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_TAKE_PHOTO && resultCode == RESULT_OK) {
            System.out.println("PICTURE RESULT ");

            BitmapFactory.Options opt = new BitmapFactory.Options();
            opt.inSampleSize = 4;
            System.out.println(mCurrentPhotoPath);

            Bitmap bitmap = BitmapFactory.decodeFile(mCurrentPhotoPath, opt);

            mFeedbackEditFragment.setFeedbackPhoto(bitmap);

            showImage(null);
        }
    }

    private File createImageFile() {
        String imageFileName = mFeedbackEditFragment.getFeedback().getId();
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);

        // creates unique filename with a trailing number
            /*
                File image = File.createTempFile(
                imageFileName,  // prefix
                ".jpg",         // suffix
                storageDir      // directory
            );
            */

        File image = new File(storageDir, imageFileName + ".jpg");

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.getAbsolutePath();

        return image;
    }
}