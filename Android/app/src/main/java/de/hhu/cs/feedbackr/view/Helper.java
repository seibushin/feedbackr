package de.hhu.cs.feedbackr.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.DrawableRes;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;

import java.util.Objects;

import de.hhu.cs.feedbackr.R;
import de.hhu.cs.feedbackr.model.CategoryConverter;

public class Helper {
    /**
     * @param context    Context for getting Resources
     * @param isPositive Kind of Feedback
     * @param tag        Category of Feedback
     * @return Bitmap to Display
     */
    public static Bitmap getBitmap(Context context, boolean isPositive, String tag) {
        @DrawableRes int drawableId;
        drawableId = CategoryConverter.tagToDrawable(tag);
        Drawable drawable = ContextCompat.getDrawable(context, drawableId);
        DrawableCompat.setTint(Objects.requireNonNull(drawable), isPositive ? ContextCompat.getColor(context, R.color.green) : ContextCompat.getColor(context, R.color.red));
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            drawable = (DrawableCompat.wrap(drawable)).mutate();
        }

        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        double scaleFactor = 0.9;
        return Bitmap.createScaledBitmap(bitmap, (int) (bitmap.getWidth() * scaleFactor), (int) (bitmap.getHeight() * scaleFactor), false);
    }
}
