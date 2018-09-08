package de.hhu.cs.feedbackr.view.fragment;


import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.util.Objects;

import de.hhu.cs.feedbackr.R;
import de.hhu.cs.feedbackr.view.activity.MainActivity;


/**
 * A Class To Display the Send Feedback Buttons
 */
public class FeedbackSendFragment extends Fragment {

    public FeedbackSendFragment() {
        // Required empty public constructor
    }

    /**
     * Creates the View
     *
     * @param inflater           Inflater
     * @param container          Container
     * @param savedInstanceState SavedInstance
     * @return View of the Fragment
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_feedback_send, container, false);

        view.findViewById(R.id.posneg).setOnTouchListener((View v, MotionEvent event) -> {
            ImageView imageView = ((ImageView) v);
            Bitmap bitmap = ((BitmapDrawable) imageView.getDrawable()).getBitmap();

            // hint only scale proportional
            int imageW = imageView.getWidth();
            int bitmapW = bitmap.getWidth();

            double scale = (double) bitmapW / imageW;

            int x = (int) Math.floor(event.getX() * scale);
            int  y = (int) Math.floor(event.getY() * scale);

            int pixel = bitmap.getPixel(x, y);

            int green = Color.green(pixel);
            int red = Color.red(pixel);

            if (green > 150) {
                ((MainActivity) Objects.requireNonNull(getActivity())).sendFeedback(true);
            } else if (red > 150) {
                ((MainActivity) Objects.requireNonNull(getActivity())).sendFeedback(false);
            }

            return v.performClick();
        });

        return view;
    }
}
