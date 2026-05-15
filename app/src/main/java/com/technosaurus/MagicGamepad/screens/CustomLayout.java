package com.technosaurus.MagicGamepad.screens;
import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import java.util.HashSet;
import java.util.Set;

public class CustomLayout extends ViewGroup {

    public int parentWidth;
    public int parentHeight;

    public CustomLayout(Context context) {
        super(context);
    }

    public CustomLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CustomLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        parentWidth = getWidth();
        parentHeight = getHeight();
        Log.d("Layout height", String.valueOf(parentHeight));
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);

            // Get the layout parameters for the child view
            LayoutParams layoutParams = (LayoutParams) child.getLayoutParams();

            // Calculate the child's left and top position based on center (0,0) as origin
            int left = (parentWidth / 2) + layoutParams.x - (child.getMeasuredWidth() / 2);
            int top = (parentHeight / 2) + layoutParams.y - (child.getMeasuredHeight() / 2);

            // Layout the child view
            child.layout(left, top, left + child.getMeasuredWidth(), top + child.getMeasuredHeight());
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);

        measureChildren(widthMeasureSpec, heightMeasureSpec);
        setMeasuredDimension(width, height);
    }

    @Override
    protected LayoutParams generateLayoutParams(ViewGroup.LayoutParams lp) {
        return new LayoutParams(lp);
    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    @Override
    protected LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    // Custom LayoutParams to store the (x, y) coordinates
    public static class LayoutParams extends ViewGroup.LayoutParams {
        public int x = 0;
        public int y = 0;

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
        }

        public LayoutParams(int width, int height) {
            super(width, height);
        }

        public LayoutParams(ViewGroup.LayoutParams source) {
            super(source);
        }
    }

    // Function to move a child view by (x, y) coordinates
    public void moveViewTo(View view, int deltaX, int deltaY) {
        LayoutParams layoutParams = (LayoutParams) view.getLayoutParams();
        layoutParams.x += deltaX;
        layoutParams.y += deltaY;
        view.setLayoutParams(layoutParams);
        requestLayout();
    }

    public void moveViewToWithBoundaryCheck(View view, int deltaX, int deltaY) {
        LayoutParams layoutParams = (LayoutParams) view.getLayoutParams();

        // Calculate the new x and y positions
        int newX = layoutParams.x + deltaX;
        int newY = layoutParams.y + deltaY;

        // Calculate the left, top, right, and bottom positions based on the new coordinates
        int left = (parentWidth / 2) + newX - (view.getMeasuredWidth() / 2);
        int top = (parentHeight / 2) + newY - (view.getMeasuredHeight() / 2);
        int right = left + view.getMeasuredWidth();
        int bottom = top + view.getMeasuredHeight();

        // Check and adjust for boundaries
        if (left < 0) {
            newX = -(parentWidth / 2) + (view.getMeasuredWidth() / 2);
        } else if (right > parentWidth) {
            newX = (parentWidth / 2) - (view.getMeasuredWidth() / 2);
        }

        if (top < 0) {
            newY = -(parentHeight / 2) + (view.getMeasuredHeight() / 2);
        } else if (bottom > parentHeight) {
            newY = (parentHeight / 2) - (view.getMeasuredHeight() / 2);
        }

        // Update the layout parameters with the new (x, y) coordinates
        layoutParams.x = newX;
        layoutParams.y = newY;
        view.setLayoutParams(layoutParams);
        requestLayout();
    }

    public int[] getViewCoordinates(View view) {
        LayoutParams layoutParams = (LayoutParams) view.getLayoutParams();
        int[] coordinates = new int[2];
        coordinates[0] = layoutParams.x;
        coordinates[1] = layoutParams.y;
        return coordinates;
    }
    public boolean isViewTouchingBoundary(View view) {
        LayoutParams layoutParams = (LayoutParams) view.getLayoutParams();

        // Calculate the left, top, right, and bottom positions of the view
        int left = (parentWidth / 2) + layoutParams.x - (view.getMeasuredWidth() / 2);
        int top = (parentHeight / 2) + layoutParams.y - (view.getMeasuredHeight() / 2);
        int right = left + view.getMeasuredWidth();
        int bottom = top + view.getMeasuredHeight();

        // Check if the view is touching or exceeding the parent's boundaries
        return left <= 0 || top <= 0 || right >= parentWidth || bottom >= parentHeight;
    }
    public Set<String> getTouchedBoundaries(View view) {
        LayoutParams layoutParams = (LayoutParams) view.getLayoutParams();

        // Calculate the left, top, right, and bottom positions of the view
        int left = (parentWidth / 2) + layoutParams.x - (view.getMeasuredWidth() / 2);
        int top = (parentHeight / 2) + layoutParams.y - (view.getMeasuredHeight() / 2);
        int right = left + view.getMeasuredWidth();
        int bottom = top + view.getMeasuredHeight();

        // Set to store which boundaries are touched
        Set<String> touchedBoundaries = new HashSet<>();

        // Check each boundary and add to the set if touched
        if (left <= 0) {
            touchedBoundaries.add("LEFT");
        }
        if (top <= 0) {
            touchedBoundaries.add("TOP");
        }
        if (right >= parentWidth) {
            touchedBoundaries.add("RIGHT");
        }
        if (bottom >= parentHeight) {
            touchedBoundaries.add("BOTTOM");
        }

        return touchedBoundaries;
    }


    public void setViewSize(View view, int width, int height) {
        LayoutParams layoutParams = (LayoutParams) view.getLayoutParams();
        layoutParams.width = width;
        layoutParams.height = height;
        view.setLayoutParams(layoutParams);
        requestLayout(); // Request a layout pass to apply the new size
    }

    public int[][] getViewCornerCoordinates(View view) {
        LayoutParams layoutParams = (LayoutParams) view.getLayoutParams();

        // Calculate the left and top positions based on the parent's center and view's position
        int left = (parentWidth / 2) + layoutParams.x - (view.getMeasuredWidth() / 2);
        int top = (parentHeight / 2) + layoutParams.y - (view.getMeasuredHeight() / 2);

        // Calculate the right and bottom positions
        int right = left + view.getMeasuredWidth();
        int bottom = top + view.getMeasuredHeight();

        // Coordinates of the corners: top-left, top-right, bottom-left, bottom-right
        int[][] corners = {
                {left, top},      // Top-left corner
                {right, top},     // Top-right corner
                {left, bottom},   // Bottom-left corner
                {right, bottom}   // Bottom-right corner
        };

        return corners;
    }

    public void scaleViewSize(View view, float scaleFactor, float aspectRatio) {
        // Get the current layout parameters of the view
        ViewGroup.LayoutParams layoutParams = view.getLayoutParams();

        // Convert 55dp to pixels
        int minSize = (int) (50 * view.getResources().getDisplayMetrics().density);
        Log.d("minSize"," "+minSize);

        // Calculate new width and height based on the scale factor
        int newWidth = (int) (view.getMeasuredWidth() * scaleFactor);
        int newHeight = (int) (view.getMeasuredHeight() * scaleFactor);

        // Adjust dimensions to maintain the aspect ratio
        if (newWidth / (float) newHeight > aspectRatio) {
            // Width is too large compared to height, adjust width
            newWidth = (int) (newHeight * aspectRatio);
        } else {
            // Height is too large compared to width, adjust height
            newHeight = (int) (newWidth / aspectRatio);
        }

        if(newWidth>=minSize&&newHeight>=minSize) {
            // Update the layout parameters with the new dimensions
            layoutParams.width = newWidth;
            layoutParams.height = newHeight;
            view.setLayoutParams(layoutParams);
            // Request layout to apply the changes
            view.requestLayout();
            Log.d("scale","true");
        }
        else{
            Log.d("scale","false");
            Log.d("","width:"+layoutParams.width / view.getResources().getDisplayMetrics().density+" height:"+layoutParams.height / view.getResources().getDisplayMetrics().density);
//            if(scaleFactor>0) {
//                layoutParams.width = (int) (newWidth * scaleFactor);
//                layoutParams.height = (int) (newHeight * scaleFactor);
//                view.setLayoutParams(layoutParams);
//                // Request layout to apply the changes
//                view.requestLayout();
//            }
        }
    }




    public int getViewHeight(View view) {
        // Force the view to measure itself
        view.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED);
        return view.getMeasuredHeight();
    }
    public int getViewWidth(View view) {
        // Force the view to measure itself
        view.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED);
        return view.getMeasuredWidth();
    }
    // Function to show a view
    public void showView(View view) {
        view.setVisibility(View.VISIBLE);
        View rootView = this.getRootView();
        rootView.requestLayout();
        rootView.invalidate();
    }

    public void hideView(View view) {
        View rootView = this.getRootView();
        view.setVisibility(View.GONE);
        rootView.requestLayout();
        rootView.invalidate();
    }


}
