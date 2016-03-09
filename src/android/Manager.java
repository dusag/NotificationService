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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Central way to access all or single local notifications set by specific
 * state like triggered or scheduled. Offers shortcut ways to schedule,
 * cancel or clear local notifications.
 */
public class Manager {

    // Context passed through constructor and used for notification builder.
    private Context fContext;

    /**
     * Constructor
     *
     * @param context Application context
     */
    public Manager(Context context) {
        fContext = context;
    }


    /**
     * Schedule local notification specified by options object.
     *
     * @param options Set of notification options
     */
    public NotificationObj schedule(JSONObject options) {
        final NotificationObj notificationObj = new NotificationObj(fContext, options);

        notificationObj.schedule();

        return notificationObj;
    }

    /**
     * Clear local notification specified by ID.
     */
    public NotificationObj update(String id, JSONObject updates) {
        final NotificationObj notificationObj = get(id);

        if (notificationObj == null) {
            return null;
        }

        notificationObj.cancel();

        final JSONObject options = mergeJSONObjects(notificationObj.getOptions(), updates);

        return schedule(options);
    }

    public void cancelAllButIds(Set<String> idsToPreserve) {
        final Set<String> allIds = getIds();
        if (allIds != null) {
            for (String id : allIds) {
                if (!idsToPreserve.contains(id)) {
                    cancel(id);
                }
            }
        }
    }

    /**
     * Clear local notification specified by ID.
     *
     * @param id The notification ID
     */
    public NotificationObj clear(String id) {
        final NotificationObj notificationObj = get(id);

        if (notificationObj != null) {
            notificationObj.clear();
        }

        return notificationObj;
    }

    /**
     * Cancel local notification specified by ID.
     *
     * @param id The notification ID
     */
    public NotificationObj cancel(String id) {
        final NotificationObj notificationObj = get(id);

        if (notificationObj != null) {
            notificationObj.cancel();
        }

        return notificationObj;
    }

    /**
     * Clear all local notifications.
     */
    public void clearAll() {
        final List<NotificationObj> notificationObjs = getAll();

        for (NotificationObj notificationObj : notificationObjs) {
            notificationObj.clear();
        }

        getNotMgr().cancelAll();
    }

    /**
     * Cancel all local notifications.
     */
    public void cancelAll() {
        final List<NotificationObj> notificationObjs = getAll();

        for (NotificationObj notificationObj : notificationObjs) {
            notificationObj.cancel();
        }

        getNotMgr().cancelAll();
    }

    /**
     * All local notifications IDs.
     */
    public Set<String> getIds() {
        return getPrefs().getAll().keySet();
    }

    /**
     * List of local notifications with matching ID.
     *
     * @param ids Set of notification IDs
     */
    public List<NotificationObj> getByIds(Collection<String> ids) {
        final ArrayList<NotificationObj> notificationObjs = new ArrayList<NotificationObj>();

        for (String id : ids) {
            final NotificationObj notificationObj = get(id);

            if (notificationObj != null) {
                notificationObjs.add(notificationObj);
            }
        }

        return notificationObjs;
    }

    /**
     * List of all local notification.
     */
    public List<NotificationObj> getAll() {
        return getByIds(getIds());
    }


    /**
     * If a notification with an ID and type exists.
     *
     * @param id Notification ID
     */
    public boolean exist(String id) {
        final NotificationObj notificationObj = get(id);

        return notificationObj != null;
    }

    /**
     * Get existent local notification.
     *
     * @param notificationId Notification ID
     */
    public NotificationObj get(String notificationId) {
        final Map<String, ?> alarms = getPrefs().getAll();

        if (!alarms.containsKey(notificationId)) {
            return null;
        }

        try {
            final String json = alarms.get(notificationId).toString();
            final JSONObject options = new JSONObject(json);

            return new NotificationObj(fContext, options);
        } catch (JSONException e) {
            Log.e("Manager", "get", e);
        }

        return null;
    }

    /**
     * Merge two JSON objects.
     *
     * @param obj1 JSON object
     * @param obj2 JSON object with new options
     */
    private JSONObject mergeJSONObjects(JSONObject obj1, JSONObject obj2) {
        final Iterator it = obj2.keys();

        while (it.hasNext()) {
            try {
                final String key = (String) it.next();

                obj1.put(key, obj2.opt(key));
            } catch (JSONException e) {
                Log.e("Manager", "mergeJSONObjects", e);
            }
        }

        return obj1;
    }

    /**
     * Shared private preferences for the application.
     */
    private SharedPreferences getPrefs() {
        return fContext.getSharedPreferences(NotificationService.PREF_KEY, Context.MODE_PRIVATE);
    }

    /**
     * Notification manager for the application.
     */
    private NotificationManager getNotMgr() {
        return (NotificationManager) fContext.getSystemService(Context.NOTIFICATION_SERVICE);
    }
}
