package cz.raynet.raynetcrm;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * User: dusag
 * Date: 12.2.16
 * Time: 0:24
 */
public class ClickActivity extends Activity {

    /**
     * Called when local notification was clicked to launch the main intent.
     *
     * @param state Saved instance state
     */
    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);

        final Intent intent = getIntent();
        final Bundle bundle = intent.getExtras();

        try {
            final JSONObject data = (bundle == null) ? null : new JSONObject(bundle.getString(NotificationService.NOTIFICATION_OPTIONS_ID));


            final Context context = getApplicationContext();
            if (data != null) {
                NotificationService.getInstance().setClickedNotification(new NotificationObj(context, data));
            } else {
                NotificationService.getInstance().setOpenNotificationList(true);
            }
            NotificationServiceMain.launchApp(context);

        } catch (JSONException e) {
            Log.e("ClickActivity", "onCreate", e);
        }
    }
}
