"""
Screenshots of the demo for the Stillcraft site, on a 1080x2400 phone (the Mi 13 Pro): docs/raw/<lang>/*.png,
then 540x1170 copies with a clean status bar into docs/screenshots/<lang>/*.jpg and, when the site is next to
this repo, ../stillcraft/assets/sg-<name>[.ru].png.

    ANDROID_SERIAL=a78f414d python tools/capture_site.py [en] [ru]

Every tap checks the demo is in front; it stops otherwise.
"""
import os
import subprocess
import sys
import time

from PIL import Image, ImageDraw, ImageFont

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SITE = os.path.join(os.path.dirname(ROOT), "stillcraft", "assets")
PKG = "app.songling.demo"
SERIAL = os.environ.get("ANDROID_SERIAL", "a78f414d")
FONT = os.path.join(ROOT, "app", "src", "main", "res", "font", "nunito_semibold.ttf")
JAVA = os.environ.get("JAVA_HOME", r"C:\Program Files\Android\Android Studio\jbr")


def adb(*a, out=False):
    r = subprocess.run(["adb", "-s", SERIAL, *a], capture_output=True)
    return r.stdout if out else None


def guard():
    f = adb("shell", "dumpsys", "window", out=True).decode(errors="ignore")
    focus = next((ln for ln in f.splitlines() if "mCurrentFocus" in ln), "")
    if PKG not in focus:
        sys.exit("ABORT focus " + focus.strip())


def tap(x, y, wait=1.8):
    guard()
    adb("shell", "input", "tap", str(x), str(y))
    time.sleep(wait)


def back():
    guard()
    adb("shell", "input", "keyevent", "4")
    time.sleep(1.3)


def shot(lang, name):
    path = os.path.join(ROOT, "docs", "raw", lang, name + ".png")
    with open(path, "wb") as f:
        f.write(adb("exec-out", "screencap", "-p", out=True))


# Where things are on a 1080x2400 screen.
DOCK = {"den": 150, "shelf": 345, "saga": 732, "goals": 930}
DOCK_Y = 2235
ACTIONS_Y = 1848
ACTIONS = {"shelve": 148, "log": 344, "concert": 536, "eggs": 732, "furnish": 928}


def run(lang):
    os.makedirs(os.path.join(ROOT, "docs", "raw", lang), exist_ok=True)
    env = dict(os.environ, JAVA_HOME=JAVA, ANDROID_SERIAL=SERIAL)
    subprocess.run([os.path.join(ROOT, "gradlew.bat" if os.name == "nt" else "gradlew"), ":app:installDebug", "-Pdemo", f"-Pdemolang={lang}", "-q"], cwd=ROOT, env=env, check=True)
    adb("shell", "pm", "clear", PKG)
    adb("shell", "am", "start", "-n", f"{PKG}/app.songling.MainActivity")
    time.sleep(35)
    adb("shell", "am", "force-stop", PKG)
    adb("shell", "am", "start", "-n", f"{PKG}/app.songling.MainActivity")
    time.sleep(14)
    tap(DOCK["den"], DOCK_Y, 3); shot(lang, "1-den")
    tap(DOCK["shelf"], DOCK_Y); shot(lang, "2-shelf")
    tap(540, 760, 2.5); shot(lang, "3-album"); back()
    tap(DOCK["saga"], DOCK_Y); shot(lang, "4-saga")
    tap(880, 295); shot(lang, "9-awards")
    tap(270, 295); tap(540, 450, 4); shot(lang, "10-cards"); back()
    tap(DOCK["goals"], DOCK_Y); shot(lang, "5-goals")
    tap(DOCK["den"], DOCK_Y); tap(ACTIONS["eggs"], ACTIONS_Y); shot(lang, "6-bestiary")
    tap(270, 700, 2.5); shot(lang, "7-pet"); back(); back()
    tap(ACTIONS["furnish"], ACTIONS_Y, 2.5); shot(lang, "8-shop"); back()


def clean(im):
    """The status bar drawn anew: the screen's own colour, the time and a full battery, nothing else."""
    im = im.convert("RGB")
    w = im.width
    bar = 112
    px = im.load()
    for x in range(w):
        c = px[x, bar + 4]
        for y in range(bar):
            px[x, y] = c
    bg = px[w // 2, bar + 4]
    ink = (30, 24, 20) if sum(bg) > 380 else (245, 238, 228)
    d = ImageDraw.Draw(im)
    d.text((64, 30), "11:04", font=ImageFont.truetype(FONT, 42), fill=ink)
    # A battery.
    x0, y0 = w - 130, 42
    d.rounded_rectangle([x0, y0, x0 + 62, y0 + 30], 8, outline=ink, width=3)
    d.rounded_rectangle([x0 + 6, y0 + 6, x0 + 56, y0 + 24], 4, fill=ink)
    d.rectangle([x0 + 64, y0 + 9, x0 + 69, y0 + 21], fill=ink)
    return im


def publish():
    for lang in ("en", "ru"):
        raw = os.path.join(ROOT, "docs", "raw", lang)
        if not os.path.isdir(raw):
            continue
        os.makedirs(os.path.join(ROOT, "docs", "screenshots", lang), exist_ok=True)
        for f in sorted(os.listdir(raw)):
            if not f.endswith(".png"):
                continue
            im = clean(Image.open(os.path.join(raw, f)))
            # 1080x2400 to the site's 540x1170: the bottom strip under the dock goes.
            im = im.crop((0, 0, 1080, 2340)).resize((540, 1170), Image.LANCZOS)
            name = f[:-4]
            im.save(os.path.join(ROOT, "docs", "screenshots", lang, name + ".jpg"), quality=88)
            if os.path.isdir(SITE):
                slug = name.split("-", 1)[1]
                im.save(os.path.join(SITE, f"sg-{slug}{'.ru' if lang == 'ru' else ''}.png"), optimize=True)
    print("ok")


if __name__ == "__main__":
    langs = [a for a in sys.argv[1:] if a in ("en", "ru")]
    for lang in langs:
        run(lang)
    if "--no-publish" not in sys.argv:
        publish()
