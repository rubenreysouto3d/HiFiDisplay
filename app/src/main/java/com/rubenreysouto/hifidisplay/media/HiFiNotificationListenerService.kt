package com.rubenreysouto.hifidisplay.media

import android.service.notification.NotificationListenerService

class HiFiNotificationListenerService : NotificationListenerService() {
    override fun onListenerConnected() {
        super.onListenerConnected()
        MediaSessionRepository.notifySessionEnvironmentChanged()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        MediaSessionRepository.notifySessionEnvironmentChanged()
    }

    override fun onNotificationPosted(sbn: android.service.notification.StatusBarNotification?) {
        MediaSessionRepository.notifySessionEnvironmentChanged()
    }

    override fun onNotificationRemoved(sbn: android.service.notification.StatusBarNotification?) {
        MediaSessionRepository.notifySessionEnvironmentChanged()
    }
}
