package com.antonborries.feedbacker.view;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.antonborries.feedbacker.R;


/**
 * A Class To Display the Send Feedback Buttons
 */
public class FeedbackSendFragment extends Fragment {

    /**
     * Use FeedbackSendFragment.newInstance() instead
     */
    public FeedbackSendFragment() {
        // Required empty public constructor
    }

    /**
     * Creates a new Instance of the Fragment
     *
     * @return Instance of the FeedbackSendFragment
     */
    public static FeedbackSendFragment newInstance() {
        return new FeedbackSendFragment();
    }

    /**
     * Creates the View
     *
     * @param inflater           Inflater
     * @param container          Container
     * @param savedInstanceState SavedInstance
     * @return View of the Fragment
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_feedback_send, container, false);
        view.findViewById(R.id.buttonPositive).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ((MainActivity) getActivity()).sendFeedback(true);
            }
        });
        view.findViewById(R.id.buttonNegative).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ((MainActivity) getActivity()).sendFeedback(false);
            }
        });
        return view;
    }

}
