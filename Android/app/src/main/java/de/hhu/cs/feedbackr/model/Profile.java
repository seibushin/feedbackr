package de.hhu.cs.feedbackr.model;

import com.google.firebase.database.Exclude;

import java.io.Serializable;

/**
 * Object to hold a profile. The profile is only saved in the firebase database after the user
 * opens the profile activity for the first time. Otherwise the profile is not needed to use the
 * app. The profile simply allows the user to attach some personal information which might be
 * of value for the feedback. But the user has always the choice of attaching it or not.
 * <p>
 * This class is also used to set the auth flag, which tells us if the user was successfully
 * authenticated at firebase.
 */
public class Profile implements Serializable {
    private int age;
    private String gender = "mf";
    private boolean attach = false;

    private static Profile instance;
    private static boolean auth = false;

    /**
     * Empty constructor for Firebase
     */
    public Profile() {
    }

    /**
     * Get the singleton profile instance
     *
     * @return the profile or null
     */
    public static synchronized Profile getInstance() {
        return instance;
    }

    /**
     * Setter for the singleton profile
     *
     * @param profile profile
     */
    public static synchronized void setInstanceProfile(Profile profile) {
        instance = profile;
    }

    /**
     * Getter for the age
     *
     * @return age
     */
    public String getAge() {
        return "" + age;
    }

    /**
     * Setter for the age
     *
     * @param age age as string
     */
    public void setAge(String age) {
        try {
            this.age = Integer.parseInt(age);
        } catch (NumberFormatException e) {
            //ignore
        }
    }

    /**
     * Getter for the gender
     *
     * @return gender as "mf", "m", "f" string
     */
    public String getGender() {
        return gender;
    }

    /**
     * Setter for the gender
     *
     * @param gender gender ("mf", "m", "f")
     */
    public void setGender(String gender) {
        this.gender = gender;
    }

    /**
     * Getter for no gender
     *
     * @return true of gender is "mf"
     */
    @Exclude
    public boolean getNone() {
        return gender.equals("mf");
    }

    /**
     * Setter for no gender
     *
     * @param none no gender?
     */
    @Exclude
    public void setNone(boolean none) {
        if (none) {
            gender = "mf";
        }
    }

    /**
     * Getter for gender male
     *
     * @return true if gender is "m"
     */
    @Exclude
    public boolean getMale() {
        return gender.equals("m");
    }

    /**
     * Setter for the gender male
     *
     * @param male male?
     */
    @Exclude
    public void setMale(boolean male) {
        if (male) {
            gender = "m";
        }
    }

    /**
     * Getter for the gender female
     *
     * @return true if gender is "f"
     */
    @Exclude
    public boolean getFemale() {
        return gender.equals("f");
    }

    /**
     * Setter for the gender female
     *
     * @param female female?
     */
    @Exclude
    public void setFemale(boolean female) {
        if (female) {
            gender = "f";
        }
    }

    /**
     * Flag if the profile should be attached to every new feedback per default
     *
     * @return true if profile should per default be attached
     */
    public boolean isAttach() {
        return attach;
    }

    /**
     * Setter for the attach flag
     *
     * @param attach attach
     */
    public void setAttach(boolean attach) {
        this.attach = attach;
    }

    /**
     * Flag to determine if the user is authenticated. When the app is started it tries to authenticate
     * to user to firebase, which will result in the user getting his unique user id, which stays the
     * same as long as the user does not uninstall and reinstall the app.
     *
     * @return true if the user is authenticated
     */
    public static boolean isAuth() {
        return auth;
    }

    /**
     * Setter for the auth flag
     *
     * @param auth authenticated?
     */
    public static void setAuth(boolean auth) {
        Profile.auth = auth;
    }

    @Override
    public String toString() {
        return "Profile{" +
                "age=" + age +
                ", gender='" + gender + '\'' +
                '}';
    }
}
