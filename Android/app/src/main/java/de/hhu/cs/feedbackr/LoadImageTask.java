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

public class LoadImageTask extends AsyncTask<File, Void, Void> {
    private LoadImageTask.OnBitmapCreatedListener onBitmapCreatedListener;

    private Feedback feedback;

    public LoadImageTask(Feedback feedback) {
        this.feedback = feedback;
    }

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

    public interface OnBitmapCreatedListener {
        void onBitmapCreated(Bitmap bitmap);
    }
}