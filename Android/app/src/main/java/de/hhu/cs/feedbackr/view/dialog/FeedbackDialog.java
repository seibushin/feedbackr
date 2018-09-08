package de.hhu.cs.feedbackr.view.dialog;

import android.app.Dialog;
import android.content.DialogInterface;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;

import java.util.Objects;

import de.hhu.cs.feedbackr.R;
import de.hhu.cs.feedbackr.databinding.DialogFeedbackBinding;
import de.hhu.cs.feedbackr.model.Feedback;
import de.hhu.cs.feedbackr.view.activity.MainActivity;

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
        mFeedback = (Feedback) Objects.requireNonNull(getArguments()).get(FEEDBACK_KEY);
        mEditable = getArguments().getBoolean(USER_KEY);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        DialogFeedbackBinding binding = DataBindingUtil.inflate(LayoutInflater.from(getContext()), R.layout.dialog_feedback, null, false);
        binding.setFeedback(mFeedback);

        AlertDialog.Builder builder = new AlertDialog.Builder(Objects.requireNonNull(getActivity()));
        builder.setView(binding.getRoot());

        // If the Feedback is from the user add an edit button
        if (mEditable) {
            builder.setPositiveButton(R.string.edit, (dialogInterface, i) -> ((MainActivity) getActivity()).switchToFeedbackDetail(mFeedback));
        }

        return builder.create();
    }
}
