package de.hhu.cs.feedbackr.view;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;

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

    public interface OnBitmapCreatedListener {
        void onBitmapCreated(Bitmap bitmap);
    }
}