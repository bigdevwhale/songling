"""
Cuts the drawn den sheets (art/den/sheets, made with ChatGPT image generation through Codex; prompts in
art/den/prompts) into the den's pixels: a 200x132 room per wallpaper and one sprite per thing, placed where
the den shows it.

    python tools/den.py

Pictures are FINE times finer than the den's pixels (the app's Buf.FINE); den.txt stays in den pixels.

Writes app/src/main/resources/den/wall_<name>.png, <item>.png and den.txt (where each sprite stands, and
the spots the app draws into: the shelf's rows, the turntable's label, the speaker's cone, the clock's middle),
and a look at the whole den with everything in it to app/build/art/den.png.
"""
import os
import sys

import numpy as np
from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from cut import load, figures, crisp, outline  # noqa: E402

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, "art", "den", "sheets")
OUT = os.path.join(ROOT, "app", "src", "main", "resources", "den")
LOOK = os.path.join(ROOT, "app", "build", "art", "den.png")
W, H = 200, 132
FINE = 2

WALLS = ["plum", "night", "forest", "brick", "teal", "rose", "cream", "vinyl"]
SHEETS = {
    "items_floor": ["turntable", "gramophone", "speakers", "shelf"],
    "items_small": ["plant", "lava", "cassettes", "candles"],
    "items_wall": ["clock", "neon", "headphones", "disco"],
    "items_deco": ["rug", "garland", "stars", "frames"],
    "items_music": ["guitar", "gold_record", "metronome", "bongos"],
    # Other things for the same places (Item's slot): each stands where the thing it stands in for does.
    "vars_floor": ["synth", "boombox", "toy_piano", "harp"],
    "vars_pairs": ["amps", "floor_lamps", "pouf", "drum"],
    "vars_rugs": ["rug_stripes", "rug_vinyl", "rug_fluffy", "bunting"],
    "vars_ceiling": ["lanterns", "paper_lamp", "mobile", "dreamcatcher"],
    "vars_neon": ["neon_heart", "neon_moon", "neon_bolt", "moon_stickers"],
    "vars_plants": ["cactus", "sunflowers", "lemon_tree", "butterflies"],
    "vars_strings": ["violin", "banjo", "ukulele", "tambourine"],
    "vars_wall": ["sun_clock", "donut_clock", "platinum_record", "ticket"],
    "vars_shelf": ["walkman", "music_box", "hourglass", "mushroom_lamp"],
    "vars_sill": ["lantern", "snow_globe", "maracas", "bell"],
}
PLACE_OF = {
    "synth": "turntable", "boombox": "turntable", "toy_piano": "gramophone", "harp": "gramophone",
    "amps": "speakers", "floor_lamps": "speakers", "pouf": "bongos", "drum": "bongos",
    "rug_stripes": "rug", "rug_vinyl": "rug", "rug_fluffy": "rug", "bunting": "garland",
    "lanterns": "garland", "paper_lamp": "disco", "mobile": "disco", "dreamcatcher": "headphones",
    "neon_heart": "neon", "neon_moon": "neon", "neon_bolt": "neon", "moon_stickers": "stars",
    "cactus": "plant", "sunflowers": "plant", "lemon_tree": "plant", "butterflies": "stars",
    "violin": "guitar", "banjo": "guitar", "ukulele": "guitar", "tambourine": "headphones",
    "sun_clock": "clock", "donut_clock": "clock", "platinum_record": "gold_record", "ticket": "gold_record",
    "walkman": "cassettes", "music_box": "cassettes", "hourglass": "lava", "mushroom_lamp": "lava",
    "lantern": "candles", "snow_globe": "candles", "maracas": "metronome", "bell": "metronome",
}
# Glowing things: no dark outline round their light.
GLOW = ("neon", "stars", "neon_heart", "neon_moon", "neon_bolt", "moon_stickers")

# Where each thing goes, in den pixels: the box it's fitted into, and how it sits in that box
# (bottom: stands on the box's floor; top: hangs from its top; centre; stretch: fills it, for the flat rug).
# A thing may stand in several places.
PLACES = {
    "rug": [((44, 112, 112, 20), "stretch")],
    "plant": [((168, 76, 30, 44), "bottom")],
    "garland": [((4, 0, 58, 15), "top"), ((71, 0, 58, 15), "top"), ((138, 0, 58, 15), "top")],
    "candles": [((76, 48, 24, 16), "bottom")],
    "cassettes": [((10, 44, 18, 18), "bottom")],
    "lava": [((31, 36, 13, 26), "bottom")],
    "stars": [((140, 44, 56, 14), "top", "flip")],
    "shelf": [((4, 62, 48, 58), "bottom")],
    "speakers": [((54, 78, 16, 41), "bottom"), ((130, 78, 16, 41), "bottom", "flip")],
    "headphones": [((177, 20, 22, 26), "top")],
    "turntable": [((150, 80, 46, 40), "bottom")],
    "neon": [((158, 14, 30, 34), "centre")],
    "disco": [((161, 0, 20, 27), "top")],
    "gramophone": [((18, 80, 32, 40), "bottom")],
    "clock": [((46, 64, 14, 14), "centre")],
    # The music things (#26): the wall left of the window, the wall above the turntable, the sill left of the
    # candles, the floor's front left corner.
    "guitar": [((47, 10, 20, 50), "top")],
    "gold_record": [((147, 57, 20, 21), "centre")],
    "metronome": [((67, 46, 12, 18), "bottom")],
    "bongos": [((0, 108, 28, 23), "bottom")],
}
# The two frames: the picture inside each lands on the den's poster spots (DenArt.POSTER_RECTS).
FRAMES = [("poster", (12, 8, 26, 26)), ("band", (134, 16, 20, 20))]


def wall(name):
    im = Image.open(os.path.join(SRC, f"wall_{name}.png")).convert("RGB")
    k = W * FINE / im.width
    h = round(im.height * k)
    a = np.array(im.convert("RGBA"))
    small = crisp(a, W * FINE, h, colors=64)
    top = (h - H * FINE) // 2
    Image.fromarray(small[top:top + H * FINE], "RGBA").save(os.path.join(OUT, f"wall_{name}.png"))


def parts(sheet):
    im, solid = load(os.path.join(SRC, f"{sheet}.png"))
    runs = figures(solid, 4)
    out = []
    for s, e in runs:
        rows = np.nonzero(solid[:, s:e].any(axis=1))[0]
        out.append(im[rows[0]:rows[-1] + 1, s:e].copy())
    return out


def magenta(a):
    r, g, b = a[..., 0].astype(int), a[..., 1].astype(int), a[..., 2].astype(int)
    return (r > 170) & (b > 170) & (g < 110)


def fit(part, box, how):
    x, y, w, h = box
    ph, pw = part.shape[:2]
    if how == "stretch":
        return w, h, x, y
    k = min(w / pw, h / ph)
    sw, sh = max(1, round(pw * k)), max(1, round(ph * k))
    sx = x + (w - sw) // 2
    sy = y + h - sh if how == "bottom" else y if how == "top" else y + (h - sh) // 2
    return sw, sh, sx, sy


def shrink(part, sw, sh, line=True):
    s = crisp(part, sw, sh)
    return outline(s) if line else s


def fine(part, sw, sh, line=True):
    """The picture the app shows: FINE times the den's pixels."""
    return shrink(part, sw * FINE, sh * FINE, line)


def runs_of(mask):
    out, x = [], 0
    while x < len(mask):
        if mask[x]:
            s = x
            while x < len(mask) and mask[x]:
                x += 1
            out.append((s, x))
        else:
            x += 1
    return out


def main():
    os.makedirs(OUT, exist_ok=True)
    for w in WALLS:
        wall(w)
    table, extra, sprites, shown = [], [], {}, {}
    for sheet, names in SHEETS.items():
        if not os.path.exists(os.path.join(SRC, f"{sheet}.png")):
            print("no sheet yet:", sheet)
            continue
        got = parts(sheet)
        if len(got) != len(names):
            print(f"{sheet}: {len(got)} figures for {len(names)} things, skipped")
            continue
        for name, part in zip(names, got):
            if name == "frames":
                solid = part[..., 3] >= 128
                for (fname, (tx, ty, tw, th)), (s, e) in zip(FRAMES, figures(solid, 2)):
                    f = part[:, s:e].copy()
                    rows = np.nonzero((f[..., 3] >= 128).any(axis=1))[0]
                    f = f[rows[0]:rows[-1] + 1]
                    hole = magenta(f)
                    ys, xs = np.nonzero(hole)
                    hx0, hx1, hy0, hy1 = xs.min(), xs.max() + 1, ys.min(), ys.max() + 1
                    f[hole] = 0
                    k = tw / (hx1 - hx0)
                    sw, sh = round(f.shape[1] * k), round(f.shape[0] * k)
                    sx, sy = round(tx - hx0 * k), round(ty - hy0 * k)
                    spr = fine(f, sw, sh)
                    # The picture fills the hole exactly: nothing of the frame may cover it.
                    spr[(ty - sy) * FINE:(ty - sy + th) * FINE, (tx - sx) * FINE:(tx - sx + tw) * FINE] = 0
                    sprites[fname] = spr
                    shown[fname] = spr
                    table.append((fname, sx, sy, sw, sh, ""))
                continue
            for place in PLACES[PLACE_OF.get(name, name)]:
                box, how = place[0], place[1]
                flip = len(place) > 2
                sw, sh, sx, sy = fit(part, box, how)
                if name not in sprites:
                    # Measured on the den's pixels; shown finer.
                    sprites[name] = shrink(part, sw, sh, line=name not in GLOW)
                    shown[name] = fine(part, sw, sh, line=name not in GLOW)
                    if name == "stars":
                        # Shrunk, their soft glow turns olive: they're glow stickers, so make them glow.
                        s = shown[name]
                        s[..., :3] = (s[..., :3] * 0.35 + np.array([214, 255, 168]) * 0.65).astype(np.uint8)
                table.append((name, sx, sy, sw, sh, "flip" if flip else ""))
            spr = sprites[name]
            first = next(t for t in table if t[0] == name)
            _, sx, sy, sw, sh, _ = first
            if name == "shelf":
                # The openings: dark rows down the middle, and how far they reach to the sides.
                L = spr[..., :3].astype(float).mean(axis=2)
                dark = (L[:, sw // 2] < 50) & (spr[:, sw // 2, 3] > 0)
                rows = [(a, b) for a, b in runs_of(dark) if b - a >= 6]
                mid = (rows[0][0] + rows[0][1]) // 2
                inside = np.nonzero((L[mid] < 50) & (spr[mid, :, 3] > 0))[0]
                inside = inside[(inside > 1) & (inside < sw - 2)]
                extra.append(("shelf.x", sx + inside.min() + 1, sx + inside.max()))
                extra.append(("shelf.rows", *[v for a, b in rows[:3] for v in (sy + a, sy + b)]))
            if name == "turntable":
                r, g, b = (spr[..., i].astype(int) for i in range(3))
                red = (r > 150) & (g > 50) & (g < 140) & (b < 90) & (spr[..., 3] > 0)
                red[int(sh * 0.3):] = False
                # The label is the biggest red patch on the record (the arm and the cabinet's edge are other bits).
                from scipy import ndimage
                lab, n = ndimage.label(red)
                if n:
                    red = lab == 1 + int(np.argmax(ndimage.sum(red, lab, range(1, n + 1))))
                ys, xs = np.nonzero(red)
                if len(xs):
                    extra.append(("turntable.label", sx + xs.min(), sy + ys.min(), xs.max() - xs.min() + 1, ys.max() - ys.min() + 1))
            if name == "speakers":
                extra.append(("speakers.cone", sw // 2, round(sh * 0.6), max(2, sw // 3)))
            if name == "clock":
                extra.append(("clock.centre", sx + sw // 2, sy + sh // 2, sw // 2))
    for name, spr in shown.items():
        Image.fromarray(spr, "RGBA").save(os.path.join(OUT, f"{name}.png"))
    with open(os.path.join(OUT, "den.txt"), "w", encoding="utf-8") as f:
        f.write("# Made by tools/den.py: name x y w h [flip] (where a sprite stands), then the spots the app draws into.\n")
        for name, sx, sy, sw, sh, flag in table:
            f.write(f"{name} {sx} {sy} {sw} {sh} {flag}".rstrip() + "\n")
        for e in extra:
            f.write(" ".join(str(int(v)) if not isinstance(v, str) else v for v in e) + "\n")
    # A look at the den with everything in it, on the plum wall.
    room = Image.open(os.path.join(OUT, "wall_plum.png")).convert("RGBA")
    for name, sx, sy, sw, sh, flag in table:
        if name in PLACE_OF:
            continue
        im = Image.fromarray(shown[name], "RGBA")
        if flag == "flip":
            im = im.transpose(Image.FLIP_LEFT_RIGHT)
        room.alpha_composite(im, (sx * FINE, sy * FINE))
    os.makedirs(os.path.dirname(LOOK), exist_ok=True)
    room.resize((W * 4, H * 4), Image.NEAREST).save(LOOK)
    print("den:", len(sprites), "sprites,", len(WALLS), "walls ->", OUT)


if __name__ == "__main__":
    main()
