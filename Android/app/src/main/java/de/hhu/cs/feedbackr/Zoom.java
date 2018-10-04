package de.hhu.cs.feedbackr;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.graphics.Point;
import android.graphics.Rect;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;

/**
 * This class allows to zoom in views. This function is used to show dialogs and enlarge thumbnails
 * <p>
 * The code is based on the following implementation:
 * https://developer.android.com/training/animation/zoom
 * Apache 2.0 license
 * <p>
 * The original code was edited to meet the applications requirements
 */
public class Zoom {
    private Animator animator;
    private AnimationEndListener animationEndListener;
    private int duration = 200;

    /**
     * The listener will perform the {@link AnimationEndListener#end()} method after the zoomed view
     * is zoomed out again in the {@link #zoomView(View, View)} method
     *
     * @param animationEndListener
     */
    public void setAnimationEndListener(AnimationEndListener animationEndListener) {
        this.animationEndListener = animationEndListener;
    }

    /**
     * Zooms the expander from scale 0 to scale 1 the actual size is determined by the surrounding
     * container and its dimensions.
     *
     * @param expander  view to be zoom
     * @param container container determining the size of the view to be zoomed to
     */
    public void zoomView(View expander, View container) {
        // If there's an animation in progress, cancel it
        // immediately and proceed with this one.
        if (animator != null) {
            animator.cancel();
        }

        // Calculate the starting and ending bounds for the zoomed-in image.
        // This step involves lots of math. Yay, math.
        final Rect startBounds = new Rect();
        final Rect finalBounds = new Rect();
        final Point globalOffset = new Point();

        // we want the view to start from the center of the screen and end there as well
        // The final bounds are the global visible rectangle of the container view
        container.getGlobalVisibleRect(finalBounds, globalOffset);
        // set the offset for the final bounds
        finalBounds.offset(-globalOffset.x, -globalOffset.y);

        // Show the expander
        expander.setVisibility(View.VISIBLE);

        // Construct and run the parallel animation of the four translation and
        // scale properties (X, Y, SCALE_X, and SCALE_Y).
        AnimatorSet set = new AnimatorSet();
        set
                .play(ObjectAnimator.ofFloat(expander, View.X, startBounds.left, finalBounds.left))
                .with(ObjectAnimator.ofFloat(expander, View.Y, startBounds.top, finalBounds.top))
                // The StartScale is always 0 and the end scaling factor is always 1.0
                .with(ObjectAnimator.ofFloat(expander, View.SCALE_X, 0f, 1f))
                .with(ObjectAnimator.ofFloat(expander, View.SCALE_Y, 0f, 1f));
        set.setDuration(duration);
        set.setInterpolator(new DecelerateInterpolator());
        set.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                animator = null;
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                animator = null;
            }
        });
        animator = set;
        set.start();

        // Upon clicking the zoomed-in expander, it should zoom back down
        // to the original bounds and disappear
        expander.setOnClickListener(view -> {
            if (animator != null) {
                animator.cancel();
            }

            // Animate the four positioning/sizing properties in parallel,
            // back to their original values.
            AnimatorSet set1 = new AnimatorSet();
            set1.play(ObjectAnimator
                    .ofFloat(expander, View.X, startBounds.left))
                    .with(ObjectAnimator.ofFloat(expander, View.Y, startBounds.top))
                    .with(ObjectAnimator.ofFloat(expander, View.SCALE_X, 0F))
                    .with(ObjectAnimator.ofFloat(expander, View.SCALE_Y, 0F));
            set1.setDuration(duration);
            set1.setInterpolator(new DecelerateInterpolator());
            set1.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    expander.setVisibility(View.GONE);
                    animator = null;

                    if (animationEndListener != null) {
                        animationEndListener.end();
                    }
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                    expander.setVisibility(View.GONE);
                    animator = null;

                    if (animationEndListener != null) {
                        animationEndListener.end();
                    }
                }
            });
            animator = set1;
            set1.start();
        });
    }


    /**
     * Zooms the given expandedImage from its corresponding thumbView into the dimensions of the given
     * container
     *
     * @param thumbView     the image thumbnail
     * @param expandedImage the wrapper in which the thumbnail should be expanded
     */
    public void zoomImageFromThumb(final ImageView thumbView, View expandedImage, View container) {
        // If there's an animation in progress, cancel it
        // immediately and proceed with this one.
        if (animator != null) {
            animator.cancel();
        }

        // Calculate the starting and ending bounds for the zoomed-in image.
        // This step involves lots of math. Yay, math.
        final Rect startBounds = new Rect();
        final Rect finalBounds = new Rect();
        final Point globalOffset = new Point();

        // The start bounds are the global visible rectangle of the thumbnail,
        // and the final bounds are the global visible rectangle of the container
        // view. Also set the container view's offset as the origin for the
        // bounds, since that's the origin for the positioning animation
        // properties (X, Y).
        thumbView.getGlobalVisibleRect(startBounds);
        container.getGlobalVisibleRect(finalBounds, globalOffset);
        startBounds.offset(-globalOffset.x, -globalOffset.y);
        finalBounds.offset(-globalOffset.x, -globalOffset.y);

        // Adjust the start bounds to be the same aspect ratio as the final
        // bounds using the "center crop" technique. This prevents undesirable
        // stretching during the animation. Also calculate the start scaling
        // factor (the end scaling factor is always 1.0).
        float startScale;
        if ((float) finalBounds.width() / finalBounds.height() > (float) startBounds.width() / startBounds.height()) {
            // Extend start bounds horizontally
            startScale = (float) startBounds.height() / finalBounds.height();
            float startWidth = startScale * finalBounds.width();
            float deltaWidth = (startWidth - startBounds.width()) / 2;
            startBounds.left -= deltaWidth;
            startBounds.right += deltaWidth;
        } else {
            // Extend start bounds vertically
            startScale = (float) startBounds.width() / finalBounds.width();
            float startHeight = startScale * finalBounds.height();
            float deltaHeight = (startHeight - startBounds.height()) / 2;
            startBounds.top -= deltaHeight;
            startBounds.bottom += deltaHeight;
        }

        // Hide the thumbnail and show the zoomed-in view. When the animation
        // begins, it will position the zoomed-in view in the place of the
        // thumbnail.
        thumbView.setAlpha(0f);
        expandedImage.setVisibility(View.VISIBLE);

        // Set the pivot point for SCALE_X and SCALE_Y transformations
        // to the top-left corner of the zoomed-in view (the default
        // is the center of the view).
        expandedImage.setPivotX(1f);
        expandedImage.setPivotY(0f);

        // Construct and run the parallel animation of the four translation and
        // scale properties (X, Y, SCALE_X, and SCALE_Y).
        AnimatorSet set = new AnimatorSet();
        set
                .play(ObjectAnimator.ofFloat(expandedImage, View.X, startBounds.left, finalBounds.left))
                .with(ObjectAnimator.ofFloat(expandedImage, View.Y, startBounds.top, finalBounds.top))
                .with(ObjectAnimator.ofFloat(expandedImage, View.SCALE_X, startScale, 1f))
                .with(ObjectAnimator.ofFloat(expandedImage, View.SCALE_Y, startScale, 1f));
        set.setDuration(200);
        set.setInterpolator(new DecelerateInterpolator());
        set.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                animator = null;
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                animator = null;
            }
        });
        animator = set;
        set.start();

        // Upon clicking the zoomed-in image, it should zoom back down
        // to the original bounds and show the thumbnail instead of
        // the expanded image.
        final float startScaleFinal = startScale;
        expandedImage.setOnClickListener(view -> {
            if (animator != null) {
                animator.cancel();
            }

            // Animate the four positioning/sizing properties in parallel,
            // back to their original values.
            AnimatorSet set1 = new AnimatorSet();
            set1.play(ObjectAnimator
                    .ofFloat(expandedImage, View.X, startBounds.left))
                    .with(ObjectAnimator.ofFloat(expandedImage, View.Y, startBounds.top))
                    .with(ObjectAnimator.ofFloat(expandedImage, View.SCALE_X, startScaleFinal))
                    .with(ObjectAnimator.ofFloat(expandedImage, View.SCALE_Y, startScaleFinal));
            set1.setDuration(200);
            set1.setInterpolator(new DecelerateInterpolator());
            set1.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    thumbView.setAlpha(1f);
                    expandedImage.setVisibility(View.GONE);
                    animator = null;
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                    thumbView.setAlpha(1f);
                    expandedImage.setVisibility(View.GONE);
                    animator = null;
                }
            });
            animator = set1;
            set1.start();
        });
    }

    /**
     * Listener for the that is executed on the dismiss of the {@link #zoomImageFromThumb(ImageView, View, View)} method
     */
    public interface AnimationEndListener {
        void end();
    }
}
