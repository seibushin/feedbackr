package de.hhu.cs.feedbackr.view.activity;

import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;

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

        WebView webView = findViewById(R.id.about_web);
        webView.loadUrl("file:///android_asset/about.html");
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
        //WebView about = (WebView) LayoutInflater.from(getApplicationContext()).inflate(R.layout.dialog_about, null);
        about.loadUrl("file:///android_asset/license.html");

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