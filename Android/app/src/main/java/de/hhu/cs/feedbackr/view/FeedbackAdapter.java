package de.hhu.cs.feedbackr.view;

import android.databinding.DataBindingUtil;
import android.support.v7.util.SortedList;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.util.SortedListAdapterCallback;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.concurrent.Executors;

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
    private HashMap<String, Feedback> data = new HashMap<>();
    private final SortedList<Feedback> mFeedback = new SortedList(Feedback.class, new SortedListAdapterCallback(this) {
        @Override
        public int compare(Object o1, Object o2) {
            return (((Feedback) o2).getDate().compareTo(((Feedback) o1).getDate()));
        }

        @Override
        public boolean areContentsTheSame(Object oldItem, Object newItem) {
            return false;
        }

        @Override
        public boolean areItemsTheSame(Object item1, Object item2) {
            return item1 == item2;
        }
    });

    private static FeedbackAdapter instance;

    public static synchronized FeedbackAdapter getInstance() {
        if (instance == null) {
            instance = new FeedbackAdapter();
        }
        return instance;
    }

    /**
     * Creates the adapter
     */
    public FeedbackAdapter() {
        DatabaseReference userRef = FirebaseHelper.getUserRef();

        if (userRef != null) {
            // this is a hack to make the list stay at top
            // we insert the last item (most likely the newest) as first to make the view focus on it
            userRef.child("feedback").limitToLast(1).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {

                    for (DataSnapshot ds : dataSnapshot.getChildren()) {
                        String key = ds.getKey();
                        FirebaseHelper.getFeedback().child(key).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                Feedback feedback = dataSnapshot.getValue(Feedback.class);

                                if (feedback != null) {
                                    if (!data.containsKey(feedback.getId())) {
                                        mFeedback.add(feedback);
                                        data.put(feedback.getId(), feedback);

//                                    if (feedback.isHasPhoto()) {
//                                        FirebaseStorageHelper.loadImage(feedback);
//                                    }
                                    } else {
                                        // update Feedback
                                        mFeedback.updateItemAt(mFeedback.indexOf(data.get(feedback.getId())), feedback);
                                        data.put(feedback.getId(), feedback);
                                    }
                                }
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {

                            }
                        });
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });

            //Listen to Changes in the Feedback Section of the User
            userRef.child("feedback").addChildEventListener(new ChildEventListener() {
                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                    //For new Items Get the Feedback from the Feedback Section. Gets called when first Loaded
                    FirebaseHelper.getFeedback().child(dataSnapshot.getKey()).addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            Feedback feedback = dataSnapshot.getValue(Feedback.class);
                            if (feedback != null) {
                                if (!data.containsKey(feedback.getId())) {
                                    mFeedback.add(feedback);
                                    data.put(feedback.getId(), feedback);

//                                    if (feedback.isHasPhoto()) {
//                                        FirebaseStorageHelper.loadImage(feedback);
//                                    }
                                } else {
                                    // update Feedback
                                    mFeedback.updateItemAt(mFeedback.indexOf(data.get(feedback.getId())), feedback);
                                    data.put(feedback.getId(), feedback);
                                }
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    });
                }

                @Override
                public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                }

                @Override
                public void onChildRemoved(DataSnapshot dataSnapshot) {
                    //Delete the Feedback from the List
                    String key = dataSnapshot.getKey();
                    if (data.containsKey(key)) {
                        mFeedback.remove(data.remove(key));
                        notifyDataSetChanged();
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
        binding.getRoot().setOnClickListener(view -> {
            // todo feedback holds an old reference causing the profile attachment not to be shown correctly
            ((MainActivity) view.getContext()).switchToFeedbackDetail(feedback);
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
