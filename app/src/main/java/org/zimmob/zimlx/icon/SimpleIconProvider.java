package org.zimmob.zimlx.icon;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.TextView;

import org.zimmob.zimlx.config.Config;
import org.zimmob.zimlx.manager.Setup;
import org.zimmob.zimlx.util.Tool;
import org.zimmob.zimlx.folder.FolderIcon;

public class SimpleIconProvider extends BaseIconProvider{
    private int drawableResource;

    protected Drawable drawable;
    
    public SimpleIconProvider(Drawable drawable) {
        this.drawable = drawable;
        this.drawableResource = -1;
    }

    public SimpleIconProvider(int drawableResource) {
        this.drawable = null;
        this.drawableResource = drawableResource;
    }

    private Drawable getDrawable() {
        if (drawable != null) {
            return drawable;
        } else if (drawableResource > 0) {
            return Setup.appContext().getResources().getDrawable(drawableResource);
        }
        return null;
    }

    @Override
    public void loadIcon(IconTargetType type, int forceSize, Object target, Object... args) {
        switch (type) {
            case ImageView: {
                ImageView iv = (ImageView) target;
                Drawable d = getDrawable();
                d = scaleDrawable(d, forceSize);
                iv.setImageDrawable(d);
                break;
            }
            case TextView: {
                TextView tv = (TextView) target;
                int gravity = (Integer) args[0];
                Drawable d = getDrawable();
                d = scaleDrawable(d, forceSize);
                if (gravity == Gravity.LEFT || gravity == Gravity.START) {
                    tv.setCompoundDrawablesWithIntrinsicBounds(d, null, null, null);
                } else if (gravity == Gravity.RIGHT || gravity == Gravity.END) {
                    tv.setCompoundDrawablesWithIntrinsicBounds(null, null, d, null);
                } else if (gravity == Gravity.TOP) {
                    tv.setCompoundDrawablesWithIntrinsicBounds(null, d, null, null);
                } else if (gravity == Gravity.BOTTOM) {
                    tv.setCompoundDrawablesWithIntrinsicBounds(null, null, null, d);
                }
                break;
            }
            case IconDrawer: {
                IconDrawer iconDrawer = (IconDrawer) target;
                int index = (Integer) args[0];
                // we simply load the drawable in a synchronised way
                iconDrawer.onIconAvailable(getDrawable(), index);
                break;
            }
        }
    }

    @Override
    public void cancelLoad(IconTargetType type, Object target) {
        // nothing to cancel... we load everything in an synchronous way
    }

    @Override
    public Drawable getDrawableSynchronously(int forceSize) {
        Drawable d = getDrawable();
        d = scaleDrawable(d, forceSize);
        return d;
    }

    @Override
    public boolean isGroupIconDrawable() {
        return drawable != null && drawable instanceof FolderIcon;
    }

    private Drawable scaleDrawable(Drawable drawable, int forceSize) {
        if (drawable != null && forceSize != Config.NO_SCALE) {
            forceSize = Tool.dp2px(forceSize, Setup.appContext());
            drawable = new BitmapDrawable(Setup.appContext().getResources(), Bitmap.createScaledBitmap(Tool.drawableToBitmap(drawable), forceSize, forceSize, true));
        }
        return drawable;
    }

}
