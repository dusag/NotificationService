package cz.raynet.raynetcrm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * User: dusag
 * Date: 9.2.16
 * Time: 16:27
 */
public class TriggerReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            final Bundle bundle  = intent.getExtras();
            final String data = bundle.getString(NotificationService.NOTIFICATION_OPTIONS_ID);
            final JSONObject options = new JSONObject(data);

            NotificationObj notificationObj = new NotificationObj(context, options);

            onTrigger(notificationObj);
        } catch (JSONException e) {
            Log.e("TriggerReceiver", "onReceive", e);
        }
    }

    /**
     * Called when a local notification was triggered.
     */
    public void onTrigger (NotificationObj notificationObj) {
        if (notificationObj != null && NotificationService.getInstance() != null) {
            notificationObj.show();
            NotificationService.getInstance().recountScheduled();
        }
    }
}
