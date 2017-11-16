package de.hhu.cs.feedbackr.view;

import android.content.DialogInterface;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import de.hhu.cs.feedbackr.R;
import de.hhu.cs.feedbackr.databinding.DialogSwitchBinding;
import de.hhu.cs.feedbackr.model.Feedback;
import de.hhu.cs.feedbackr.model.FirebaseHelper;

/**
 * Activity for Displaying a Feedback and Editing it
 */
public class FeedbackEditActivity extends AppCompatActivity {

    /**
     * The Fragment The Activity is Hosting
     */
    FeedbackEditFragment mFeedbackEditFragment;

    /**
     * Creates the Activity
     *
     * @param savedInstanceState Saved Instance of the Activity
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feedback_edit);
        Toolbar toolbar = (Toolbar) findViewById(R.id.detail_toolbar);
        setSupportActionBar(toolbar);
        assert getSupportActionBar() != null;
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(ContextCompat.getDrawable(this, R.drawable.ic_close_black_24dp));

        findViewById(R.id.detail_save).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveChanges();
            }
        });

        //Cancel just acts as if the Back Button was pressed
        findViewById(R.id.detail_cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        mFeedbackEditFragment = FeedbackEditFragment.newInstance((Feedback) getIntent().getExtras().get("Feedback"));
        getSupportFragmentManager().beginTransaction().add(R.id.feedback_detail_frame, mFeedbackEditFragment).commit();
    }

    /**
     * Inflates the Menu
     *
     * @param menu Menu of the Activity
     * @return true
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_edit, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.menu_edit_switch:
                switchFeedback();
                return true;
            case R.id.menu_edit_delete:
                deleteAlarm();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Switch Feedback between Positive and Negative
     */
    private void switchFeedback() {
        DialogSwitchBinding binding = DataBindingUtil.inflate(getLayoutInflater(), R.layout.dialog_switch, null, false);
        binding.setFeedback(mFeedbackEditFragment.getFeedback());
        //Create a warning Dialog
        AlertDialog switchDialog = new AlertDialog.Builder(this)
                .setView(binding.getRoot())
                .setPositiveButton(R.string.switch_word, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        mFeedbackEditFragment.switchKind();
                        mFeedbackEditFragment.updateMarker();
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.cancel();
                    }
                }).create();
        switchDialog.show();
    }

    /**
     * Handles the deletion of a Feedback
     */
    private void deleteAlarm() {
        AlertDialog deleteDialog = new AlertDialog.Builder(this)
                //set message, title, and icon
                .setTitle(R.string.delete)
                .setMessage(R.string.delete_confirmation)

                .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int whichButton) {
                        FirebaseHelper.deleteFeedback(mFeedbackEditFragment.getFeedback());
                        dialog.dismiss();
                        finish();
                    }

                })

                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {

                        dialog.dismiss();
                    }
                })
                .create();
        deleteDialog.show();
    }

    /**
     * Saves the Changes Made in the Feedback
     */
    private void saveChanges() {
        Feedback feedback = mFeedbackEditFragment.getFeedback();
        FirebaseHelper.saveFeedback(feedback);

        // Show Success Toast
        Toast.makeText(this, String.format(getString(R.string.feedback_send),
                feedback.isPositive() ? getString(R.string.positive) : getString(R.string.negative)), Toast.LENGTH_LONG).show();

        finish();
    }

    @Override
    public void onBackPressed() {
        //Confirm loosing Changes when pressing back
        AlertDialog quitDialog = new AlertDialog.Builder(this)
                .setTitle(R.string.save_changes)
                .setMessage(R.string.save_warning)
                .setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        saveChanges();
                    }
                }).setNegativeButton(R.string.discard, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        FeedbackEditActivity.super.onBackPressed();
                    }
                }).create();
        quitDialog.show();
    }
}
