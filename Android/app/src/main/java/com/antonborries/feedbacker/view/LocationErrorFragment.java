package com.antonborries.feedbacker.view;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.antonborries.feedbacker.R;


/**
 * A Fragment to Display a Location error
 */
public class LocationErrorFragment extends Fragment {


    //USe ProfileFragment.newInstance() instead
    public LocationErrorFragment() {
        // Required empty public constructor
    }

    public static LocationErrorFragment newInstance() {

        return new LocationErrorFragment();
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment

        View view = inflater.inflate(R.layout.fragment_location_error, container, false);
        view.findViewById(R.id.iVRefresh).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Try to Re-Establish the Connection for GoogleApiClient
                ((MainActivity) view.getContext()).retryConnection();
            }
        });

        return view;
    }

}


