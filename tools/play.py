"""Builds the Google Play graphics in docs/google-play/graphics from the README screenshots.

python tools/play.py

Takes docs/raw/<lang>/<n>-<name>.png (1080x2400 screens of the demo, see tools/store_shots.py) and writes
the 512 px icon, the 1024x500 feature graphics and eight 1080x1920 captioned screenshots per language.
Play wants screenshots no longer than twice their width, so each screen runs off the bottom edge under
its caption; a sheet (the album) shows only its card, without the dimmed screen above it.
The icon is docs/logo.png (python tools/icon.py) on a full square.
"""
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter, ImageFont

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "docs/google-play/graphics"
RAW = ROOT / "docs/raw"
STATUS = 112  # the status bar on the 1080x2400 captures
FONTS = ROOT / "app/src/main/res/font"

# The den at night: its plum wallpaper, a warm lamp glow, the cream of the app's cards.
PLUM = (0x2A, 0x18, 0x24)
PLUM_LIGHT = (0x5A, 0x2E, 0x45)
CREAM = (0xFF, 0xF4, 0xE4)
GOLD = (0xF2, 0xBE, 0x5C)
RUST = (0xC8, 0x55, 0x3D)
WIRE = (0x14, 0x0C, 0x12)
BULBS = [(0xF2, 0x5C, 0x54), (0xF6, 0xC9, 0x4E), (0x6F, 0xC2, 0x76), (0x5B, 0x9B, 0xE6), (0xE8, 0x7A, 0xD0)]

# The order they appear in the store, whether it's a sheet, and the caption: a line in Comfortaa and one in gold.
PAGES = [
    ("1-den", False, ("A pet that hears", "the music you play"), ("Питомец, который", "слышит вашу музыку")),
    ("6-bestiary", False, ("25 pets hatch from", "the way you listen"), ("25 питомцев: каждого", "выводит ваша музыка")),
    ("2-shelf", False, ("Every album you've heard,", "with covers and stars"), ("Всё, что вы слушали,", "с обложками и звёздами")),
    ("3-album", True, ("Records or tapes,", "heard whole or not"), ("Пластинки или кассеты,", "целиком и по песням")),
    ("4-saga", False, ("The saga of your music:", "tops, hours and genres"), ("Сага вашей музыки:", "топы, часы и жанры")),
    ("9-awards", False, ("70 awards,", "goals and weekly tasks"), ("70 наград,", "цели и задания недели")),
    ("8-shop", False, ("Plectrums furnish", "the den"), ("Медиаторы обставляют", "логово")),
    ("10-cards", False, ("Postcards", "to share your music"), ("Открытки,", "чтобы поделиться музыкой")),
]


def comfortaa(size):
    return ImageFont.truetype(str(FONTS / "comfortaa.ttf"), size)


def nunito(size):
    return ImageFont.truetype(str(FONTS / "nunito_bold.ttf"), size)


def fit(make, text, size, width):
    while make(size).getlength(text) > width:
        size -= 2
    return make(size)


def rounded(im, r):
    mask = Image.new("L", im.size, 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, im.width - 1, im.height - 1), r, fill=255)
    out = im.convert("RGBA")
    out.putalpha(mask)
    return out


def lay(g, card, x, y, r=40, outline=PLUM_LIGHT):
    """A screen on the board: rounded corners, a soft shadow and a thin edge."""
    sh = Image.new("RGBA", (card.width + 120, card.height + 120), (0, 0, 0, 0))
    ImageDraw.Draw(sh).rounded_rectangle((60, 76, 60 + card.width, 76 + card.height), r, fill=(8, 3, 6, 170))
    g.alpha_composite(sh.filter(ImageFilter.GaussianBlur(24)), (x - 60, y - 60))
    g.alpha_composite(rounded(card, r), (x, y))
    ImageDraw.Draw(g).rounded_rectangle((x - 1, y - 1, x + card.width, y + card.height), r, outline=outline, width=3)


def board(W, H, cx, cy, rad):
    """The den's plum wall, warmer where the lamp light falls."""
    g = Image.new("RGB", (W, H), PLUM)
    glow = Image.new("L", (W, H), 0)
    ImageDraw.Draw(glow).ellipse((cx - rad, cy - rad, cx + rad, cy + rad), fill=255)
    glow = glow.filter(ImageFilter.GaussianBlur(rad // 2))
    g.paste(Image.new("RGB", (W, H), PLUM_LIGHT), (0, 0), glow)
    return g.convert("RGBA")


def lights(g, W, y, sag=14, step=46, r=7):
    """Fairy lights from the den: a sagging wire with coloured bulbs, each with a little halo."""
    halo = Image.new("RGBA", g.size, (0, 0, 0, 0))
    hd = ImageDraw.Draw(halo)
    d = ImageDraw.Draw(g)
    pts = []
    span = W / 3
    for x in range(0, W + 1, 4):
        t = (x % span) / span
        pts.append((x, y + sag * 4 * t * (1 - t)))
    d.line(pts, fill=WIRE, width=3)
    for i, x in enumerate(range(step // 2, W, step)):
        t = (x % span) / span
        by = y + sag * 4 * t * (1 - t) + 6
        c = BULBS[i % len(BULBS)]
        hd.ellipse((x - r * 3, by - r * 3, x + r * 3, by + r * 3), fill=c + (90,))
        d.rectangle((x - 2, by - 8, x + 2, by - 3), fill=WIRE)
        d.ellipse((x - r, by - r + 2, x + r, by + r + 2), fill=c)
    g.alpha_composite(halo.filter(ImageFilter.GaussianBlur(r * 1.5)))
    # The bulbs again over their own halo, so they stay crisp.
    for i, x in enumerate(range(step // 2, W, step)):
        t = (x % span) / span
        by = y + sag * 4 * t * (1 - t) + 6
        ImageDraw.Draw(g).ellipse((x - r, by - r + 2, x + r, by + r + 2), fill=BULBS[i % len(BULBS)])


def sheet_top(im):
    """The first row of a sheet's light card, a few pixels above it to keep its rounded edge."""
    grey = im.convert("L")
    x0, x1 = im.width // 4, im.width * 3 // 4
    for y in range(0, im.height):
        row = [grey.getpixel((x, y)) for x in range(x0, x1, 8)]
        if sum(row) / len(row) > 200:
            return max(0, y - 8)
    return 0


def screenshot(src, sheet, caption):
    W, H = 1080, 1920
    g = board(W, H, W // 2, 200, 640)
    lights(g, W, 18)
    d = ImageDraw.Draw(g)
    d.text((W / 2, 168), caption[0], font=fit(comfortaa, caption[0], 68, W - 120), fill=CREAM, anchor="mm")
    d.text((W / 2, 262), caption[1], font=fit(nunito, caption[1], 62, W - 120), fill=GOLD, anchor="mm")
    im = Image.open(src).convert("RGB")
    # The phone's status bar goes: its clock and other apps' chips are not Songling's.
    im = im.crop((0, STATUS, im.width, im.height))
    if sheet:
        im = im.crop((0, sheet_top(im), im.width, im.height))
    w = 860
    im = im.resize((w, round(im.height * w / im.width)), Image.LANCZOS)
    top = 350
    # Only the top corners show: the screen runs on past the bottom edge.
    lay(g, im.crop((0, 0, w, H - top + 60)), (W - w) // 2, top)
    return g.convert("RGB")


def feature(lang):
    W, H = 1024, 500
    g = board(W, H, 740, 260, 420)
    lights(g, W, 6, sag=10, step=40, r=5)
    d = ImageDraw.Draw(g)
    logo = Image.open(ROOT / "docs/logo.png").convert("RGBA").resize((128, 128), Image.LANCZOS)
    g.alpha_composite(rounded(logo, 30), (64, 82))
    lines = ("A pet that listens", "to music with you") if lang == "en" else ("Питомец, который", "слушает с вами музыку")
    # The den with the pet, cut from the first screen; the words keep to the left of it.
    im = Image.open(RAW / lang / "1-den.png").convert("RGB").crop((54, 280, 1026, 946))
    im = im.resize((round(im.width * 360 / im.height), 360), Image.LANCZOS)
    left = W - im.width - 48
    d.text((60, 236), "Songling", font=fit(comfortaa, "Songling", 76, left - 60 - 32), fill=CREAM)
    d.rectangle((66, 344, 280, 347), fill=RUST)
    f = min((fit(nunito, l, 38, left - 66 - 32) for l in lines), key=lambda x: x.size)
    for i, l in enumerate(lines):
        d.text((66, 364 + i * 48), l, font=f, fill=CREAM if i == 0 else GOLD)
    lay(g, im, left, 80, r=28, outline=GOLD)
    g.convert("RGB").save(OUT / f"feature-graphic-{lang}.png", optimize=True)


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    # Play rounds the icon itself: the logo goes on a full square of its own background, no transparent corners.
    logo = Image.open(ROOT / "docs/logo.png").convert("RGBA").resize((512, 512), Image.LANCZOS)
    square = Image.new("RGBA", (512, 512), logo.getpixel((256, 6))[:3] + (255,))
    square.alpha_composite(logo)
    square.convert("RGB").save(OUT / "icon-512.png", optimize=True)
    for lang in ("en", "ru"):
        feature(lang)
        folder = OUT / f"screenshots-{lang}"
        folder.mkdir(exist_ok=True)
        for i, (name, sheet, en, ru) in enumerate(PAGES, 1):
            screenshot(RAW / lang / f"{name}.png", sheet, en if lang == "en" else ru).save(folder / f"{i}.png", optimize=True)
    print("done:", OUT)


if __name__ == "__main__":
    main()
