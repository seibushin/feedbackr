package de.hhu.cs.feedbackr.view;

import android.databinding.DataBindingUtil;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import de.hhu.cs.feedbackr.R;
import de.hhu.cs.feedbackr.databinding.FeedbackHolderBinding;
import de.hhu.cs.feedbackr.model.CategoryConverter;
import de.hhu.cs.feedbackr.model.Feedback;
import de.hhu.cs.feedbackr.model.FirebaseHelper;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

/**
 * RecyclerView.Adapter for Created Feedback Holder
 */

public class FeedbackAdapter extends RecyclerView.Adapter {
    private final ArrayList<Feedback> mFeedback;

    /**
     * Creates the adapter
     */
    public FeedbackAdapter() {
        mFeedback = new ArrayList<>();
        DatabaseReference userRef = FirebaseHelper.getUserRef();
        if (userRef != null) {
            //Listen to Changes in the Feedback Section of the User
            userRef.child("feedback").addChildEventListener(new ChildEventListener() {
                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                    //For new Items Get the Feedback from the Feedback Section. Gets called when first Loaded
                    FirebaseHelper.getFeedback().child(dataSnapshot.getKey()).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            Feedback feedback = dataSnapshot.getValue(Feedback.class);
                            if (feedback != null) {
                                mFeedback.add(feedback);
                                notifyItemInserted(mFeedback.size() - 1);
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    });
                }

                @Override
                public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                    //When a Feedback Changed get it from the Feedback Section
                    FirebaseHelper.getFeedback().child(dataSnapshot.getKey()).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            Feedback feedback = dataSnapshot.getValue(Feedback.class);
                            for (Feedback old : mFeedback) {
                                //Swap the Old Feedback with the new One
                                if (feedback != null && old.getId().equals(feedback.getId())) {
                                    int pos = mFeedback.indexOf(old);
                                    mFeedback.set(pos, feedback);
                                    notifyItemChanged(pos);
                                    break;
                                }
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    });
                }

                @Override
                public void onChildRemoved(DataSnapshot dataSnapshot) {
                    //Delete the Feedback from the List
                    String key = dataSnapshot.getKey();
                    for (int i = 0; i < mFeedback.size(); i++) {
                        if (mFeedback.get(i).getId().equals(key)) {
                            mFeedback.remove(i);
                            notifyItemRemoved(i);
                        }
                    }
                }

                @Override
                public void onChildMoved(DataSnapshot dataSnapshot, String s) {

                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        }
    }

    /**
     * Creates the ViewHolder by inflating A ViewHolder using Databinding
     *
     * @param parent   Parent
     * @param viewType ViewType
     * @return ViewHolder is instance of FeedbackHolder
     */
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        FeedbackHolderBinding binding = DataBindingUtil.inflate(LayoutInflater.from(parent.getContext()), R.layout.feedback_holder, parent, false);
        return new FeedbackHolder(binding);
    }

    /**
     * Binds the ViewHolder to the List of Feedback
     *
     * @param holder   ViewHolder should be Instance of FeedbackHolder
     * @param position Position of the Feedback in the List
     */
    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, int position) {
        FeedbackHolderBinding binding = ((FeedbackHolder) holder).getBinding();
        final Feedback feedback = mFeedback.get(position);
        binding.setFeedback(feedback);
        binding.getRoot().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ((MainActivity) view.getContext()).switchToFeedbackDetail(feedback);

            }
        });
        binding.feedbackHolderImg.setImageResource(CategoryConverter.tagToDrawable(feedback.getCategory()));
    }

    /**
     * @return Amount of Feedback in the List
     */
    @Override
    public int getItemCount() {
        return mFeedback.size();
    }
}
