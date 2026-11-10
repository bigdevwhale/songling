#!/bin/bash
# Screenshots of the demo on a phone, in English and Russian: docs/raw/<lang>/*.png, docs/screenshots/<lang>/*.jpg.
#   ANDROID_SERIAL=<phone> bash tools/capture.sh
# Every tap checks that the demo is in front and no call is going on; it stops otherwise.
set -e
cd "$(dirname "$0")/.."
export MSYS_NO_PATHCONV=1
: "${JAVA_HOME:=/c/Program Files/Android/Android Studio/jbr}"; export JAVA_HOME
PKG=app.songling.demo

guard() {
  f=$(adb shell dumpsys window | grep mCurrentFocus)
  c=$(adb shell dumpsys telephony.registry | grep -m1 mCallState | tr -d ' \r')
  case "$f" in *$PKG*) ;; *) echo "ABORT focus $f"; exit 1;; esac
  [ "$c" = "mCallState=0" ] || { echo "ABORT call $c"; exit 1; }
}
tap() { guard; adb shell input tap "$1" "$2"; sleep "${3:-1.8}"; }
back() { guard; adb shell input keyevent 4; sleep 1.3; }
shot() { adb exec-out screencap -p > "docs/raw/$LANG_/$1.png"; }

for LANG_ in ${LANGS:-en ru}; do
  mkdir -p "docs/raw/$LANG_" "docs/screenshots/$LANG_"
  ./gradlew :app:assembleDebug -Pdemo -Pdemolang=$LANG_ -q
  adb install -r app/build/outputs/apk/debug/app-debug.apk >/dev/null
  adb shell pm clear $PKG >/dev/null
  adb shell am start -n $PKG/app.songling.MainActivity >/dev/null; sleep 35
  adb shell am force-stop $PKG; adb shell am start -n $PKG/app.songling.MainActivity >/dev/null; sleep 14
  shot 1-den
  tap 344 2170; shot 2-shelf
  tap 700 765; shot 3-album; back
  tap 732 2170; shot 4-saga
  tap 926 2170; shot 5-goals
  tap 144 2170; tap 733 1890; shot 6-bestiary
  tap 288 1350; shot 7-pet; back; back
  tap 823 150; shot 8-shop; back
done
python - <<'PY'
from PIL import Image
import glob, os
for p in glob.glob("docs/raw/*/*.png"):
    lang = p.replace("\\", "/").split("/")[-2]
    out = f"docs/screenshots/{lang}/" + os.path.basename(p).replace(".png", ".jpg")
    Image.open(p).convert("RGB").resize((540, 1170), Image.LANCZOS).save(out, quality=88)
print("ok")
PY
