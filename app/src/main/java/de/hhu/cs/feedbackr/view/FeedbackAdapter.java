package de.hhu.cs.feedbackr.view;

import android.databinding.DataBindingUtil;
import android.support.annotation.NonNull;
import android.support.v7.util.SortedList;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.util.SortedListAdapterCallback;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;

import java.util.HashMap;
import java.util.Objects;

import de.hhu.cs.feedbackr.R;
import de.hhu.cs.feedbackr.databinding.FeedbackHolderBinding;
import de.hhu.cs.feedbackr.firebase.FirebaseHelper;
import de.hhu.cs.feedbackr.model.CategoryConverter;
import de.hhu.cs.feedbackr.model.Feedback;
import de.hhu.cs.feedbackr.view.activity.MainActivity;

/**
 * RecyclerView.Adapter for Created Feedback Holder
 */

public class FeedbackAdapter extends RecyclerView.Adapter {
    private OnSizeChangedListener listener;
    private HashMap<String, Feedback> data = new HashMap<>();
    private final SortedList<Feedback> mFeedback = new SortedList<>(Feedback.class, new SortedListAdapterCallback<Feedback>(this) {
        @Override
        public int compare(Feedback o1, Feedback o2) {
            return o2.getDate().compareTo(o1.getDate());
        }

        @Override
        public boolean areContentsTheSame(Feedback oldItem, Feedback newItem) {
            return false;
        }

        @Override
        public boolean areItemsTheSame(Feedback item1, Feedback item2) {
            return item1 == item2;
        }
    });


    /**
     * Creates the adapter
     */
    public FeedbackAdapter(OnSizeChangedListener listener2) {
        this.listener = listener2;
        // get all feedback of the user
        FirebaseHelper.getFeedbackRef().orderByChild("owner").equalTo(Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid()).addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, String s) {
                Feedback feedback = dataSnapshot.getValue(Feedback.class);

                if (feedback != null) {
                    // add Feedback
                    mFeedback.add(feedback);
                    data.put(feedback.getId(), feedback);
                    listener.changed();
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, String s) {
                Feedback feedback = dataSnapshot.getValue(Feedback.class);

                if (feedback != null) {
                    // update feedback
                    mFeedback.updateItemAt(mFeedback.indexOf(data.get(feedback.getId())), feedback);
                    data.put(feedback.getId(), feedback);
                }
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {
                // delete the feedback from the list
                String key = dataSnapshot.getKey();
                if (data.containsKey(key)) {
                    mFeedback.remove(data.remove(key));
                    notifyDataSetChanged();
                    listener.changed();
                }
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, String s) {
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
    }

    /**
     * Creates the ViewHolder by inflating A ViewHolder using Databinding
     *
     * @param parent   Parent
     * @param viewType ViewType
     * @return ViewHolder is instance of FeedbackHolder
     */
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
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
    public void onBindViewHolder(@NonNull final RecyclerView.ViewHolder holder, int position) {
        FeedbackHolderBinding binding = ((FeedbackHolder) holder).getBinding();
        final Feedback feedback = mFeedback.get(position);
        binding.setFeedback(feedback);
        binding.getRoot().setOnClickListener(view -> ((MainActivity) view.getContext()).switchToFeedbackDetail(feedback));
        binding.feedbackHolderImg.setImageResource(CategoryConverter.tagToDrawable(feedback.getCategory()));
    }

    /**
     * @return Amount of Feedback in the List
     */
    @Override
    public int getItemCount() {
        return mFeedback.size();
    }

    /**
     * Listener that reacts on changes in the data size
     */
    public interface OnSizeChangedListener {
        void changed();
    }
}
