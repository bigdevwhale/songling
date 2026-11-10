"""
The launcher icon and docs/logo.png: a record on terracotta, a pixel note on its oatmeal label.

    python tools/icon.py

Writes the adaptive icon layers (mipmap-*/ic_launcher_foreground.png and _monochrome.png, a solid
terracotta background drawable), legacy square icons and docs/logo.png (512, rounded).
"""
import os

from PIL import Image, ImageDraw

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(ROOT, "app", "src", "main", "res")
OAT = (243, 234, 219)
TERRA = (200, 85, 61)
VINYL = (38, 33, 32)
GROOVE = (60, 53, 51)
DENS = {"mdpi": 1, "hdpi": 1.5, "xhdpi": 2, "xxhdpi": 3, "xxxhdpi": 4}
NOTE = ["...XXX..", "...X.XX.", "...X..X.", "...X....", "...X....", ".XXX....", "XXXX....", ".XX....."]


def record(size, r_share, bg=None):
    """A record of radius r_share·size in a size×size picture, drawn 4× bigger and scaled down."""
    big = size * 4
    im = Image.new("RGBA", (big, big), (bg + (255,)) if bg else (0, 0, 0, 0))
    d = ImageDraw.Draw(im, "RGBA")
    c = big / 2
    r = big * r_share
    d.ellipse([c - r, c - r, c + r, c + r], fill=VINYL)
    for k in range(1, 7):
        rr = r * (0.48 + k * 0.075)
        d.ellipse([c - rr, c - rr, c + rr, c + rr], outline=GROOVE, width=max(2, big // 128))
    d.arc([c - r * .84, c - r * .84, c + r * .84, c + r * .84], 205, 240, fill=(255, 255, 255, 46), width=int(r * .1))
    lr = r * 0.36
    d.ellipse([c - lr, c - lr, c + lr, c + lr], fill=OAT)
    # The note, on a pixel grid, sitting on the label.
    p = lr * 1.1 / 8
    for j, row in enumerate(NOTE):
        for i, ch in enumerate(row):
            if ch == "X":
                x0 = c - 4 * p + i * p + p * 0.3
                y0 = c - 4 * p + j * p
                d.rectangle([x0, y0, x0 + p, y0 + p], fill=TERRA)
    return im.resize((size, size), Image.LANCZOS)


def rounded(im, r):
    m = Image.new("L", im.size, 0)
    ImageDraw.Draw(m).rounded_rectangle([0, 0, im.width - 1, im.height - 1], radius=r, fill=255)
    im.putalpha(m)
    return im


def main():
    # Adaptive foreground: 108 dp; launchers keep the middle 66 dp, so the record stays inside it.
    for name, f in DENS.items():
        size = int(108 * f)
        fg = record(size, 0.3)
        d = os.path.join(RES, f"mipmap-{name}")
        os.makedirs(d, exist_ok=True)
        fg.save(os.path.join(d, "ic_launcher_foreground.png"))
        mono = Image.new("RGBA", fg.size, (0, 0, 0, 0))
        mono.putalpha(fg.getchannel("A"))
        mono.save(os.path.join(d, "ic_launcher_monochrome.png"))
        legacy = rounded(record(int(48 * f), 0.4, TERRA), int(48 * f) // 5)
        legacy.save(os.path.join(d, "ic_launcher.png"))
    with open(os.path.join(RES, "drawable", "ic_launcher_background.xml"), "w", encoding="utf-8", newline="\n") as fh:
        fh.write('<?xml version="1.0" encoding="utf-8"?>\n<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="108dp" android:height="108dp" android:viewportWidth="108" android:viewportHeight="108">\n    <path android:fillColor="#C8553D" android:pathData="M0,0h108v108h-108z" />\n</vector>\n')
    fgx = os.path.join(RES, "drawable", "ic_launcher_foreground.xml")
    if os.path.exists(fgx):
        os.remove(fgx)
    with open(os.path.join(RES, "mipmap-anydpi-v26", "ic_launcher.xml"), "w", encoding="utf-8", newline="\n") as fh:
        fh.write('<?xml version="1.0" encoding="utf-8"?>\n<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">\n    <background android:drawable="@drawable/ic_launcher_background" />\n    <foreground android:drawable="@mipmap/ic_launcher_foreground" />\n    <monochrome android:drawable="@mipmap/ic_launcher_monochrome" />\n</adaptive-icon>\n')
    os.makedirs(os.path.join(ROOT, "docs"), exist_ok=True)
    rounded(record(512, 0.4, TERRA), 112).save(os.path.join(ROOT, "docs", "logo.png"))
    print("ok")


if __name__ == "__main__":
    main()
