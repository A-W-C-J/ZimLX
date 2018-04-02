package org.zimmob.zimlx.core.model;

import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.mikepenz.fastadapter.items.AbstractItem;

import org.zimmob.zimlx.core.R;
import org.zimmob.zimlx.core.interfaces.AbstractApp;
import org.zimmob.zimlx.core.interfaces.FastItem;
import org.zimmob.zimlx.core.manager.Setup;
import org.zimmob.zimlx.core.util.DragAction;
import org.zimmob.zimlx.core.widget.AppDrawerVertical;
import org.zimmob.zimlx.core.widget.AppItemView;
import org.zimmob.zimlx.core.widget.Desktop;

import java.util.List;

public class DrawerAppItem extends AbstractItem<DrawerAppItem, DrawerAppItem.ViewHolder> implements FastItem.AppItem<DrawerAppItem, DrawerAppItem.ViewHolder> {
    private AbstractApp app;
    private AppItemView.LongPressCallBack onLongClickCallback;

    public DrawerAppItem(AbstractApp app) {
        this.app = app;
        onLongClickCallback = new AppItemView.LongPressCallBack() {
            @Override
            public boolean readyForDrag(View view) {
                return Setup.Companion.appSettings().getDesktopStyle() != Desktop.DesktopMode.INSTANCE.getSHOW_ALL_APPS();
            }

            @Override
            public void afterDrag(View view) {
                //This will be handled by the Drag N Drop listener in the CoreHome
                //CoreHome.Companion.getLauncher().closeAppDrawer();
            }
        };
    }

    @Override
    public int getType() {
        return R.id.id_adapter_drawer_app_item;
    }

    @Override
    public int getLayoutRes() {
        return R.layout.item_app;
    }

    @Override
    public ViewHolder getViewHolder(View v) {
        return new ViewHolder(v);
    }

    @Override
    public AbstractApp getApp() {
        return app;
    }

    @Override
    public void bindView(DrawerAppItem.ViewHolder holder, List payloads) {
        holder.builder
                .setAppItem(app)
                .withOnLongClick(app, DragAction.Action.APP_DRAWER, onLongClickCallback)
                .withOnTouchGetPosition(null, null);
        holder.appItemView.load();
        super.bindView(holder, payloads);
    }

    @Override
    public void unbindView(DrawerAppItem.ViewHolder holder) {
        super.unbindView(holder);
        holder.appItemView.reset();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        AppItemView appItemView;
        AppItemView.Builder builder;

        ViewHolder(View itemView) {
            super(itemView);
            appItemView = (AppItemView) itemView;
            appItemView.setTargetedWidth(AppDrawerVertical.itemWidth);
            appItemView.setTargetedHeightPadding(AppDrawerVertical.itemHeightPadding);

            builder = new AppItemView.Builder(appItemView, Setup.Companion.appSettings().getDrawerIconSize())
                    .withOnTouchGetPosition(null, null)
                    .setLabelVisibility(Setup.Companion.appSettings().isDrawerShowLabel())
                    .setTextColor(Setup.Companion.appSettings().getDrawerLabelColor())
                    .setFontSize(appItemView.getContext(), Setup.Companion.appSettings().getDrawerLabelFontSize())
                    .setFastAdapterItem();
        }
    }
}
