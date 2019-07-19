package de.hhu.cs.feedbackr;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.firebase.storage.StorageReference;

import java.io.File;

import de.hhu.cs.feedbackr.firebase.FirebaseStorageHelper;
import de.hhu.cs.feedbackr.model.Feedback;

/**
 * This class can be used to load images from firebase in the background. On result the listener
 * will be executed on the MainThread, which is able to interact with the UI.
 */
public class LoadImageTask extends AsyncTask<File, Void, Void> {
    private LoadImageTask.OnBitmapCreatedListener onBitmapCreatedListener;

    private Feedback feedback;

    /**
     * Constructor, to create a new LoadImageTask for the given feedback
     *
     * @param feedback
     */
    public LoadImageTask(Feedback feedback) {
        this.feedback = feedback;
    }

    /**
     * Set the listener, the listener must be set!
     *
     * @param onBitmapCreatedListener
     */
    public void setOnBitmapCreatedListener(LoadImageTask.OnBitmapCreatedListener onBitmapCreatedListener) {
        this.onBitmapCreatedListener = onBitmapCreatedListener;
    }

    @Override
    protected Void doInBackground(File... files) {
        // handler to execute on main thread (ui)
        Handler handler = new Handler(Looper.getMainLooper());

        // check if image is already downloaded
        File image = files[0];
        // image does not exist download it
        if (!image.exists()) {
            Log.d(getClass().getName(), "Load image from firebase");
            StorageReference load = FirebaseStorageHelper.feedbackRef.child(feedback.getImage() + ".jpg");
            load.getFile(image).addOnSuccessListener(taskSnapshot -> {
                Bitmap bitmap = BitmapFactory.decodeFile(image.getAbsolutePath());
                // execute callback
                handler.post(() -> onBitmapCreatedListener.onBitmapCreated(bitmap));
            });
        } else {
            Log.d(getClass().getName(), "Use local image");
            // image exists display it
            Bitmap bitmap = BitmapFactory.decodeFile(image.getAbsolutePath());
            // execute callback
            handler.post(() -> onBitmapCreatedListener.onBitmapCreated(bitmap));
        }

        return null;
    }

    /**
     * Listener interface, for when the bitmap was created
     */
    public interface OnBitmapCreatedListener {
        void onBitmapCreated(Bitmap bitmap);
    }
}