package de.hhu.cs.feedbackr.view.fragment;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import de.hhu.cs.feedbackr.R;
import de.hhu.cs.feedbackr.view.activity.MainActivity;

/**
 * A fragment to display a location error. This fragment is used if the gps is not enabled on the
 * device and gives the user a possibility  to enable it and return to the feedback send fragment.
 */
public class LocationErrorFragment extends Fragment {
    public LocationErrorFragment() {
        // Required empty public constructor
    }

    /**
     * Creates the View
     *
     * @param inflater           Inflater
     * @param container          Container
     * @param savedInstanceState SavedState
     * @return the View of the Fragment
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_location_error, container, false);
        view.findViewById(R.id.iVRefresh).setOnClickListener(view1 -> {
            //Try to Re-Establish the Connection for GoogleApiClient
            ((MainActivity) view1.getContext()).makeLocationInit(false);
        });

        return view;
    }
}