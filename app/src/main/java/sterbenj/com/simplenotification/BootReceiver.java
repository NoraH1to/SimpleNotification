package sterbenj.com.simplenotification;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * XJB Created by 野良人 on 2018/11/2.
 */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {

        if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED")){
            Intent intent1 = new Intent(context, MainActivity.class);
            intent1.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent1.putExtra("ACTION_BOOT", "ACTION_BOOT");
            context.startActivity(intent1);
        }
    }
}
