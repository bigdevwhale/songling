package app.songling.listen

import android.content.ComponentName
import android.content.Context
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
        // The system drops a listener after an update or when it kills the app, and doesn't always bind it again
        // by itself: the app then hears nothing although access is still on (#7). Ask to be bound again.
        rebind(this)
    }

    companion object {
        /** Asks the system to bind the listener again; harmless when it already is. */
        fun rebind(ctx: Context) {
            runCatching { requestRebind(ComponentName(ctx.applicationContext, Listener::class.java)) }
        }
    }
}
