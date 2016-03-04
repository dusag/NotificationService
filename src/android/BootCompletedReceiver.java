package cz.raynet.raynetcrm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * User: dusag
 * Date: 10.2.16
 * Time: 10:55
 */
public class BootCompletedReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if ("android.intent.action.BOOT_COMPLETED".equals(intent.getAction())) {
            Intent pushIntent = new Intent(context, NotificationService.class);
            context.startService(pushIntent);
        }
    }
}
