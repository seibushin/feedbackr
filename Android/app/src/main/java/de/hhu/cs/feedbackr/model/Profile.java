package de.hhu.cs.feedbackr.model;

import com.google.firebase.database.Exclude;

import java.io.Serializable;

/**
 * Object to hold an profile
 */

public class Profile implements Serializable {
    private int age;
    private String gender = "mf";
    private boolean attach = false;

    private static Profile instance;

    /**
     * Empty Constructor for Firebase
     */
    public Profile() {
    }

    public static synchronized Profile getInstance() {
        return instance;
    }

    public Profile(Profile mProfile, boolean attach) {
        this.age = mProfile.age;
        this.gender = mProfile.gender;
        this.attach = attach;
    }

    public static synchronized void setInstanceProfile(Profile profile) {
        instance = profile;
    }

    public String getAge() {
        return "" + age;
    }

    public void setAge(String age) {
        try {
            this.age = Integer.parseInt(age);
        } catch (NumberFormatException e) {
            //ignore
        }
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    @Exclude
    public boolean getNone() {
        return gender.equals("mf");
    }

    @Exclude
    public void setNone(boolean none) {
        if (none) {
            gender = "mf";
        }
    }

    @Exclude
    public boolean getMale() {
        return gender.equals("m");
    }

    @Exclude
    public void setMale(boolean male) {
        if (male) {
            gender = "m";
        }
    }

    @Exclude
    public boolean getFemale() {
        return gender.equals("f");
    }

    @Exclude
    public void setFemale(boolean female) {
        if (female) {
            gender = "f";
        }
    }

    public boolean isAttach() {
        return attach;
    }

    public void setAttach(boolean attach) {
        this.attach = attach;
    }

    @Override
    public String toString() {
        return "Profile{" +
                "age=" + age +
                ", gender='" + gender + '\'' +
                '}';
    }
}
