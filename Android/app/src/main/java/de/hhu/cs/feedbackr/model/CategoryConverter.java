package de.hhu.cs.feedbackr.model;

import android.content.Context;

import de.hhu.cs.feedbackr.R;

/**
 * Created by antonborries on 26/04/2017.
 */

public class CategoryConverter {
    final public static String POS_GENERAL = "POS_GENERAL";
    final private static String POS_SIT = "POS_SIT";
    final private static String POS_TOILET = "POS_TOILET";
    final private static String POS_DISABILITY = "POS_DISABILITY";
    final private static String POS_WIFI = "POS_WIFI";
    final private static String POS_EAT_DRINK = "POS_EAT_DRINK";
    final private static String POS_VIEW = "POS_VIEW";

    final public static String NEG_GENERAL = "NEG_GENERAL";
    final private static String NEG_DARK = "NEG_DARK";
    final private static String NEG_DIRTY = "NEG_DIRTY";
    final private static String NEG_PEOPLE = "NEG_PEOPLE";
    final private static String NEG_WALKING = "NEG_WALKING";
    final private static String NEG_GRAFFITI = "NEG_GRAFFITI";

    /**
     * @param positive  Kind of the Feedback
     * @param context   Context to get String Resources
     * @param selection Selected String
     * @return Firebase Accepted Category String
     */
    public static String stringToTag(boolean positive, Context context, String selection) {
        //Switch Statement does not work with Strings from Resources
        //Divide The Request in the Positive and Negative Categories
        //to Minimize the Amount of if-else Statements
        if (positive) {
            if (selection.equals(context.getString(R.string.cat_pos_general))) {
                return POS_GENERAL;
            } else if (selection.equals(context.getString(R.string.cat_pos_sit))) {
                return POS_SIT;
            } else if (selection.equals(context.getString(R.string.cat_pos_toilet))) {
                return POS_TOILET;
            } else if (selection.equals(context.getString(R.string.cat_pos_disability_access))) {
                return POS_DISABILITY;
            } else if (selection.equals(context.getString(R.string.cat_pos_wifi))) {
                return POS_WIFI;
            } else if (selection.equals(context.getString(R.string.cat_pos_eat_drink))) {
                return POS_EAT_DRINK;
            } else if (selection.equals(context.getString(R.string.cat_pos_nice_view))) {
                return POS_VIEW;
            }
            return POS_GENERAL;
        } else {
            if (selection.equals(context.getString(R.string.cat_neg_general))) {
                return NEG_GENERAL;
            } else if (selection.equals(context.getString(R.string.cat_neg_dark))) {
                return NEG_DARK;
            } else if (selection.equals(context.getString(R.string.cat_neg_dirty))) {
                return NEG_DIRTY;
            } else if (selection.equals(context.getString(R.string.cat_neg_people))) {
                return NEG_PEOPLE;
            } else if (selection.equals(context.getString(R.string.cat_neg_walking_friendly))) {
                return NEG_WALKING;
            } else if (selection.equals(context.getString(R.string.cat_neg_graffiti))) {
                return NEG_GRAFFITI;
            }
            return NEG_GENERAL;
        }
    }

    /**
     * Gets A Firebase conform Category Tag and converts it to a Localized String
     *
     * @param tag Firebase Category
     * @return Localized String Resource Tag
     */
    public static int tagToString(String tag) {
        switch (tag) {
            case POS_GENERAL:
                return R.string.cat_pos_general;
            case POS_SIT:
                return R.string.cat_pos_sit;
            case POS_TOILET:
                return R.string.cat_pos_toilet;
            case POS_DISABILITY:
                return R.string.cat_pos_disability_access;
            case POS_WIFI:
                return R.string.cat_pos_wifi;
            case POS_EAT_DRINK:
                return R.string.cat_pos_eat_drink;
            case POS_VIEW:
                return R.string.cat_pos_nice_view;

            case NEG_GENERAL:
                return R.string.cat_neg_general;
            case NEG_DARK:
                return R.string.cat_neg_dark;
            case NEG_DIRTY:
                return R.string.cat_neg_dirty;
            case NEG_PEOPLE:
                return R.string.cat_neg_people;
            case NEG_WALKING:
                return R.string.cat_neg_walking_friendly;
            case NEG_GRAFFITI:
                return R.string.cat_neg_graffiti;
            default:
                return tag.startsWith("POS") ? R.string.cat_pos_general : R.string.cat_neg_general;
        }
    }

    /**
     * Returns a Icon for the category
     *
     * @param tag Firebase Category Tag
     * @return Drawable Id of Category Icon
     */
    public static int tagToDrawable(String tag) {
        switch (tag) {
            case POS_GENERAL:
                return R.drawable.ic_thumb_up_white_24dp;
            case POS_SIT:
                return R.drawable.ic_bench_black_24dp;
            case POS_TOILET:
                return R.drawable.ic_wc_black_24dp;
            case POS_DISABILITY:
                return R.drawable.ic_accessible_black_24dp;
            case POS_WIFI:
                return R.drawable.ic_wifi_black_24dp;
            case POS_EAT_DRINK:
                return R.drawable.ic_restaurant_black_24dp;
            case POS_VIEW:
                return R.drawable.ic_view_black_24dp;
            case NEG_GENERAL:
                return R.drawable.ic_thumb_down_white_24dp;
            case NEG_DARK:
                return R.drawable.ic_dark_black_24dp;
            case NEG_DIRTY:
                return R.drawable.ic_delete_black_24dp;
            case NEG_PEOPLE:
                return R.drawable.ic_people_black_24dp;
            case NEG_WALKING:
                return R.drawable.ic_walk_black_24dp;
            case NEG_GRAFFITI:
                return R.drawable.ic_graffiti_black_24dp;
            default:
                return tag.startsWith("POS") ? R.drawable.ic_thumb_up_white_24dp : R.drawable.ic_thumb_down_white_24dp;
        }
    }

    /**
     * Returns Default Category
     *
     * @param kind Kind of Feedback
     * @return POS_GENERAL for positive Feedback and NEG_GENERAL for negative Feedback
     */
    public static String getDefault(boolean kind) {
        return kind ? POS_GENERAL : NEG_GENERAL;
    }
}
