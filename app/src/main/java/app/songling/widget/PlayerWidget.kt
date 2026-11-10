package app.songling.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import app.songling.MainActivity
import app.songling.R
import app.songling.container
import app.songling.data.Covers
import app.songling.ru
import kotlinx.coroutines.launch

/**
 * A player on the home screen, drawn as a record or a tape (as chosen in settings): what plays in any player,
 * with previous, play/pause and next. The buttons go to the player through its media session.
 */
class PlayerWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) = refresh(context)

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            PREV, TOGGLE, NEXT -> context.container.tracker.control(intent.action!!)
            else -> super.onReceive(context, intent)
        }
    }

    companion object {
        const val PREV = "app.songling.widget.PREV"
        const val TOGGLE = "app.songling.widget.TOGGLE"
        const val NEXT = "app.songling.widget.NEXT"

        private var last = ""
        private var picture: android.graphics.Bitmap? = null

        fun refresh(context: Context) {
            val ctx = context.applicationContext
            val manager = AppWidgetManager.getInstance(ctx)
            val ids = manager.getAppWidgetIds(ComponentName(ctx, PlayerWidget::class.java))
            if (ids.isEmpty()) return
            val tracker = ctx.container.tracker
            val np = tracker.now.value
            ctx.container.scope.launch {
                val s = ctx.container.settings.now()
                val ru = ctx.ru
                val tape = s.look == "TAPE"
                val views = RemoteViews(ctx.packageName, R.layout.widget_player)
                val art = tracker.art() ?: np?.cover?.let { Covers.bitmap(ctx, it) }
                // Redrawing the picture only when the song or the look changes keeps the updates light.
                val key = "${np?.title}|${np?.artist}|${art != null}|$tape"
                val accent = 0xFFC8743C.toInt()
                if (key != last || picture == null) picture = if (tape) PlayerArt.tape(art, accent) else PlayerArt.record(art, accent)
                last = key
                views.setImageViewBitmap(R.id.art, picture)
                if (np == null) {
                    views.setTextViewText(R.id.eyebrow, if (ru) "ТИШИНА" else "SILENCE")
                    views.setTextViewText(R.id.title, if (ru) "Ничего не играет" else "Nothing playing")
                    views.setTextViewText(R.id.artist, if (ru) "Включи музыку – питомец услышит" else "Play some music – your pet will hear")
                    views.setViewVisibility(R.id.progress, View.INVISIBLE)
                    views.setImageViewResource(R.id.toggle, R.drawable.wp_play)
                } else {
                    views.setTextViewText(R.id.eyebrow, if (np.playing) (if (ru) "СЕЙЧАС ИГРАЕТ" else "NOW PLAYING") else (if (ru) "НА ПАУЗЕ" else "PAUSED"))
                    views.setTextViewText(R.id.title, np.title)
                    views.setTextViewText(R.id.artist, np.artist + if (np.album.isNotEmpty()) " · ${np.album}" else "")
                    views.setViewVisibility(R.id.progress, if (np.length > 0) View.VISIBLE else View.INVISIBLE)
                    if (np.length > 0) views.setProgressBar(R.id.progress, 1000, (tracker.position() * 1000 / np.length).toInt().coerceIn(0, 1000), false)
                    views.setImageViewResource(R.id.toggle, if (np.playing) R.drawable.wp_pause else R.drawable.wp_play)
                }
                views.setOnClickPendingIntent(R.id.prev, action(ctx, PREV, 1))
                views.setOnClickPendingIntent(R.id.toggle, action(ctx, TOGGLE, 2))
                views.setOnClickPendingIntent(R.id.next, action(ctx, NEXT, 3))
                // The picture opens the player itself; the words open Songling.
                val player = np?.app?.let { ctx.packageManager.getLaunchIntentForPackage(it) }
                views.setOnClickPendingIntent(R.id.art, PendingIntent.getActivity(ctx, 4, player ?: Intent(ctx, MainActivity::class.java),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT))
                views.setOnClickPendingIntent(R.id.words, PendingIntent.getActivity(ctx, 5, Intent(ctx, MainActivity::class.java),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT))
                manager.updateAppWidget(ids, views)
            }
        }

        private fun action(ctx: Context, what: String, code: Int): PendingIntent =
            PendingIntent.getBroadcast(ctx, code, Intent(ctx, PlayerWidget::class.java).setAction(what), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }
}
