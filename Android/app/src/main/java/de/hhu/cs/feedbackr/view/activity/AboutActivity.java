package de.hhu.cs.feedbackr.view.activity;

import android.app.ActionBar;
import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.AttributeSet;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.AbsoluteLayout;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import de.hhu.cs.feedbackr.R;

/**
 * Activity for Displaying a Feedback and Editing it
 */
public class AboutActivity extends AppCompatActivity {
    /**
     * Creates the Activity
     *
     * @param savedInstanceState Saved Instance of the Activity
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        Toolbar toolbar = findViewById(R.id.about_toolbar);
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

    /**
     * Show the about dialog
     *
     * @param view unused
     */
    public void showLicense(View view) {
        // get the webView and load the about info
        WebView about = new WebView(getApplicationContext());
        about.loadUrl("file:///android_asset/license.html");
        about.setHorizontalScrollBarEnabled(false);

        // show the dialog
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.license))
                .setPositiveButton(R.string.ok, null)
                .setView(about)
                .show();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}