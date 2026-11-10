package app.songling

import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.songling.data.Settings
import app.songling.ui.SonglingRoot
import app.songling.ui.theme.SonglingTheme

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // A demo built for one language shows it, whatever the phone speaks.
        if (BuildConfig.DEMO && BuildConfig.DEMO_LANG.isNotEmpty() && androidx.appcompat.app.AppCompatDelegate.getApplicationLocales().toLanguageTags() != BuildConfig.DEMO_LANG)
            androidx.appcompat.app.AppCompatDelegate.setApplicationLocales(androidx.core.os.LocaleListCompat.forLanguageTags(BuildConfig.DEMO_LANG))
        val t = android.graphics.Color.TRANSPARENT
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.light(t, t), navigationBarStyle = SystemBarStyle.light(t, t))
        setContent {
            val settings: Settings? by container.settings.settings.collectAsStateWithLifecycle(initialValue = null)
            val system = androidx.compose.foundation.isSystemInDarkTheme()
            settings?.let { s ->
                val dark = s.theme == "dark" || (s.theme == "system" && system)
                app.songling.ui.theme.Mode.dark = dark
                androidx.compose.runtime.LaunchedEffect(dark) {
                    val st = if (dark) SystemBarStyle.dark(t) else SystemBarStyle.light(t, t)
                    enableEdgeToEdge(statusBarStyle = st, navigationBarStyle = st)
                }
                SonglingTheme { SonglingRoot(s) }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        container.kick()
        container.refreshOutside()
        app.songling.widget.DenWidget.refresh(this)
    }
}
