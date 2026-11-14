"""Screenshots for the README, the Stillcraft site and Google Play without a phone.

    ./gradlew :app:testDebugUnitTest -Prender -Pdemo --tests app.songling.RenderStore
    python tools/store_shots.py
    python tools/play.py

RenderStore draws the demo at 1080x2229, a 1080x2400 phone between its status bar and its gesture strip. Here each
screen gets bands for those two and lands in docs/raw/<lang>/<n>-<name>.png, as tools/capture_site.py takes them from a phone;
then its publish() draws a clean status bar and writes docs/screenshots and the site's assets.
"""
import os
import sys

from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from capture_site import ROOT, publish  # noqa: E402

BAR, STRIP = 111, 60


def main():
    for lang in ("en", "ru"):
        src = os.path.join(ROOT, "app", "build", "art", "store", lang)
        dst = os.path.join(ROOT, "docs", "raw", lang)
        os.makedirs(dst, exist_ok=True)
        for f in sorted(os.listdir(src)):
            im = Image.open(os.path.join(src, f)).convert("RGB")
            out = Image.new("RGB", (im.width, im.height + BAR + STRIP), im.getpixel((im.width // 2, 2)))
            out.paste(im.getpixel((2, im.height - 2)), (0, BAR + im.height, im.width, out.height))
            out.paste(im, (0, BAR))
            out.save(os.path.join(dst, f), optimize=True)
    publish()


if __name__ == "__main__":
    main()
