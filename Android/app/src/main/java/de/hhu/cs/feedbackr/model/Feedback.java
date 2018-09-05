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
    transient private Bitmap mPhoto;

    private String mId;

    private Profile mProfile;

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
    public Feedback(Location location, Calendar calendar, String city, boolean kind, String id) {
        setLatitude(location.getLatitude());
        setLongitude(location.getLongitude());

        mDate = calendar.getTimeInMillis();
        setCity(city);
        setPositive(kind);
        mDetails = "";
        mPublish = true;
        mCategory = CategoryConverter.getDefault(kind);

        setId(id);
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
     * Switches between Positive and Negative Feedback
     */
    public void switchKind() {
        setPositive(!mPositive);
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
    public boolean hasProfile() {
        return mProfile != null;
    }

    public String getId() {
        return mId;
    }

    public void setId(String id){
        mId = id;
    }

    public Profile getmProfile() {
        return mProfile;
    }

    public void setmProfile(Profile mProfile) {
        this.mProfile = mProfile;
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

    @BindingAdapter("android:src")
    public static void loadImage(ImageView view, Bitmap bitmap) {
        System.out.println("BITMAP:" + bitmap);
        System.out.println(view);
        view.setImageBitmap(bitmap);
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
                ", mProfile='" + mProfile + '\'' +
                ", mRating=" + mRating +
                ", mHasPhoto=" + mHasPhoto +
                '}';
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
}
