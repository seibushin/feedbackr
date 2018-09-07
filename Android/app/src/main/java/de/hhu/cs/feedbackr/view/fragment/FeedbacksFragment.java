package de.hhu.cs.feedbackr.view.fragment;


import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import de.hhu.cs.feedbackr.R;
import de.hhu.cs.feedbackr.view.FeedbackAdapter;


/**
 * A Fragment to Display the Profile Information
 */
public class FeedbacksFragment extends Fragment {
    private RecyclerView mFeedbackLayout;

    public FeedbacksFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
        View view = inflater.inflate(R.layout.fragment_feedbacks, container, false);

        mFeedbackLayout = view.findViewById(R.id.profile_feedback);

        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getFeedbackList();
    }

    /**
     * Initializes the RecyclerView
     */
    private void getFeedbackList() {
        mFeedbackLayout.setAdapter(new FeedbackAdapter());
        mFeedbackLayout.setNestedScrollingEnabled(true);
        mFeedbackLayout.setLayoutManager(new LinearLayoutManager(getActivity()));
        mFeedbackLayout.addItemDecoration(new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL));
    }
}
