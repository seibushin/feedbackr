package de.hhu.cs.feedbackr.model;


import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.location.Location;

import de.hhu.cs.feedbackr.BR;
import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/**
 * <p>
 * Feedback gets created by Users
 */

@IgnoreExtraProperties
public class Feedback extends BaseObservable implements Serializable {
    private double mLatitude;
    private double mLongitude;
    private boolean mPositive;
    private long mDate;
    private String mCategory;
    private String mCity;
    private boolean mPublish;
    private String mDetails;

    private String mId;

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
        mPublish = false;
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

    public void setPublished(boolean publish) {
        mPublish = publish;
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
    public long getDate() {
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


    public String getId() {
        return mId;
    }

    public void setId(String id){
        mId = id;
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
                '}';
    }
}