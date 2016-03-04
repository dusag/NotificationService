module.exports = {
    stop: function (callbackContext) {
        callbackContext = callbackContext || {};
        cordova.exec(callbackContext.success || null, callbackContext.error || null, "NotificationService", "stop", []);
    },
    start: function (instanceName, userName, password, locale, dateTimeFormat, checkUrl, remindersUrl, resources, callbackContext) {
	callbackContext = callbackContext || {};
	cordova.exec(callbackContext.success || null, callbackContext.error || null, "NotificationService", "start", [instanceName, userName, password, locale, dateTimeFormat, checkUrl, remindersUrl, resources]);
    },
    isActive: function (callbackContext) {
	callbackContext = callbackContext || {};
        cordova.exec(callbackContext.success || null, callbackContext.error || null, "NotificationService", "isActive", []);
    },
    getClicked: function (callbackContext) {
	callbackContext = callbackContext || {};
        cordova.exec(callbackContext.success || null, callbackContext.error || null, "NotificationService", "getClicked", []);
    },
    resetClicked: function (callbackContext) {
	callbackContext = callbackContext || {};
        cordova.exec(callbackContext.success || null, callbackContext.error || null, "NotificationService", "resetClicked", []);
    }
};
