package org.zimmob.zimlx.graphics;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;

public class NinePatchDrawHelper {

    // The extra width used for the bitmap. This portion of the bitmap is stretched to match the
    // width of the draw region. Randomly chosen, any value > 4 will be sufficient.
    public static final int EXTENSION_PX = 20;

    public final RectF mDst = new RectF();
    public final Rect mSrc = new Rect();
    public final Paint paint = new Paint(1);

    public final void draw(Bitmap bitmap, Canvas canvas, float left, float top, float right) {
        int height = bitmap.getHeight();
        mSrc.top = 0;
        mSrc.bottom = height;
        mDst.top = top;
        mDst.bottom = top + ((float) height);
        draw3Patch(bitmap, canvas, left, right);
    }

    public final void draw3Patch(Bitmap bitmap, Canvas canvas, float left, float right) {
        int width = bitmap.getWidth();
        int center = width / 2;
        float leftWidth = left + center;
        drawRegion(bitmap, canvas, 0, center, left, leftWidth);
        float rightWidth = right - center;
        drawRegion(bitmap, canvas, center, width, rightWidth, right);
        drawRegion(bitmap, canvas, center - 5, center + 5, leftWidth, rightWidth);
    }

    private void drawRegion(Bitmap bitmap, Canvas canvas, int srcL, int srcR, float dstL, float dstR) {
        mSrc.left = srcL;
        mSrc.right = srcR;
        mDst.left = dstL;
        mDst.right = dstR;
        canvas.drawBitmap(bitmap, mSrc, mDst, paint);
    }

    public void drawVerticallyStretched(Bitmap bitmap, Canvas canvas, float left, float top,
                                        float right, float bottom) {
        draw(bitmap, canvas, left, top, right);

        // Draw bottom stretched region.
        int height = bitmap.getHeight();
        mSrc.top = height - EXTENSION_PX / 4;
        mSrc.bottom = height;
        mDst.top = top + height;
        mDst.bottom = bottom;
        draw3Patch(bitmap, canvas, left, right);
    }
}