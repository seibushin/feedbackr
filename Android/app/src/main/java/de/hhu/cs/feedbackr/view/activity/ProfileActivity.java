package de.hhu.cs.feedbackr.view.activity;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import de.hhu.cs.feedbackr.R;
import de.hhu.cs.feedbackr.databinding.ActivityProfileBinding;
import de.hhu.cs.feedbackr.firebase.FirebaseHelper;
import de.hhu.cs.feedbackr.model.Profile;

/**
 * The profile activity allows the user to edit his profile and choose if the profile should be
 * attached to the given feedback per default.
 */
public class ProfileActivity extends AppCompatActivity {
    /**
     * Creates the Activity
     *
     * @param savedInstanceState Saved Instance of the Activity
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Profile.getInstance() == null) {
            Profile.setInstanceProfile(new Profile());
        }

        ActivityProfileBinding binding = DataBindingUtil.setContentView(this, R.layout.activity_profile);
        binding.setProfile(Profile.getInstance());

        Toolbar toolbar = findViewById(R.id.profile_toolbar);
        setSupportActionBar(toolbar);
        assert getSupportActionBar() != null;
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        FirebaseHelper.saveProfile(Profile.getInstance());
        super.onBackPressed();
    }
}