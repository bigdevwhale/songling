"""Writes the small static Android resources (themes, colours, strings, icon, widget layout)."""
import os

ROOT = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "app", "src", "main", "res")

FILES = {
    "values/themes.xml": """<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="Theme.Songling" parent="Theme.AppCompat.NoActionBar">
        <item name="android:windowBackground">@color/window</item>
        <item name="android:statusBarColor">@android:color/transparent</item>
        <item name="android:navigationBarColor">@android:color/transparent</item>
        <item name="colorPrimary">@color/amber</item>
        <item name="colorAccent">@color/amber</item>
    </style>
</resources>
""",
    "values/colors.xml": """<?xml version="1.0" encoding="utf-8"?>
<resources>
    <color name="window">#FFF3EADB</color>
    <color name="amber">#FFC8553D</color>
</resources>
""",
    "values/strings.xml": """<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">Songling</string>
    <string name="listener_name">Songling: what is playing</string>
    <string name="widget_name">Songling den</string>
</resources>
""",
    "values-ru/strings.xml": """<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">Songling</string>
    <string name="listener_name">Songling: что играет</string>
    <string name="widget_name">Логово Songling</string>
</resources>
""",
    "resources.properties": "unqualifiedResLocale=en\n",
    "mipmap-anydpi-v26/ic_launcher.xml": """<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@drawable/ic_launcher_background" />
    <foreground android:drawable="@drawable/ic_launcher_foreground" />
    <monochrome android:drawable="@drawable/ic_launcher_foreground" />
</adaptive-icon>
""",
    "drawable/ic_launcher_background.xml": """<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="108dp" android:height="108dp" android:viewportWidth="108" android:viewportHeight="108">
    <path android:fillColor="#1C1729" android:pathData="M0,0h108v108h-108z" />
</vector>
""",
    "drawable/ic_launcher_foreground.xml": """<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="108dp" android:height="108dp" android:viewportWidth="108" android:viewportHeight="108">
    <path android:fillColor="#F5B841" android:pathData="M46,34 L70,28 L70,64 A8,7 0 1,1 64,57.5 L64,40 L52,43 L52,70 A8,7 0 1,1 46,63.5 Z" />
</vector>
""",
    "drawable/widget_bg.xml": """<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle">
    <solid android:color="#E61C1729" />
    <corners android:radius="22dp" />
</shape>
""",
    "xml/shared_files.xml": """<?xml version="1.0" encoding="utf-8"?>
<paths>
    <cache-path name="share" path="share/" />
</paths>
""",
    "xml/widget_den.xml": """<?xml version="1.0" encoding="utf-8"?>
<appwidget-provider xmlns:android="http://schemas.android.com/apk/res/android"
    android:minWidth="110dp"
    android:minHeight="110dp"
    android:targetCellWidth="2"
    android:targetCellHeight="2"
    android:resizeMode="horizontal|vertical"
    android:updatePeriodMillis="1800000"
    android:initialLayout="@layout/widget_den"
    android:widgetCategory="home_screen" />
""",
    "layout/widget_den.xml": """<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@+id/root"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@drawable/widget_bg"
    android:gravity="center"
    android:orientation="vertical"
    android:padding="8dp">
    <ImageView
        android:id="@+id/pet"
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="1"
        android:contentDescription="@string/app_name"
        android:scaleType="fitCenter" />
    <TextView
        android:id="@+id/line"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:gravity="center"
        android:maxLines="2"
        android:textColor="#F2ECFA"
        android:textSize="12sp" />
</LinearLayout>
""",
}

for name, text in FILES.items():
    path = os.path.join(ROOT, name)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        f.write(text)
print("ok", len(FILES))
