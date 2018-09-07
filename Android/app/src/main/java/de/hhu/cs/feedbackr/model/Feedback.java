package de.hhu.cs.feedbackr.model;


import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.databinding.BindingAdapter;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.widget.ImageView;

import de.hhu.cs.feedbackr.BR;
import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Locale;
import java.util.Objects;

/**
 * <p>
 * Feedback gets created by Users
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
    private boolean mHasPhoto;
    private String owner;
    private Profile profile;

    transient private Bitmap mPhoto;

    /**
     * Empty Constructor for Firebase
     */
    public Feedback() {
    }

    /**
     * Creates an Feedback Object
     *
     * @param location The Location the User is at
     * @param calendar A Calendar Object to get Date and Time of the Feedback
     * @param city     A String of City the User has send the Feedback from
     */
    public Feedback(Location location, Calendar calendar, String city, boolean positive, String id) {
        mLatitude = location.getLatitude();
        mLongitude = location.getLongitude();
        mDate = calendar.getTimeInMillis();
        mCity = city;
        mPositive = positive;
        mDetails = "";
        mPublish = true;
        mCategory = CategoryConverter.getDefault(positive);
        mId = id;
    }

    public double getLatitude() {
        return mLatitude;
    }

    public void setLatitude(double latitude) {
        mLatitude = latitude;
    }

    public double getLongitude() {
        return mLongitude;
    }

    public void setLongitude(double longitude) {
        mLongitude = longitude;
    }

    public String getCity() {
        return mCity;
    }

    public void setCity(String city) {
        mCity = city;
    }

    @Bindable
    public String getDetails() {
        return mDetails;
    }

    public void setDetails(String details) {
        mDetails = details;
        notifyPropertyChanged(BR.details);
    }

    @Bindable
    public boolean isPublished() {
        return mPublish;
    }

    @Bindable
    public void setPublished(boolean published) {
        mPublish = published;
        notifyPropertyChanged(BR.published);
    }

    @Bindable
    public boolean isPositive() {
        return mPositive;
    }

    public void setPositive(boolean positive) {
        mPositive = positive;
    }

    /**
     * Never Used in Code but essential to Firebase
     * @return current Date
     */
    @SuppressWarnings("unused")
    public Long getDate() {
        return mDate;
    }

    /**
     * Used By Firebase
     * @param date UNIX Timestamp
     */
    @SuppressWarnings("unused")
    public void setDate(long date) {
        mDate = date;
    }

    public String getCategory() {
        if(mCategory == null){
            setCategory(isPositive() ? CategoryConverter.POS_GENERAL : CategoryConverter.NEG_GENERAL);
        }
        return mCategory;
    }

    public void setCategory(String category) {
        mCategory = category;
    }

    public String getId() {
        return mId;
    }

    public void setId(String id){
        mId = id;
    }

    public Profile getProfile() {
        return profile;
    }

    public void setProfile(Profile profile) {
        this.profile = profile;
    }

    public float getRating() {
        return mRating;
    }

    public void setRating(float rating) {
        this.mRating = rating;
    }

    public boolean isHasPhoto() {
        return mHasPhoto;
    }

    public void setHasPhoto(boolean mHasPhoto) {
        this.mHasPhoto = mHasPhoto;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    @Exclude
    public boolean hasProfile() {
        return profile != null;
    }

    @Exclude
    public String getTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        return sdf.format(mDate);
    }

    @Exclude
    public String getDay() {
        DateFormat df = DateFormat.getDateInstance(DateFormat.SHORT, Locale.getDefault());
        return df.format(mDate);
    }

    @Exclude
    public Bitmap getPhoto() {
        System.out.println("Feedback Image: " + mPhoto);
        return mPhoto;
    }

    @Exclude
    public void setPhoto(Bitmap photo) {
        System.out.println("new Photo " + photo);
        this.mPhoto = photo;
        if (this.mPhoto != null) {
            this.mHasPhoto = true;
        } else {
            this.mHasPhoto= false;
        }
    }

    /**
     * Switches between Positive and Negative Feedback
     */
    public void switchKind() {
        setPositive(!mPositive);
    }

    @Override
    public int hashCode() {
        Object[] id = {mId};
        return  Arrays.hashCode(id);
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
                ", mHasPhoto=" + mHasPhoto +
                ", owner=" + owner +
                '}';
    }
}
