package de.hhu.cs.feedbackr.firebase;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Environment;
import android.util.Log;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageException;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.File;

import de.hhu.cs.feedbackr.model.Feedback;

/**
 * This class is used to access the Firebase Storage. The Firebase Storage provides a way to save
 * files.
 * We use firebase storage to store the images of the feedback.
 * The images are saved under the feedback ID and the current time represented as long.
 * <p>
 * This class provides a function to upload and delete a file.
 */
public class FirebaseStorageHelper {
    // Create a storage reference from our app
    private static StorageReference storageRef = FirebaseStorage.getInstance().getReference();
    // Create a child reference
    // imagesRef now points to "images"
    public static StorageReference feedbackRef = storageRef.child("feedback");

    /**
     * Upload the image of the given feedback. This method uses the class @{@link UploadTask} which
     * executes the upload on a different thread to not block the ui. This implicates that the upload
     * happens async.
     *
     * @param feedback the feedback
     */
    public static void uploadImage(Feedback feedback, Context context) {
        if (feedback.isNewImage() && feedback.getPhoto() != null) {
            Log.d(FirebaseStorageHelper.class.getName(), "Upload image");

            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            feedback.getPhoto().compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
            byte[] data = byteArrayOutputStream.toByteArray();

            StorageReference image = feedbackRef.child(feedback.getImage() + ".jpg");

            UploadTask uploadTask = image.putBytes(data);
            uploadTask.addOnFailureListener(exception -> {
                // Handle unsuccessful uploads
                Log.d(FirebaseStorageHelper.class.getName(), "Unsuccessful upload");
            }).addOnSuccessListener(taskSnapshot -> {
                // Upload was successful
                Log.d(FirebaseStorageHelper.class.getName(), "Successful upload");
            });
        }

        if (feedback.getOldImage() != null && !feedback.getOldImage().equals("")) {
            // if the call is unsuccessful the image will remain in the database
            File storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            File image = new File(storageDir, feedback.getOldImage() + ".jpg");
            if (image.delete()) {
                deleteImage(feedback.getOldImage());

                feedback.setOldImage("");
            }
        }

        // reset newImage flag
        feedback.setNewImage(false);
    }

    /**
     * Delete the image for the given Feedback Id
     *
     * @param feedbackImage the feedbacks id
     */
    public static void deleteImage(String feedbackImage) {
        // Create a storage reference from our app
        StorageReference image = feedbackRef.child(feedbackImage + ".jpg");

        // Delete the file
        image.delete().addOnSuccessListener(aVoid -> {
            // File deleted successfully
            Log.d(FirebaseStorageHelper.class.getName(), "Image deletion successful");
        }).addOnFailureListener(exception -> {
            int errorCode = ((StorageException) exception).getErrorCode();
            if (errorCode == StorageException.ERROR_OBJECT_NOT_FOUND) {
                Log.d(FirebaseStorageHelper.class.getName(), "Image deletion unsuccessful - object not found");
            } else {
                Log.d(FirebaseStorageHelper.class.getName(), "Image deletion unsuccessful");
            }
        });
    }
}
