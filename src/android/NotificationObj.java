/*
 * Copyright (c) 2013-2015 by appPlant UG. All rights reserved.
 *
 * @APPPLANT_LICENSE_HEADER_START@
 *
 * This file contains Original Code and/or Modifications of Original Code
 * as defined in and that are subject to the Apache License
 * Version 2.0 (the 'License'). You may not use this file except in
 * compliance with the License. Please obtain a copy of the License at
 * http://opensource.org/licenses/Apache-2.0/ and read it before using this
 * file.
 *
 * The Original Code and all software distributed under the License are
 * distributed on an 'AS IS' basis, WITHOUT WARRANTY OF ANY KIND, EITHER
 * EXPRESS OR IMPLIED, AND APPLE HEREBY DISCLAIMS ALL SUCH WARRANTIES,
 * INCLUDING WITHOUT LIMITATION, ANY WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE, QUIET ENJOYMENT OR NON-INFRINGEMENT.
 * Please see the License for the specific language governing rights and
 * limitations under the License.
 *
 * @APPPLANT_LICENSE_HEADER_END@
 */

package cz.raynet.raynetcrm;


import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Random;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.util.Log;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Wrapper class around OS notification class. Handles basic operations
 * like show, delete, cancel for a single local notification instance.
 */
public class NotificationObj {

    // Default receiver to handle the trigger event
    private static Class<?> sReceiver = TriggerReceiver.class;

    // Application context passed by constructor
    private final Context fContext;

    // Options
    private final JSONObject fOptions;

    // Assets
    private final AssetUtil fAssets;


    /**
     * Constructor
     *
     * @param context Application context
     * @param options Parsed notification options
     */
    protected NotificationObj(Context context, JSONObject options) {

        fContext = context;
        fOptions = options;
        fAssets = AssetUtil.getInstance(context);
    }

    /**
     * Get application context.
     */
    public Context getContext() {
        return fContext;
    }

    /**
     * Get options.
     */
    public JSONObject getOptions() {
        return fOptions;
    }

    /**
     * Get notification ID.
     */
    public int getId() {
        try {
            return fOptions.getInt("entityId");
        } catch (JSONException e) {
            Log.e("Notification", "getId", e);
        }

        return 0;
    }

    public String getEntityName() {
        try {
            return fOptions.getString("entityName");
        } catch (JSONException e) {
            Log.e("Notification", "getEntityName", e);
        }

        return null;
    }

    public String getType() {
        try {
            return fOptions.getString("remindType");
        } catch (JSONException e) {
            Log.e("Notification", "getTag", e);
        }

        return null;
    }

    private Date systemParseDate(String str) {
        return parseDate(str, NotificationService.SYSTEM_DATE_FORMAT);
    }

    private Date parseDate(String str, DateFormat format) {
        if (str == null) {
            return null;
        }

        try {
            return format.parse(str);
        } catch (ParseException e) {
            Log.e("Notification", "formatDate", e);
        }

        return null;
    }

    private String formatDate(Date date, DateFormat format) {
        if (date == null) {
            return null;
        }

        return format.format(date);
    }

    public Date getTriggerTime() {
        try {
            return systemParseDate(fOptions.getString("remindAt"));
        } catch (JSONException e) {
            Log.e("Notification", "getTriggerDate", e);
        }

        return null;
    }

    public Date getDateTimeFrom() {
        try {
            return systemParseDate(fOptions.getString("scheduledFrom"));
        } catch (JSONException e) {
            Log.e("Notification", "getTriggerDate", e);
        }

        return null;
    }

    public Date getDateTimeTo() {
        try {
            return systemParseDate(fOptions.getString("scheduledTill"));
        } catch (JSONException e) {
            Log.e("Notification", "getTriggerDate", e);
        }

        return null;
    }

    public String getTitle() {
        try {
            return fOptions.getString("title");
        } catch (JSONException e) {
            Log.e("Notification", "getTitle", e);
        }

        return null;
    }

    public String getLocale() {
        try {
            return fOptions.getString("locale");
        } catch (JSONException e) {
            Log.e("Notification", "getLocale", e);
        }

        return null;
    }

    public String getDateTimeFormat() {
        try {
            return fOptions.getString("dateTimeFormat");
        } catch (JSONException e) {
            Log.e("Notification", "getDateTimeFormat", e);
        }

        return null;
    }

    public String getNotifReminderText() {
        try {
            final JSONObject obj = fOptions.getJSONObject("resources");
            if (obj != null) {
                return obj.getString(NotificationService.NOTIF_REMINDER_TEXT_KEY);
            }
        } catch (JSONException e) {
            Log.e("Notification", "getNotifReminderText1", e);
        }

        return null;
    }

    public String getNotifReminderTitle() {
        try {
            final JSONObject obj = fOptions.getJSONObject("resources");
            if (obj != null) {
                return obj.getString(NotificationService.NOTIF_REMINDER_TITLE_KEY);
            }
        } catch (JSONException e) {
            Log.e("Notification", "getNotifReminderTitle", e);
        }

        return null;
    }

    public String getNotifTaskDeadlineTitle() {
        try {
            final JSONObject obj = fOptions.getJSONObject("resources");
            if (obj != null) {
                return obj.getString(NotificationService.NOTIF_TASK_DEADLINE_TITLE_KEY);
            }
        } catch (JSONException e) {
            Log.e("Notification", "getNotifTaskDeadlineTitle", e);
        }

        return null;
    }

    public Bitmap getNotifBigIcon() {
        try {
            final JSONObject obj = fOptions.getJSONObject("resources");
            if (obj != null) {
                int resId = fAssets.getResIdForMipmap(obj.getString(NotificationService.RES_NOTIF_BIG_ICON_KEY));
                if (resId == 0) {
                    resId = android.R.drawable.screen_background_dark;
                }

                return BitmapFactory.decodeResource(fContext.getResources(),  resId);
            }
        } catch (Exception e) {
            Log.e("Notification", "getNotifBigIcon", e);
        }

        return null;
    }

    public int getNotifSmallIcon() {
        try {
            final JSONObject obj = fOptions.getJSONObject("resources");
            if (obj != null) {
                int resId = fAssets.getResIdForDrawable(obj.getString(NotificationService.RES_NOTIF_SMALL_ICON_KEY));
                if (resId == 0) {
                    resId = android.R.drawable.screen_background_dark;
                }

                return resId;
            }
        } catch (Exception e) {
            Log.e("Notification", "getNotifSmallIcon", e);
        }

        return android.R.drawable.screen_background_dark;
    }


    /**
     * If the notification was in the past.
     */
    public boolean wasInThePast() {
        final Date triggerDate = getTriggerTime();

        return (triggerDate != null) && new Date().after(triggerDate);
    }

    /**
     * If the notification is scheduled.
     */
    public boolean isScheduled() {
        return !wasInThePast();
    }

    /**
     * If the notification is triggered.
     */
    public boolean isTriggered() {
        return wasInThePast();
    }


    /**
     * Schedule the local notification.
     */
    public void schedule() {
        if (wasInThePast()) {
            return;
        }

        final Date triggerTime = getTriggerTime();

        NotificationService.persist(getPrefs(), createNotifPersistId(), fOptions);

        // Intent gets called when the Notification gets fired
        final Intent intent = new Intent(fContext, sReceiver)
                .setAction(createNotifPersistId())
                .putExtra(NotificationService.NOTIFICATION_OPTIONS_ID, fOptions.toString());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
        }

        final PendingIntent pi = PendingIntent.getBroadcast(fContext, 1, intent, PendingIntent.FLAG_CANCEL_CURRENT);

        getAlarmMgr().set(AlarmManager.RTC_WAKEUP, triggerTime.getTime(), pi);
    }

    /**
     * Clear the local notification without canceling repeating alarms.
     */
    public void clear() {
        if (wasInThePast()) {
            NotificationService.unpersist(getPrefs(), createNotifPersistId());
        } else {
            getNotMgr().cancel(getType(), getId());
        }
    }

    /**
     * Cancel the local notification.
     * <p/>
     * Create an intent that looks similar, to the one that was registered
     * using schedule. Making sure the notification id in the action is the
     * same. Now we can search for such an intent using the 'getService'
     * method and cancel it.
     */
    public void cancel() {
        final Intent intent = new Intent(fContext, sReceiver).setAction(createNotifPersistId());

        final PendingIntent pi = PendingIntent.getBroadcast(fContext, 1, intent, 0);

        getAlarmMgr().cancel(pi);
        getNotMgr().cancel(getType(), getId());

        NotificationService.unpersist(getPrefs(), createNotifPersistId());
    }

    /**
     * Present the local notification to user.
     */
    public void show() {
        // TODO Show dialog when in foreground
        showNotification();
    }

    /**
     * Show as local notification when in background.
     */
    @SuppressWarnings("deprecation")
    private void showNotification() {
        getNotMgr().notify(getType(), getId(), buildNotificationObj());
    }

    /**
     * Show as modal dialog when in foreground.
     */
    private void showDialog() {
        // TODO
    }

    private String nullToArg(String str, String arg) {
        return (str != null && str.length() > 0) ? str : arg;
    }

    private Notification buildNotificationObj() {
        final SimpleDateFormat dateFormat = new SimpleDateFormat(getDateTimeFormat(), new Locale("CS"));

        final String dateFrom = formatDate(getDateTimeFrom(), dateFormat);
        final String dateTill = formatDate(getDateTimeTo(), dateFormat);
        final String title = nullToArg("TASK_DEADLINE".equals(getType()) ? getNotifTaskDeadlineTitle() : getNotifReminderTitle(), " - ");
        final String notifReminderText1 = nullToArg(getTitle(), " - ");
        final String notifReminderText2 = nullToArg(getNotifReminderText(), "%s - %s");
        final String textFormat = (Build.VERSION.SDK_INT >= 21) ? "%s\n%s" : "%s. %s.";
        final String text = String.format(textFormat, notifReminderText1, String.format(notifReminderText2, dateFrom, (dateTill == null ? "-" : dateTill)));

        final Notification.Builder builder = new Notification.Builder(fContext)
                .setDefaults(Notification.DEFAULT_VIBRATE)
                .setContentTitle(title)
                .setContentText(text)
                .setNumber(0)
                .setTicker(title)
                .setSmallIcon(getNotifSmallIcon())
                .setLargeIcon(getNotifBigIcon())
                .setAutoCancel(false)
                .setOngoing(false);

        if (Build.VERSION.SDK_INT >= 21) {
            builder.setColor(0xff69a300);
        }

        final Intent intent = new Intent(fContext, ClickActivity.class).setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        intent.putExtra(NotificationService.NOTIFICATION_OPTIONS_ID, fOptions.toString());

        int requestCode = new Random().nextInt();

        final PendingIntent contentIntent = PendingIntent.getActivity(
                fContext, requestCode, intent, PendingIntent.FLAG_CANCEL_CURRENT);

        builder.setContentIntent(contentIntent);

        if (Build.VERSION.SDK_INT >= 16) {
            return builder.build();
        } else {
            return builder.getNotification();
        }
    }

    private String createNotifPersistId() {
        return NotificationService.createNotificationId(getType(), getId());
    }

    private SharedPreferences getPrefs() {
        return fContext.getSharedPreferences(NotificationService.PREF_KEY, Context.MODE_PRIVATE);
    }

    /**
     * Notification manager for the application.
     */
    private NotificationManager getNotMgr() {
        return (NotificationManager) fContext.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    /**
     * Alarm manager for the application.
     */
    private AlarmManager getAlarmMgr() {
        return (AlarmManager) fContext.getSystemService(Context.ALARM_SERVICE);
    }
}
