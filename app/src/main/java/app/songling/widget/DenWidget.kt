package app.songling.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.widget.RemoteViews
import app.songling.MainActivity
import app.songling.R
import app.songling.container
import app.songling.domain.Stage
import app.songling.domain.Words
import app.songling.ru
import app.songling.ui.art.Look
import app.songling.ui.art.PetArt
import app.songling.ui.title
import kotlinx.coroutines.launch

/** The pet on the home screen, with how long until it grows. */
class DenWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) = refresh(context)

    companion object {
        fun refresh(context: Context) {
            val ctx = context.applicationContext
            val manager = AppWidgetManager.getInstance(ctx)
            val ids = manager.getAppWidgetIds(ComponentName(ctx, DenWidget::class.java))
            if (ids.isEmpty()) return
            ctx.container.scope.launch {
                val st = ctx.container.studio()
                val ru = ctx.ru
                val p = st.pet
                val views = RemoteViews(ctx.packageName, R.layout.widget_den)
                if (p != null) {
                    val look = Look(p.species, p.stage, p.pet.shiny, p.form, sleeping = st.sleeping, windup = st.windup, hatch = if (p.egg) p.fed / Stage.BABY.from.toFloat() else 0f)
                    val b = PetArt.frame(look, 0)
                    val bmp = Bitmap.createBitmap(b.d, b.fw, b.fh, Bitmap.Config.ARGB_8888)
                    views.setImageViewBitmap(R.id.pet, Bitmap.createScaledBitmap(bmp, b.w * 4, b.h * 4, false))
                    views.setTextViewText(R.id.line, p.pet.title(ru) + " · " + Words.dur(st.byDay[st.today] ?: 0L, ru))
                } else views.setTextViewText(R.id.line, if (ru) "Выбери яйцо" else "Pick an egg")
                val open = PendingIntent.getActivity(ctx, 0, Intent(ctx, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
                views.setOnClickPendingIntent(R.id.root, open)
                manager.updateAppWidget(ids, views)
            }
        }
    }
}
