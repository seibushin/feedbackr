package de.hhu.cs.feedbackr.view.dialog;

import android.app.Dialog;
import android.content.DialogInterface;
import android.databinding.DataBindingUtil;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.widget.ImageView;

import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.util.Objects;

import de.hhu.cs.feedbackr.R;
import de.hhu.cs.feedbackr.databinding.DialogFeedbackBinding;
import de.hhu.cs.feedbackr.firebase.FirebaseStorageHelper;
import de.hhu.cs.feedbackr.model.Feedback;
import de.hhu.cs.feedbackr.view.activity.MainActivity;
import de.hhu.cs.feedbackr.view.fragment.FeedbackEditFragment;

/**
 * Shows A Dialog For a Feedback which displays Information
 */

public class FeedbackDialog extends DialogFragment {

    public static final String FEEDBACK_KEY = "FEEDBACK";
    public static final String USER_KEY = "IS_USER_FEEDBACK";

    private Feedback mFeedback;
    private boolean mEditable;
    private ImageView feedback_photo;

    /**
     * Creates a Dialog with Information for a Feedback
     *
     * @param feedback     Feedback to be shown
     * @param isUsersAlarm true if User send the Feedback
     * @return FeedbackDialog
     */
    public static FeedbackDialog newInstance(Feedback feedback, boolean isUsersAlarm) {
        FeedbackDialog dialog = new FeedbackDialog();
        Bundle args = new Bundle();
        args.putSerializable(FEEDBACK_KEY, feedback);
        args.putBoolean(USER_KEY, isUsersAlarm);
        dialog.setArguments(args);
        return dialog;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mFeedback = (Feedback) Objects.requireNonNull(getArguments()).get(FEEDBACK_KEY);
        mEditable = getArguments().getBoolean(USER_KEY);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        DialogFeedbackBinding binding = DataBindingUtil.inflate(LayoutInflater.from(getContext()), R.layout.dialog_feedback, null, false);
        binding.setFeedback(mFeedback);

        AlertDialog.Builder builder = new AlertDialog.Builder(Objects.requireNonNull(getActivity()));
        builder.setView(binding.getRoot());

        // If the Feedback is from the user add an edit button
        if (mEditable) {
            builder.setPositiveButton(R.string.edit, (dialogInterface, i) -> ((MainActivity) getActivity()).switchToFeedbackDetail(mFeedback));
        }

        feedback_photo = binding.feedbackPhoto;

        if (mFeedback.isHasPhoto()) {
            // show indicator

            // get image file
            String imageFileName = mFeedback.getId();
            File storageDir = getActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            File image = new File(storageDir, imageFileName + ".jpg");

            // load image
            LoadImageTask loadImageTask = new LoadImageTask(mFeedback);
            loadImageTask.setOnBitmapCreatedListener(this::setFeedbackPhoto);
            loadImageTask.execute(image);
        }

        return builder.create();
    }

    public void setFeedbackPhoto(Bitmap image) {
        feedback_photo.setImageBitmap(image);
    }


    private static class LoadImageTask extends AsyncTask<File, Void, Void> {
        private LoadImageTask.OnBitmapCreatedListener onBitmapCreatedListener;

        private Feedback feedback;

        LoadImageTask(Feedback feedback) {
            this.feedback = feedback;
        }

        void setOnBitmapCreatedListener(LoadImageTask.OnBitmapCreatedListener onBitmapCreatedListener) {
            this.onBitmapCreatedListener = onBitmapCreatedListener;
        }

        @Override
        protected Void doInBackground(File... files) {
            System.out.println("LOAD IMAGE IN BACKGROUND");

            // check if image is already downloaded
            File image = files[0];

            // image does not exist
            // download it
            if (!image.exists()) {
                System.out.println("GET IMAGE FROM FIREBASE");
                StorageReference load = FirebaseStorageHelper.feedbackRef.child(feedback.getId() + ".jpg");
                load.getFile(image).addOnSuccessListener(taskSnapshot -> {
                    System.out.println("test");
                    Bitmap bitmap = BitmapFactory.decodeFile(image.getAbsolutePath());
                    onBitmapCreatedListener.onBitmapCreated(bitmap);
                });
            } else {
                System.out.println("USE LOCAL IMAGE");
                // image exists display it
                Bitmap bitmap = BitmapFactory.decodeFile(image.getAbsolutePath());
                onBitmapCreatedListener.onBitmapCreated(bitmap);
            }

            return null;
        }

        interface OnBitmapCreatedListener {
            void onBitmapCreated(Bitmap bitmap);
        }
    }
}
