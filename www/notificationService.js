module.exports = {
    start: function (params, callbackContext) {
	callbackContext = callbackContext || {};
	cordova.exec(callbackContext.success || null, callbackContext.error || null, "NotificationService", "start", [params]);
    },
    updateParams: function (params, callbackContext) {
	callbackContext = callbackContext || {};
	cordova.exec(callbackContext.success || null, callbackContext.error || null, "NotificationService", "updateParams", [params]);
    },
    stop: function (callbackContext) {
        callbackContext = callbackContext || {};
        cordova.exec(callbackContext.success || null, callbackContext.error || null, "NotificationService", "stop", []);
    },
    isActive: function (callbackContext) {
	callbackContext = callbackContext || {};
        cordova.exec(callbackContext.success || null, callbackContext.error || null, "NotificationService", "isActive", []);
    },
    fetch: function (callbackContext) {
        callbackContext = callbackContext || {};
        cordova.exec(callbackContext.success || null, callbackContext.error || null, "NotificationService", "fetch", []);
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
