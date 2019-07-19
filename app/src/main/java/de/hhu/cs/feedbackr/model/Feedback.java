package de.hhu.cs.feedbackr.model;

import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.graphics.Bitmap;
import android.location.Location;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Locale;

import de.hhu.cs.feedbackr.BR;

/**
 * This is basically a simple POJO representing the feedback. The members of this class are transfered
 * to firebase if the getter and setter are available. Some Methods might be unused in the scope of
 * this application but needed to create the object via the {@link de.hhu.cs.feedbackr.firebase.FirebaseStorageHelper}
 * and the provided DatabaseReferences.
 * <p>
 * Note: Methods annotated with @{@link Exclude} are not used for the feedback in firebase.
 * Note: Methods annotated with @{@link Bindable} are used for the databinding
 */

@IgnoreExtraProperties
public class Feedback extends BaseObservable implements Serializable {
    private String mId;
    private double mLatitude;
    private double mLongitude;
    private boolean mPositive;
    private Long mDate;
    private String mCategory;
    private String mCity;
    private boolean mPublish;
    private String mDetails;
    private float mRating;
    private String owner;
    private Profile profile;
    private String image;
    private String oldImage;
    private Bitmap mPhoto;
    private boolean newImage;

    /**
     * Empty Constructor for Firebase
     */
    public Feedback() {
    }

    /**
     * Creates an Feedback Object
     *
     * @param location The Location the User is at
     * @param city     A String of City the User has send the Feedback from
     */
    public Feedback(Location location, String city, boolean positive, String id) {
        mLatitude = location.getLatitude();
        mLongitude = location.getLongitude();
        mDate = location.getTime();
        mCity = city;
        mPositive = positive;
        mDetails = "";
        image = "";
        mPublish = true;
        mCategory = CategoryConverter.getDefault(positive);
        mId = id;
    }

    /**
     * Getter for the latitude
     *
     * @return latitude
     */
    public double getLatitude() {
        return mLatitude;
    }

    /**
     * Setter for the latitude
     *
     * @param latitude latitude
     */
    @SuppressWarnings("unused")
    public void setLatitude(double latitude) {
        mLatitude = latitude;
    }

    /**
     * Getter for the longitude
     *
     * @return longitude
     */
    public double getLongitude() {
        return mLongitude;
    }

    /**
     * Setter for the longitude
     *
     * @param longitude longitude
     */
    @SuppressWarnings("unused")
    public void setLongitude(double longitude) {
        mLongitude = longitude;
    }

    /**
     * Getter for the determined city for the lat/long coordinates
     *
     * @return city name
     */
    public String getCity() {
        return mCity;
    }

    /**
     * Setter for the city name
     *
     * @param city city name
     */
    @SuppressWarnings("unused")
    public void setCity(String city) {
        mCity = city;
    }

    /**
     * The details/description for the feedback
     *
     * @return details
     */
    @Bindable
    public String getDetails() {
        return mDetails;
    }

    /**
     * Setter for the details
     *
     * @param details details
     */
    public void setDetails(String details) {
        mDetails = details;
        notifyPropertyChanged(BR.details);
    }

    /**
     * Flag if feedback is visible to other users
     *
     * @return true if the feedback is public
     */
    @Bindable
    public boolean isPublished() {
        return mPublish;
    }

    /**
     * Setter for the published flag
     *
     * @param published
     */
    @Bindable
    public void setPublished(boolean published) {
        mPublish = published;
        notifyPropertyChanged(BR.published);
    }

    /**
     * Flag if the feedback is of positive nature
     *
     * @return true if positive feedback
     */
    @Bindable
    public boolean isPositive() {
        return mPositive;
    }

    /**
     * Setter for the flag positive
     *
     * @param positive positive?
     */
    public void setPositive(boolean positive) {
        mPositive = positive;
    }

    /**
     * Never Used in Code but essential to Firebase
     *
     * @return current Date
     */
    public Long getDate() {
        return mDate;
    }

    /**
     * Used By Firebase
     *
     * @param date UNIX Timestamp
     */
    @SuppressWarnings("unused")
    public void setDate(long date) {
        mDate = date;
    }

    /**
     * Getter for the category
     *
     * @return category
     */
    public String getCategory() {
        if (mCategory == null) {
            setCategory(isPositive() ? CategoryConverter.POS_GENERAL : CategoryConverter.NEG_GENERAL);
        }
        return mCategory;
    }

    /**
     * Setter for the category
     *
     * @param category category
     */
    public void setCategory(String category) {
        mCategory = category;
    }

    /**
     * Getter for the feedback ID
     *
     * @return id
     */
    public String getId() {
        return mId;
    }

    /**
     * Setter for the feedback ID
     *
     * @param id id
     */
    public void setId(String id) {
        mId = id;
    }

    /**
     * Getter for the attached @{@link Profile}
     *
     * @return profile
     */
    public Profile getProfile() {
        return profile;
    }

    /**
     * Setter for the profile
     *
     * @param profile profile
     */
    public void setProfile(Profile profile) {
        this.profile = profile;
    }

    /**
     * The feedback can be rated from 0 to 5 stars. This is the getter for the rating.
     *
     * @return rating
     */
    public float getRating() {
        return mRating;
    }

    /**
     * Setter for the rating
     *
     * @param rating rating 0 to 5
     */
    public void setRating(float rating) {
        this.mRating = rating;
    }

    /**
     * Flag to determine if the feedback has a new image which needs to be uploaded to
     * firebase storage. After the upload the flag should be reset to false. If the image is deleted
     * before being uploaded the flag should also be reset to false
     *
     * @return true if feedback has new image
     */
    @Exclude
    public boolean isNewImage() {
        return newImage;
    }

    /**
     * Setter for the newImage flag
     *
     * @param newImage newImage?
     */
    @Exclude
    public void setNewImage(boolean newImage) {
        this.newImage = newImage;
    }

    /**
     * Getter to check if the feedback has a image. This is just a shortcut
     *
     * @return true if feedback has image
     */
    public boolean hasImage() {
        return image != null && !image.equals("");
    }

    /**
     * Getter for the image name. The actual image can only be accessed after downloading it from
     * firebase or after displaying the local copy of the image.
     *
     * @return the image name
     */
    public String getImage() {
        return image;
    }

    /**
     * Setter for the image name
     *
     * @param image image name
     */
    public void setImage(String image) {
        this.image = image;
    }

    /**
     * This is used for uploading a new image. If the user decides, that he doesnt like the taken
     * image and dismisses the image capture dialog the image will be reset to the old value.
     * Otherwise this value is used to delete the old image from firebase. After deleting it, the
     * value should be set to ""
     *
     * @return oldimage name
     */
    @Exclude
    public String getOldImage() {
        return this.oldImage;
    }

    /**
     * Setter for the oldImage, in most cases the oldImage should be the equal to the previously
     * used value for the image
     *
     * @param oldImage the old image name
     */
    @Exclude
    public void setOldImage(String oldImage) {
        this.oldImage = oldImage;
    }

    /**
     * Get the bitmap of the associated image
     *
     * @return the image as bitmap
     */
    @Exclude
    public Bitmap getPhoto() {
        return mPhoto;
    }

    /**
     * Setter for the bitmap image
     *
     * @param photo bitmap image
     */
    @Exclude
    public void setPhoto(Bitmap photo) {
        this.mPhoto = photo;
    }

    /**
     * Get the creator of the feedback. Since we only use anonymous user this is the user ID.
     *
     * @return user id
     */
    public String getOwner() {
        return owner;
    }

    /**
     * Setter for the user id
     *
     * @param owner user id
     */
    public void setOwner(String owner) {
        this.owner = owner;
    }

    /**
     * Is a profile attached to the feedback?
     *
     * @return true if the feedback has a profile attached
     */
    @Exclude
    public boolean hasProfile() {
        return profile != null;
    }

    /**
     * Get the time for the feedback as a formatted string.
     *
     * @return time as string
     */
    @Exclude
    public String getTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        return sdf.format(mDate);
    }

    /**
     * Get the date for the feedback as a formatted string.
     *
     * @return date as string
     */
    @Exclude
    public String getDay() {
        DateFormat df = DateFormat.getDateInstance(DateFormat.SHORT, Locale.getDefault());
        return df.format(mDate);
    }

    /**
     * Switches between positive and negative feedback
     */
    public void switchKind() {
        setPositive(!mPositive);
    }

    @Override
    public int hashCode() {
        Object[] id = {mId};
        return Arrays.hashCode(id);
    }

    @Override
    public boolean equals(Object obj) {
        return mId.equals(((Feedback) obj).mId);
    }

    @Override
    public String toString() {
        return "Feedback{" +
                "mLatitude=" + mLatitude +
                ", mLongitude=" + mLongitude +
                ", mPositive=" + mPositive +
                ", mDate=" + mDate +
                ", mCategory='" + mCategory + '\'' +
                ", mCity='" + mCity + '\'' +
                ", mPublish=" + mPublish +
                ", mDetails='" + mDetails + '\'' +
                ", mId='" + mId + '\'' +
                ", mProfile='" + profile + '\'' +
                ", mRating=" + mRating +
                ", mImage=" + image +
                ", owner=" + owner +
                '}';
    }
}
