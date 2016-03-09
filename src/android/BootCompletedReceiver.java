package cz.raynet.raynetcrm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * User: dusag
 * Date: 10.2.16
 * Time: 10:55
 */
public class BootCompletedReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if ("android.intent.action.BOOT_COMPLETED".equals(intent.getAction())) {
            if (startIntent(context)) {
                final Intent pushIntent = new Intent(context, NotificationService.class);
                context.startService(pushIntent);
            }
        }
    }

    private boolean startIntent(Context context) {
        final JSONObject params = NotificationService.getPersisted(NotificationService.getSevicePrefs(context), NotificationService.NOTIFICATION_SERVICE_ID);

        try {
            return NotificationService.isRequestParamsSet(params);
        } catch (JSONException e) {
            Log.e("BootCompletedReceiver", "startIntent", e);
        }

        return false;
    }
}
