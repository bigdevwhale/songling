package app.songling.listen

import android.content.ComponentName
import android.service.notification.NotificationListenerService
import app.songling.container

/**
 * Android only lets an app see other players' media sessions while it is a notification listener.
 * Songling reads no notifications: it only asks for the sessions and hands them to the [Tracker].
 */
class Listener : NotificationListenerService() {
    override fun onListenerConnected() {
        super.onListenerConnected()
        container.tracker.start(ComponentName(this, Listener::class.java))
    }

    override fun onListenerDisconnected() {
        container.tracker.stop()
        super.onListenerDisconnected()
    }
}
