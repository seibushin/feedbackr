package de.hhu.cs.feedbackr.firebase;

import android.graphics.Bitmap;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageException;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;

import de.hhu.cs.feedbackr.model.Feedback;

public class FirebaseStorageHelper {
    // Create a storage reference from our app
    private static StorageReference storageRef = FirebaseStorage.getInstance().getReference();
    // Create a child reference
    // imagesRef now points to "images"
    public static StorageReference feedbackRef = storageRef.child("feedback");

    /**
     * Upload the image of the given feedback
     *
     * @param feedback the feedback
     */
    public static void uploadImage(Feedback feedback) {
        if (feedback.getPhoto() != null) {
            System.out.println("Upload Image");
            System.out.println(feedback);

            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            feedback.getPhoto().compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
            byte[] data = byteArrayOutputStream.toByteArray();

            StorageReference image = feedbackRef.child(feedback.getId() + ".jpg");

            UploadTask uploadTask = image.putBytes(data);
            uploadTask.addOnFailureListener(exception -> {
                // Handle unsuccessful uploads
                System.out.println("unsuccessful upload");
            }).addOnSuccessListener(taskSnapshot -> {
                // Upload was successful
                System.out.println("successful upload");
            });
        }
    }

    /**
     * Delete the image for the given Feedback Id
     *
     * @param feedbackId the feedbacks id
     */
    public static void deleteImage(String feedbackId) {
        // Create a storage reference from our app
        StorageReference image = feedbackRef.child(feedbackId + ".jpg");

        // Delete the file
        image.delete().addOnSuccessListener(aVoid -> {
            // File deleted successfully
            System.out.println("deleltion successful");
        }).addOnFailureListener(exception -> {
            int errorCode = ((StorageException) exception).getErrorCode();
            if (errorCode == StorageException.ERROR_OBJECT_NOT_FOUND) {
                System.out.println("deletion unsuccessful - object not found");
            } else {
                System.out.println("deletion unsuccessful");
            }
        });
    }
}
