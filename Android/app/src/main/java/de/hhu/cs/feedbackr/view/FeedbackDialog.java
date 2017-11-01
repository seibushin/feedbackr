package de.hhu.cs.feedbackr.view;

import android.app.Dialog;
import android.content.DialogInterface;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;

import de.hhu.cs.feedbackr.R;
import de.hhu.cs.feedbackr.databinding.DialogFeedbackBinding;
import de.hhu.cs.feedbackr.model.Feedback;

/**
 * Shows A Dialog For a Feedback which displays Information
 */

public class FeedbackDialog extends DialogFragment {

    public static final String FEEDBACK_KEY = "FEEDBACK";
    public static final String USER_KEY = "IS_USER_FEEDBACK";

    private Feedback mFeedback;
    private boolean mEditable;

    /**
     * Creates a Dialog with Information for a Feedback
     *
     * @param feedback     Feedback to be shown
     * @param isUsersAlarm true if User send the Feedback
     * @return FeedbackDialog
     */
    public static FeedbackDialog newInstance(Feedback feedback, boolean isUsersAlarm) {
        FeedbackDialog dialog = new FeedbackDialog();
        Bundle args = new Bundle();
        args.putSerializable(FEEDBACK_KEY, feedback);
        args.putBoolean(USER_KEY, isUsersAlarm);
        dialog.setArguments(args);
        return dialog;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mFeedback = (Feedback) getArguments().get(FEEDBACK_KEY);
        mEditable = getArguments().getBoolean(USER_KEY);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        DialogFeedbackBinding binding = DataBindingUtil.inflate(LayoutInflater.from(getContext()), R.layout.dialog_feedback, null, false);

        builder.setView(binding.getRoot());

        //If the Feedback is from the User add an Edit Button
        if (mEditable) {
            builder.setPositiveButton(R.string.edit, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    ((MainActivity) getActivity()).switchToFeedbackDetail(mFeedback);
                }
            });
        }

        binding.setFeedback(mFeedback);

        return builder.create();
    }


}
