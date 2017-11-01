package de.hhu.cs.feedbackr.view;

import android.support.v7.widget.RecyclerView;

import de.hhu.cs.feedbackr.databinding.FeedbackHolderBinding;

/**
 * RecyclerView.ViewHolder for a Feedback Object
 */
public class FeedbackHolder extends RecyclerView.ViewHolder {

    private final FeedbackHolderBinding mBinding;

    /**
     * Creates the Holder
     *
     * @param binding Binding of View and Feedback
     */
    public FeedbackHolder(FeedbackHolderBinding binding) {
        super(binding.getRoot());
        mBinding = binding;
        mBinding.executePendingBindings();
    }

    /**
     * @return Binding of the View
     */
    public FeedbackHolderBinding getBinding() {
        return mBinding;
    }
}
