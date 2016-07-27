/*
    Copyright 2013-2014 appPlant UG

    Licensed to the Apache Software Foundation (ASF) under one
    or more contributor license agreements.  See the NOTICE file
    distributed with this work for additional information
    regarding copyright ownership.  The ASF licenses this file
    to you under the Apache License, Version 2.0 (the
    "License"); you may not use this file except in compliance
    with the License.  You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on an
    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied.  See the License for the
    specific language governing permissions and limitations
    under the License.
 */

package cz.raynet.raynetcrm;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Base64;
import android.util.Log;
import android.widget.RemoteViews;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Puts the service in a foreground state, where the system considers it to be
 * something the user is actively aware of and thus not a candidate for killing
 * when low on memory.
 */
public class NotificationService extends Service {

    // Fixed ID for the 'foreground' notification
    private static final int NOTIFICATION_ID = -1;
    private static final int DEFAULT_TASK_INTERVAL = 30000;

    public static final String NOTIFICATION_OPTIONS_ID = "notificationOptions";
    public static final String NOTIFICATION_SERVICE_ID = "notificationService";
    public static final String PREF_KEY = "LocalNotification";
    public static final String SERV_PREF_KEY = "LocalNotificationService";
    public static final SimpleDateFormat SYSTEM_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm", new Locale("CS"));

    public static final String NOTIF_SERVICE_TITLE_KEY = "NOTIF_SERVICE_TITLE";
    public static final String NOTIF_SERVICE_TEXT_KEY = "NOTIF_SERVICE_TEXT";
    public static final String NOTIF_REMINDER_TITLE_KEY = "NOTIF_REMINDER_TITLE";
    public static final String NOTIF_REMINDER_TEXT_KEY = "NOTIF_REMINDER_TEXT";
    public static final String NOTIF_TASK_DEADLINE_TITLE_KEY = "NOTIF_TASK_DEADLINE_TITLE";

    public static final String RES_SERVICE_SMALL_ICON_KEY = "RES_SERVICE_SMALL_ICON";
    public static final String RES_NOTIF_BIG_ICON_KEY = "RES_NOTIF_BIG_ICON";
    public static final String RES_NOTIF_SMALL_ICON_KEY = "RES_NOTIF_SMALL_ICON";

    private static int sApiResponseErrorCount = 0;
    private static NotificationService sInstance = null;

    private String fCheckRequestUrl;
    private String fRemindersRequestUrl;
    private String fAuthUserName;
    private String fAuthPassword;
    private String fAuthInstanceName;
    private String fLocale;
    private String fDateTimeFormat;
    private JSONObject fResources;

    // Scheduler to exec periodic tasks
    private Timer fScheduler = new Timer();
    private final Manager fManager = new Manager(this);
    private final AssetUtil fAssets =  AssetUtil.getInstance(this);

    // Used to keep the app alive
    private TimerTask fKeepAliveTask;
    private RequestQueue fRequestQueue;

    private int fRemindersCount = 0;
    private boolean fLoadedAtLeastOnce = false;
    private NotificationObj fClickedNotification;
    private boolean fOpenNotificationList;

    /**
     * Static instance (singleton)
     */
    public static NotificationService getInstance() {
        return sInstance;
    }

    /**
     * Allow clients to call on to the service.
     */
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * Put the service in a foreground state to prevent app from being killed
     * by the OS.
     */
    @Override
    public void onCreate() {
        super.onCreate();
        sInstance = this;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        sApiResponseErrorCount = 0;

        try {
            final JSONObject params;
            if (intent != null && intent.getStringExtra("params") != null) {
                Log.e("NotificationService", intent.getStringExtra("params"));
                params = new JSONObject(intent.getStringExtra("params"));
                persist(getSevicePrefs(), NOTIFICATION_SERVICE_ID, params);
            } else {
                params = getPersisted(getSevicePrefs(), NOTIFICATION_SERVICE_ID);
                Log.e("NotificationService", params == null ? null : params.toString());
                fRemindersCount = fManager.getAll().size();

                for (NotificationObj notification : fManager.getAll()) {
                    if (notification.isScheduled()) {
                        notification.schedule();
                    }
                }
            }

            _updateParams(params);

        } catch (JSONException e) {
            Log.e("NotificationService", "onStartCommand", e);
        }

        fRequestQueue = Volley.newRequestQueue(this);

        keepAwake();

        return Service.START_STICKY;
    }

    public static boolean isRequestParamsSet(JSONObject params) throws JSONException {
        return (params != null) &&
                (params.getString("checkUrl") != null) &&
                (params.getString("remindersUrl") != null) &&
                (params.getString("userName") != null) &&
                (params.getString("password") != null) &&
                (params.getString("instanceName") != null) &&
                (params.getString("locale") != null) &&
                (params.getString("dateTimeFormat") != null) &&
                (params.getString("resources") != null);
    }

    public void _updateParams(JSONObject params) throws JSONException {
        if (params != null) {
            fCheckRequestUrl = params.getString("checkUrl");
            fRemindersRequestUrl = params.getString("remindersUrl");
            fAuthUserName = params.getString("userName");
            fAuthPassword = params.getString("password");
            fAuthInstanceName = params.getString("instanceName");
            fLocale = params.getString("locale");
            fDateTimeFormat = params.getString("dateTimeFormat");
            fResources = params.getJSONObject("resources");

            sApiResponseErrorCount = 0;
        }
    }

    public void updateParams(JSONObject params) {
        try {
            _updateParams(params);
        } catch (JSONException e) {
            Log.e("NotificationService", "updateParams", e);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        fManager.clearAll();
        persist(getSevicePrefs(), NOTIFICATION_SERVICE_ID, null);
        sleepWell();
        sInstance = null;
    }

    public boolean isRunning() {
        return fKeepAliveTask != null;
    }

    public NotificationObj getClickedNotification() {
        return fClickedNotification;
    }

    public void setClickedNotification(NotificationObj clickedNotification) {
        fClickedNotification = clickedNotification;
    }

    public void resetClickedNotification() {
        fClickedNotification = null;
    }

    public boolean isOpenNotificationList() {
        return fOpenNotificationList;
    }

    public void setOpenNotificationList(boolean openNotificationList) {
        fOpenNotificationList = openNotificationList;
    }

    public void resetOpenNotificationList() {
        fOpenNotificationList = false;
    }

    /**
     * Put the service in a foreground state to prevent app from being killed
     * by the OS.
     */
    public void keepAwake() {
        startForeground(NOTIFICATION_ID, makeNotification(fRemindersCount));

        if (fKeepAliveTask != null) {
            fKeepAliveTask.cancel();
            fScheduler.cancel();
            fScheduler = new Timer();
        }

        fKeepAliveTask = createTimerTask();
        fScheduler.schedule(fKeepAliveTask, 0, DEFAULT_TASK_INTERVAL);
    }

    private TimerTask createTimerTask() {
        final Handler handler = new Handler();

        return new TimerTask() {
            @Override
            public void run() {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        performCheck();
                    }
                });
            }
        };
    }

    /**
     * Stop background mode.
     */
    private void sleepWell() {
        sApiResponseErrorCount = 0;
        stopForeground(true);
        fKeepAliveTask.cancel();
        fKeepAliveTask = null;
        fScheduler.cancel();
    }

    private void performCheck() {
        if (fKeepAliveTask == null || fRequestQueue == null || sApiResponseErrorCount > 10) {
            return;
        }

        JsonObjectRequest jsObjRequest = new JsonObjectRequest(
                Request.Method.GET,
                fCheckRequestUrl,
                (String) null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        sApiResponseErrorCount = 0;
                        try {
                            if (!fLoadedAtLeastOnce || response.getBoolean("userHasNewReminder")) {
                                performTask();
                            } else {
                                recountScheduled();
                            }
                        } catch (JSONException ex) {
                            Log.e("NotificationService", "performCheck", ex);
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e("NotificationService", "performCheck", error);

                        if (error != null && error.networkResponse != null &&
                                (error.networkResponse.statusCode == 401 || error.networkResponse.statusCode == 404)) {
                            sApiResponseErrorCount++;
                        }
                    }
                }
        ) {
            @Override
            public Map<String, String> getHeaders() {
                return createRequestHeaders();
            }
        };

        fRequestQueue.add(jsObjRequest);
    }

    public void recountScheduled() {
        int count = 0;
        for (NotificationObj notification : fManager.getAll()) {
            if (notification.isScheduled()) {
                count++;
            }
        }

        if (count != fRemindersCount) {
            fRemindersCount = count;
            getNotMgr().notify(NOTIFICATION_ID, makeNotification(fRemindersCount));
        }
    }

    public void fetchNotificationsNow() {
        performCheck();
    }

    private void performTask() {
        if (fRequestQueue == null) {
            return;
        }

        JsonObjectRequest jsObjRequest = new JsonObjectRequest(
                Request.Method.GET,
                fRemindersRequestUrl,
                (String) null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        handleResponse(response);
                        recountScheduled();
                     }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e("NotificationService", "performTask", error);
                    }
                }
        ) {
            @Override
            public Map<String, String> getHeaders() {
                return createRequestHeaders();
            }
        };

        fRequestQueue.add(jsObjRequest);
    }

    private Map<String, String> createRequestHeaders() {
        final Map<String, String> params = new HashMap<String, String>();
        params.put(
                "Authorization",
                String.format("Basic %s", encodeBase64(fAuthUserName, fAuthPassword))
        );
        params.put("X-Instance-Name", fAuthInstanceName);

        return params;
    }

    private String encodeBase64(String userName, String password) {
        return Base64.encodeToString(String.format("%s:%s", userName, password).getBytes(), Base64.DEFAULT);
    }

    private void handleResponse(JSONObject response) {
        try {
            if (response.getBoolean("success")) {
                fLoadedAtLeastOnce = true;

                final JSONArray reminders = response.getJSONArray("data");
                final Set<String> ids = new HashSet<String>();

                if (reminders != null) {
                    for (int i = 0; i < reminders.length(); i++) {
                        final JSONObject options = reminders.getJSONObject(i);
                        addOptionsParams(options);
                        final NotificationObj potentialNotification = new NotificationObj(this, options);

                        NotificationObj existingNotification = fManager.get(createNotificationId(potentialNotification.getType(), potentialNotification.getId()));
                        if (existingNotification == null) {
                            if (!potentialNotification.wasInThePast()) {
                                fManager.schedule(options);
                            }
                        } else if ((!areDatesEquals(existingNotification.getTriggerTime(), potentialNotification.getTriggerTime()))
                                && (existingNotification.isScheduled() || potentialNotification.isScheduled())) {
                            existingNotification.cancel();
                            fManager.schedule(options);
                        }

                        ids.add(NotificationService.createNotificationId(potentialNotification.getType(), potentialNotification.getId()));
                    }
                }

                fManager.cancelAllButIds(ids);
            }
        } catch (JSONException e) {
            Log.e("NotificationService", "performTask", e);
        }
    }

    private boolean areDatesEquals(Date date1, Date date2) {
        return (date1 != null) ? date1.equals(date2) : (date2 == null);
    }

    private void addOptionsParams(JSONObject options) {
        try {
            options.put("locale", fLocale);
            options.put("dateTimeFormat", fDateTimeFormat);
            options.put("resources", fResources);
        } catch (JSONException e) {
            Log.e("NotificationService", "createOptionsParams", e);
        }
    }

    /**
     * Create a notification as the visible part to be able to put the service
     * in a foreground state.
     *
     * @return A local ongoing notification which pending intent is bound to the
     * main activity.
     */
    @SuppressLint("NewApi")
    @SuppressWarnings("deprecation")
    private Notification makeNotification(int remindersNumber) {
        final NotificationCompat.Builder notification = new NotificationCompat.Builder(this);
        final Intent intent = new Intent(this, ClickActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        final PendingIntent contentIntent = PendingIntent.getActivity(
                this, NOTIFICATION_ID, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        try {
            int resId = fAssets.getResIdForDrawable(fResources.getString(RES_SERVICE_SMALL_ICON_KEY));
            final String title = fResources.getString(NOTIF_SERVICE_TITLE_KEY);
            final String text = String.format(fResources.getString(NOTIF_SERVICE_TEXT_KEY), remindersNumber);

            if (resId == 0) {
                resId = android.R.drawable.screen_background_dark;
            }

            notification.setSmallIcon(resId).setContentTitle(title).setContentText(text).setOngoing(true);

            if (Build.VERSION.SDK_INT >= 21) {
                final RemoteViews remoteViews = new RemoteViews(getPackageName(), R.layout.notification_service);
                remoteViews.setImageViewResource(R.id.imagenotileft, resId);
                remoteViews.setTextViewText(R.id.title, title);
                remoteViews.setTextViewText(R.id.text, text);
                remoteViews.setOnClickPendingIntent(R.id.imageButton, contentIntent);

                notification.setContent(remoteViews);
            }

        } catch (JSONException ex) {
            Log.e("NotificationService", "makeNotification", ex);
        }

        notification.setContentIntent(contentIntent);

        return notification.build();
    }

    public static String createNotificationId(String reminderType, int reminderId) {
        return reminderType + "#" + reminderId;
    }

    public static String getReminderType(String notificationId) {
        return notificationId.substring(0, notificationId.indexOf("#"));
    }

    public static int getReminderId(String notificationId) {
        try {
            return Integer.parseInt(notificationId.substring(notificationId.indexOf("#") + 1));
        } catch (NumberFormatException ex) {
            Log.e("NotificationService", "getReminderId", ex);
        }

        return 0;
    }

    private NotificationManager getNotMgr() {
        return (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
    }

    private SharedPreferences getSevicePrefs() {
        return getSevicePrefs(this);
    }

    public static SharedPreferences getSevicePrefs(Context context) {
        return context.getSharedPreferences(NotificationService.SERV_PREF_KEY, Context.MODE_PRIVATE);
    }

    /**
     * Persist the information of this notification to the Android Shared
     * Preferences. This will allow the application to restore the notification
     * upon device reboot, app restart, retrieve notifications, aso.
     */
    public static void persist(SharedPreferences prefs, String key, JSONObject data) {
        final SharedPreferences.Editor editor = prefs.edit();

        editor.putString(key, (data == null) ? null : data.toString());

        if (Build.VERSION.SDK_INT < 9) {
            editor.commit();
        } else {
            editor.apply();
        }
    }

    public static JSONObject getPersisted(SharedPreferences prefs, String key) {
        try {
            final String result = prefs.getString(key, null);
            if (result != null) {
                return new JSONObject(result);
            }
        } catch (JSONException ex) {
            Log.e("NotificationService", "getPersisted", ex);
        }

        return null;
    }

    /**
     * Remove the notification from the Android shared Preferences.
     */
    public static void unpersist(SharedPreferences prefs, String key) {
        final SharedPreferences.Editor editor = prefs.edit();

        editor.remove(key);

        if (Build.VERSION.SDK_INT < 9) {
            editor.commit();
        } else {
            editor.apply();
        }
    }
}
