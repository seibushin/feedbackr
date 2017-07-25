package com.antonborries.feedbacker.view;


import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.antonborries.feedbacker.R;


/**
 * A Fragment to Display the Profile Information
 */
public class ProfileFragment extends Fragment {

    private RecyclerView mFeedbackLayout;


    //USe ProfileFragment.newInstance() instead
    public ProfileFragment() {
        // Required empty public constructor
    }

    public static ProfileFragment newInstance() {

        return new ProfileFragment();
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
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        mFeedbackLayout = (RecyclerView) view.findViewById(R.id.profile_feedback);

        return view;
    }

    /**
     * Initializes the RecyclerView
     */
    private void getFeedbackList() {
        mFeedbackLayout.setAdapter(new FeedbackAdapter());
        mFeedbackLayout.setNestedScrollingEnabled(true);
        mFeedbackLayout.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, true));
        mFeedbackLayout.addItemDecoration(new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL));
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getFeedbackList();
    }

}
