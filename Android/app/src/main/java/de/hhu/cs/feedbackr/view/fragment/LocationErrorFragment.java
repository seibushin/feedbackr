package de.hhu.cs.feedbackr.view.fragment;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import de.hhu.cs.feedbackr.R;
import de.hhu.cs.feedbackr.view.activity.MainActivity;


/**
 * A Fragment to Display a Location error
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_location_error, container, false);
        view.findViewById(R.id.iVRefresh).setOnClickListener(view1 -> {
            //Try to Re-Establish the Connection for GoogleApiClient
            ((MainActivity) view1.getContext()).retryConnection();
        });

        return view;
    }
}