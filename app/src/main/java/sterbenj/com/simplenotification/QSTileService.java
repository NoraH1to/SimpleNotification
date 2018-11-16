package sterbenj.com.simplenotification;

import android.annotation.TargetApi;
import android.content.Intent;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

import java.lang.reflect.Method;

/**
 * XJB Created by 野良人 on 2018/11/2.
 */
@TargetApi(24)
public class QSTileService extends TileService {
    @Override
    public void onTileAdded() {
        getQsTile().setState(Tile.STATE_ACTIVE);
        super.onTileAdded();
    }

    @Override
    public void onTileRemoved() {
        getQsTile().setState(Tile.STATE_INACTIVE);
        super.onTileRemoved();
    }

    @Override
    public void onStartListening() {
        super.onStartListening();
    }

    @Override
    public void onStopListening() {
        super.onStopListening();
    }

    @Override
    public void onClick() {
        super.onClick();
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (!isLocked()){
            startActivity(intent);
            collapseStatusBar();
        }
    }

    //收起状态栏
    public  void collapseStatusBar() {
        Object service = getSystemService("statusbar");
        if (null == service)
            return;
        try {
            Class<?> clazz = Class.forName("android.app.StatusBarManager");
            int sdkVersion = android.os.Build.VERSION.SDK_INT;
            Method collapse = null;
            if (sdkVersion <= 16) {
                collapse = clazz.getMethod("collapse");
            } else {
                collapse = clazz.getMethod("collapsePanels");
            }
            collapse.setAccessible(true);
            collapse.invoke(service);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
