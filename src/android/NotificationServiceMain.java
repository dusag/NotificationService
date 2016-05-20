package cz.raynet.raynetcrm;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaArgs;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONException;
import org.json.JSONObject;


public class NotificationServiceMain extends CordovaPlugin {

    private static final String ACTION_START = "start";
    private static final String ACTION_STOP = "stop";
    private static final String ACTION_FETCH = "fetch";
    private static final String ACTION_IS_ACTIVE = "isActive";
    private static final String ACTION_GET_CLICKED = "getClicked";
    private static final String ACTION_RESET_CLICKED = "resetClicked";

    @Override
    public boolean execute(String action, CordovaArgs args, CallbackContext callbackContext) throws JSONException {

        try {
            if (action.equals(ACTION_START)) {
                actionStart(args.getString(0), args.getString(1), args.getString(2), args.getString(3), args.getString(4),
                        args.getString(5), args.getString(6), args.getJSONObject(7), callbackContext);
                return true;
            } else if (action.equals(ACTION_STOP)) {
                actionStop(callbackContext);
                return true;
            } else if (action.equals(ACTION_FETCH)) {
                actionFetch(callbackContext);
                return true;
            } else if (action.equals(ACTION_IS_ACTIVE)) {
                actionIsActive(callbackContext);
                return true;
            } else if (action.equals(ACTION_GET_CLICKED)) {
                actionGetClicked(callbackContext);
                return true;
            } else if (action.equals(ACTION_RESET_CLICKED)) {
                actionResetClicked(callbackContext);
                return true;
            }
        } catch (JSONException e) {
            JSONObject errorObj = new JSONObject();
            errorObj.put("status", PluginResult.Status.JSON_EXCEPTION.ordinal());
            errorObj.put("message", e.getMessage());
            callbackContext.error(errorObj);
        }

        return false;
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void actionStart(String instanceName, String userName, String password, String locale, String dateTimeFormat,
                             String checkUrl, String remindersUrl, JSONObject resources, CallbackContext callbackContext) throws JSONException {
        final Activity activity = cordova.getActivity();
        final Intent intent = new Intent(activity, NotificationService.class);

        final JSONObject params = new JSONObject();
        params.put("instanceName", instanceName);
        params.put("userName", userName);
        params.put("password", password);
        params.put("locale", locale);
        params.put("dateTimeFormat", dateTimeFormat);
        params.put("checkUrl", checkUrl);
        params.put("remindersUrl", remindersUrl);
        params.put("resources", resources);

        intent.putExtra("params", params.toString());

        activity.startService(intent);
        callbackContext.success();
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void actionFetch(CallbackContext callbackContext) throws JSONException {
        final NotificationService service = NotificationService.getInstance();
        if (service != null) {
            service.fetchNotificationsNow();
        }

        callbackContext.success();
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void actionStop(CallbackContext callbackContext) throws JSONException {
        final Activity activity = cordova.getActivity();
        activity.stopService(new Intent(activity, NotificationService.class));
        callbackContext.success();
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void actionIsActive(CallbackContext callbackContext) throws JSONException {
        final JSONObject result = new JSONObject();
        final NotificationService service = NotificationService.getInstance();
        final boolean running = (service != null) && service.isRunning();
        result.put("active", running);
        callbackContext.success(result);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void actionGetClicked(CallbackContext callbackContext) throws JSONException {
        final NotificationService service = NotificationService.getInstance();
        JSONObject clicked = null;
        if (service != null) {
            if (service.getClickedNotification() != null) {
                clicked = new JSONObject(service.getClickedNotification().getOptions().toString());
                clicked.put("clicked", "notification");
            } else if (service.isOpenNotificationList()) {
                clicked = new JSONObject();
                clicked.put("clicked", "notificationList");
            } else {
                clicked = new JSONObject();
            }
        }
        callbackContext.success(clicked);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void actionResetClicked(CallbackContext callbackContext) throws JSONException {
        final NotificationService service = NotificationService.getInstance();
        if (service != null) {
            service.resetClickedNotification();
            service.resetOpenNotificationList();
        }
        callbackContext.success();
    }

    /**
     * Use this instead of deprecated sendJavascript
     *
     * @param js JS code snippet as string
     */
//    private static synchronized void sendJavascript(final String js) {
//        final Runnable jsLoader = new Runnable() {
//            public void run() {
//                sWebView.loadUrlIntoView("#!/agenda/meeting/detail/377", false);
//            }
//        };
//
//        try {
//            final Method post = sWebView.getClass().getMethod("post", Runnable.class);
//            post.invoke(sWebView, jsLoader);
//            sWebView.showWebPage("#!/agenda/meeting/detail/377", false, false, null);
//        } catch (Exception e) {
//            Log.e("NotificationServiceMain", "sendJavascript", e);
//            ((Activity) (sWebView.getContext())).runOnUiThread(jsLoader);
//        }
//    }

    /**
     * Launch main intent from package.
     */
    public static void launchApp(Context context) {
        final String pkgName = context.getPackageName();

        final Intent intent = context
                .getPackageManager()
                .getLaunchIntentForPackage(pkgName);

        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        context.startActivity(intent);
    }
}
