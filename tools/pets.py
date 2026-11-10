"""
Paints the Songling pets: every species at every stage (egg, baby, young, adult, ancient), 80 x 80.

    python tools/pets.py [species ...]

Writes art/pets/<species>_<stage>.png, art/pets/eyes.txt (eye boxes, for blinking and sleep) and a
preview sheet art/preview.png (4x, on the den's background).
"""
import os
import sys

import numpy as np
from PIL import Image, ImageDraw

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from pix import N, EYE, OUT, WHITE, Pen, Px, Ramp, bez, flat, hx, mix  # noqa: E402

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUTDIR = os.path.join(ROOT, "art", "pets")
STAGES = ["egg", "baby", "young", "adult", "ancient"]
SCALE = [0.86, 0.6, 0.76, 0.9, 1.0]


class R:
    cream = Ramp("5c3a4a", "926c6e", "c4a094", "e4cab2", "f6e8d0", "fffaec")
    wood = Ramp("2a1420", "4c2426", "743c2c", "9c5c38", "c4844e", "e2b074", "f6d6a0")
    gold = Ramp("46221e", "80481e", "ba7826", "e4aa38", "fad664", "fff4ba")
    brass = Ramp("3c2418", "6e4422", "a06c2e", "c8963e", "e6c068", "fae6a6")
    iron = Ramp("1e1a2c", "38344a", "5a586e", "848496", "b6b8c4", "e2e6ec")
    tongue = Ramp("601634", "a43452", "da6078", "f496a6", "ffccd2")
    velvet = Ramp("2a0616", "4a0c22", "741a30", "a02c3e", "c8505a", "e88078")
    pink = Ramp("68244e", "b04874", "e880a2", "fcc0ce", "fff0f2")
    beak = Ramp("663418", "aa641c", "de9624", "f8c444", "ffe88c")
    snow = Ramp("46405c", "7a768e", "acaabc", "d4d4de", "eeeef2", "ffffff")
    peach = Ramp("6e3a3a", "a8604e", "d88e6a", "f2b48a", "fcd6b0", "fff0dc")
    dusk = Ramp("1c1230", "2e1e48", "463064", "604682", "7e62a0", "a088bc", "c6b2d8")
    char = Ramp("120e14", "1e181e", "2e2628", "40363a", "56494c", "6e6064", "8c7e80")
    ember = Ramp("6e1e28", "b03826", "e2682e", "fa9e40", "ffd678", "fff8cc")
    redb = Ramp("3c0c1c", "6e1424", "a42630", "d0483a", "ec7c56", "f8b484")
    slate = Ramp("1a1c2a", "2c3042", "42485c", "5c6478", "7a8296", "9ea6b6", "c8ced8")
    ivory = Ramp("6e6258", "a49a8a", "cfc6b2", "e8e2d0", "f8f4e8", "fffef8")
    black = Ramp("0a0812", "121022", "1c1a32", "282744", "36365a", "4a4c74", "686c96")
    felt = Ramp("10142c", "1a2246", "26345e", "344a7a", "4a6496", "6c86b2")
    brown = Ramp("2c1418", "4e2822", "764030", "9a5c3e", "be8050", "daa66c", "f0cc98")
    tan = Ramp("443022", "6e4a2e", "9a6c3e", "c49456", "e2bc78", "f6deaa")
    sky = Ramp("10284e", "1e4c82", "3678b6", "64b0e2", "a8dcfa", "e4f8ff")


GENRE = ["#e2584a", "#f0a63a", "#f2d84c", "#6cc46a", "#4cb4d8", "#6a7ae0", "#b06ad8", "#e86aa8", "#f4f0e0"]


def feet(p, xs, y, ramp=R.beak, w=2.2, h=1.6):
    for x in xs:
        p.shade(p.ell(x, y, w * 1.4, h), p.dome(x, y, w * 1.4, h), ramp)


IRIS = "7a4a30"


def eyes(p, a, b, r, ry=None):
    """Two big eyes: larger than drawn before, the way chibi pets have them."""
    k = 1.42
    return [p.eye(a[0], a[1] + r * 0.15, r * k, None if ry is None else ry * k, iris=IRIS), p.eye(b[0], b[1] + r * 0.15, r * k, None if ry is None else ry * k, iris=IRIS)]


# ---------------------------------------------------------------- birds

def bird(p, st, body, belly, wing=None, head=None):
    """A round songbird; returns (eyes, head centre, head radius)."""
    head = head or body
    wing = wing or body
    if st == 0:
        b, h, er = (50, 72, 17, 15), (46, 53, 14, 13), 3.8
    elif st == 1:
        b, h, er = (51, 69, 17, 17), (45, 47, 13, 12), 3.5
    else:
        b, h, er = (52, 66, 17, 20), (43, 42, 12.5, 11.5), 3.3
    feet(p, [b[0] - 6, b[0] + 3], 86, w=2.4)
    p.shade(p.ell(*b), p.dome(*b), body, rim=belly[-1])
    bm = p.ell(b[0] - 5, b[1] + 5, b[2] * 0.62, b[3] * 0.68) & p.ell(*b)
    p.shade(bm, p.dome(b[0] - 5, b[1] + 5, b[2] * 0.62, b[3] * 0.68), belly, outline=False)
    wx, wy = b[0] + 6, b[1] - 1
    p.shade(p.ell(wx, wy, 8, b[3] * 0.72, ang=-18), p.dome(wx, wy, 8, b[3] * 0.72, ang=-18), wing)
    p.shade(p.ell(*h), p.dome(*h), head, rim=belly[-1])
    hx_, hy, hr = h[0], h[1], h[2]
    # beak, pointing left
    bx = hx_ - hr + 1
    p.shade(p.poly([bx + 2, hy - 1.5, bx - 5.5, hy + 1.2, bx + 2, hy + 3.5]), p.face(-0.3, -0.4, 1), R.beak)
    e = eyes(p, (hx_ - hr * 0.42, hy - 0.5), (hx_ + hr * 0.3, hy - 0.5), er)
    p.cheek(hx_ - hr * 0.55, hy + hr * 0.42, 2.4)
    return e, (hx_, hy, hr), b


def lyrebird(p, st):
    brown = Ramp.around("#9a6a48")
    n = [1, 3, 5, 9][st]
    base = (58, 74) if st else (60, 76)
    if st == 0:
        m, nn = p.stroke(bez((60, 78), (74, 70), (78, 54), (70, 50)) + bez((70, 50), (64, 48), (64, 56), (68, 56), 6)[1:], 3.0, 1.6)
        p.shade(m, nn, Ramp.around(GENRE[0]))
    else:
        # the filaments first, fanning between the arms
        for i in range(n - 2 if st >= 2 else n):
            t = (i + 0.5) / max(1, (n - 2 if st >= 2 else n))
            tx = 40 + 40 * t
            ty = 20 + 10 * abs(t - 0.5) + (6 if st == 1 else 0)
            col = Ramp.around(GENRE[i % len(GENRE)])
            pts = bez(base, (base[0] + (tx - base[0]) * 0.3, 50), (tx, ty + 14), (tx, ty), 14)
            m, nn = p.stroke(pts, 1.4 if st < 3 else 1.6, 1.0)
            p.shade(m, nn, col, outline=False, dither=0.2)
            if st == 3:
                p.shade(p.ell(tx, ty, 2.0, 2.4), p.dome(tx, ty, 2, 2.4), R.gold, outline=False)
        if st >= 2:
            for side in (-1, 1):
                pts = bez(base, (base[0] + side * 22, 64), (base[0] + side * 30, 40), (base[0] + side * 20, 18), 18)
                pts += bez(pts[-1], (base[0] + side * 16, 12), (base[0] + side * 26, 10), (base[0] + side * 28, 16), 8)[1:]
                m, nn = p.stroke(pts, 4.2, 2.4)
                ramp = Ramp("3c2018", "6a3a24", "9a5e34", "c8884a", "e8b878", "f8e0b0")
                p.shade(m, nn, ramp, rim=R.gold[4])
                # barring across the arm
                for k in range(3, len(pts) - 3, 3):
                    p.fill(p.ell(pts[k][0], pts[k][1], 1.6, 1.6) & m, "#f4e2b4", 200)
                if st == 3:
                    tip = pts[-1]
                    p.shade(p.ell(tip[0], tip[1], 3, 3), p.dome(tip[0], tip[1], 3, 3), R.gold)
    e, h, b = bird(p, st, brown, R.cream, wing=Ramp.around("#7a5038"))
    if st >= 1:
        # a little crest
        hx_, hy, hr = h
        for i, a in enumerate((-30, -10, 10)[: 1 + st]):
            m, nn = p.stroke(bez((hx_ + 2, hy - hr + 1), (hx_ + 4 + i * 2, hy - hr - 4), (hx_ + 8 + i * 3, hy - hr - 6), (hx_ + 10 + i * 3, hy - hr - 4 + i), 8), 1.4, 0.8)
            p.shade(m, nn, brown, outline=False)
    return e


def lark(p, st):
    gold = Ramp("5a3416", "8e5a22", "c08a36", "e2b456", "f4d684", "fff0c0")
    dawn = Ramp("6a2c44", "a84a5e", "dc7a7a", "f4a88e", "fcd2aa", "fff0dc")
    # tail: short fan
    for i, a in enumerate((-10, 8, 26)):
        m = p.ell(66 + i, 72 - i * 2, 4, 11, ang=60 + a)
        p.shade(m, p.dome(66 + i, 72 - i * 2, 4, 11, ang=60 + a), dawn)
    e, h, b = bird(p, st, gold, R.cream, wing=dawn)
    hx_, hy, hr = h
    if st >= 1:
        # a tuning-fork crest: a stem and two prongs
        top = hy - hr
        ln = [0, 5, 8, 10][st]
        stem, sn = p.stroke([(hx_ + 1, top + 2), (hx_ + 3, top - 3)], 1.3)
        p.shade(stem, sn, R.iron, outline=False)
        for dx in (-1.8, 1.8):
            m, nn = p.stroke([(hx_ + 3 + dx, top - 3), (hx_ + 3 + dx + 0.6, top - 3 - ln)], 1.2)
            p.shade(m, nn, R.iron, outline=False)
        p.fill(p.box(hx_ + 1.2, top - 3.6, hx_ + 4.8, top - 2.4), R.iron[3])
    if st == 3:
        cols = ["#f4a6c0", "#fff2a0", "#a8d8ff", "#f4a6c0", "#fff2a0"]
        for i, c in enumerate(cols):
            a = np.radians(200 + i * 35)
            x, y = hx_ + np.cos(a) * hr * 0.95, hy + np.sin(a) * hr * 0.8
            p.shade(p.ell(x, y, 2.2, 2.2), p.dome(x, y, 2.2, 2.2), Ramp.around(c))
            p.fill(p.ell(x, y, 0.7, 0.7), "#ffe066")
    # sunrise band on the wing
    wing = p.ell(b[0] + 6, b[1] - 1, 8, b[3] * 0.72, ang=-18)
    for i in range(3):
        p.fill(wing & (p.V > b[1] + 3 + i * 3) & (p.V < b[1] + 4.2 + i * 3), ["#fcd27a", "#f4a88e", "#dc7a7a"][i], 200)
    return e


def parrot(p, st):
    green = Ramp("0e2e1e", "16502a", "247a34", "3ca442", "6cc858", "a8e480", "dcf6b4")
    yellow = Ramp("6a4a10", "a87a16", "dcae22", "f4d43c", "fcec88", "fffad0")
    red = R.redb
    # tail: long feathers
    ln = [8, 14, 18, 26][st]
    for i, (c, a) in enumerate(((red, 30), (Ramp.around("#3c78d8"), 45), (green, 58))):
        cx, cy = 66 + i * 2, 74 + ln * 0.35
        m = p.ell(cx, cy, 3.4, ln, ang=a + 90)
        p.shade(m, p.dome(cx, cy, 3.4, ln, ang=a + 90), c)
    e, h, b = bird(p, st, green, yellow, wing=green)
    hx_, hy, hr = h
    # repeat-sign crest: two dots and a bar
    if st >= 1:
        for i in range(2 + st):
            m, nn = p.stroke(bez((hx_, hy - hr + 2), (hx_ + 2 + i, hy - hr - 6), (hx_ + 6 + i * 3, hy - hr - 8 - i), (hx_ + 9 + i * 3, hy - hr - 5 - i), 8), 1.8, 0.9)
            p.shade(m, nn, red if i % 2 == 0 else yellow, outline=True)
    # note spots on the wing
    wx, wy = b[0] + 6, b[1] - 1
    for i in range(min(3, st + 1)):
        x, y = wx - 2 + i * 2.6, wy - 3 + i * 4
        p.fill(p.ell(x, y, 1.4, 1.2), "#fff2a0")
        p.fill(p.box(x + 1, y - 4.5, x + 1.8, y), "#fff2a0")
    # a redder face patch
    p.fill(p.ell(hx_ - 4, hy + 3, 4, 3) & p.ell(hx_, hy, hr, hr), red[3], 140)
    if st == 3:
        # beak open mid-song
        bx = hx_ - hr + 1
        p.fill(p.poly([bx + 1, hy + 2.6, bx - 4.5, hy + 4.5, bx + 1.5, hy + 5.5]), R.tongue[2])
        p.shade(p.poly([bx + 1.5, hy + 4.2, bx - 4, hy + 5.4, bx + 1.5, hy + 7]), p.face(), R.beak)
        for i in range(3):
            x, y = bx - 7 - i * 4, hy - 4 - i * 5
            p.fill(p.ell(x, y, 1.5, 1.2), "#fffad0")
            p.fill(p.box(x + 1, y - 4, x + 1.8, y), "#fffad0")
    return e


# ---------------------------------------------------------------- music box

def musicbox(p, st):
    wood = R.wood
    w = [30, 40, 46, 50][st]
    h = [24, 28, 30, 32][st]
    x0, x1 = 50 - w / 2, 50 + w / 2
    top, bot = 86 - h, 85
    # stubby feet
    for x in (x0 + 4, x1 - 4):
        p.shade(p.ell(x, 86, 3.2, 2.2), p.dome(x, 86, 3.2, 2.2), wood)
    # the lid, open and tipped back (shut on the egg-fresh baby)
    open_h = [5, 12, 16, 18][st]
    lid = p.poly([x0 + 1, top, x1 - 1, top, x1 - 4, top - open_h, x0 + 4, top - open_h])
    p.shade(lid, p.face(0, -0.2, 1), Ramp("2a1420", "4c2426", "743c2c", "9c5c38", "c4844e", "e2b074"))
    inner = p.poly([x0 + 3, top, x1 - 3, top, x1 - 5.5, top - open_h + 2, x0 + 5.5, top - open_h + 2])
    p.shade(inner, p.face(0, 0.3, 1), R.velvet, outline=False)
    if st >= 2:
        # the brass cylinder and its pins
        cy = top - 3.2
        cyl = p.box(x0 + 8, cy - 2.6, x1 - 8, cy + 2.6)
        p.shade(cyl, p.log(cy, 2.6), R.brass)
        rng = np.random.default_rng(3)
        for i in range(int((x1 - x0 - 18) / 2.2)):
            x = x0 + 9.5 + i * 2.2
            p.fill(p.box(x, cy - 2 + rng.integers(0, 3), x + 0.9, cy - 1 + rng.integers(0, 3)), "#fff4c0")
        # the comb
        for i in range(int((x1 - x0 - 18) / 2.2)):
            x = x0 + 9.5 + i * 2.2
            p.fill(p.box(x, cy + 2.6, x + 1, cy + 4.5), R.iron[4])
    if st == 3:
        # a tiny ballerina on a stem
        bx, by = 50, top - open_h + 1
        p.fill(p.box(bx - 0.5, by - 4, bx + 0.5, by + 2), R.brass[3])
        p.shade(p.ell(bx, by - 6, 4.5, 1.8), p.dome(bx, by - 6, 4.5, 1.8), R.pink)
        p.shade(p.ell(bx, by - 9.5, 1.3, 2.6), p.dome(bx, by - 9.5, 1.3, 2.6), R.peach, outline=False)
        p.shade(p.ell(bx, by - 13, 1.6, 1.6), p.dome(bx, by - 13, 1.6, 1.6), R.peach)
        p.fill(p.box(bx - 3, by - 11.6, bx + 3, by - 10.8), R.peach[3])
    # the box body: front face and a sliver of side
    front = p.box(x0, top, x1, bot)
    tex = (np.sin(p.V * 1.7 + np.sin(p.U * 0.4) * 1.5) * 0.05).astype(np.float32)
    p.shade(front, p.face(-0.15, 0.1, 1), wood, tex=tex, rim=wood[5])
    p.fill(p.box(x0, top, x1, top + 1.6), wood[5])
    # brass corners and keyhole band
    for (cx, cy) in ((x0 + 2, top + 2), (x1 - 2, top + 2), (x0 + 2, bot - 2), (x1 - 2, bot - 2)):
        p.shade(p.box(cx - 2, cy - 2, cx + 2, cy + 2), p.face(-0.3, -0.3, 1), R.brass, outline=False)
    if st >= 1:
        p.fill(p.box(x0, bot - 6, x1, bot - 5), wood[1])
    if st == 3:
        # inlay
        for m in (p.box(x0 + 4, top + 4, x1 - 4, top + 4.8), p.box(x0 + 4, bot - 8, x1 - 4, bot - 7.2)):
            p.fill(m, R.gold[4])
        for i in range(5):
            x = x0 + 8 + i * (w - 16) / 4
            p.fill(p.ell(x, bot - 3, 1, 1), R.gold[4])
    # the key on the side
    if st >= 1:
        ky = top + h * 0.45
        p.shade(p.box(x1, ky - 1, x1 + 4, ky + 1), p.log(ky, 1), R.brass)
        for dy in (-3, 3):
            p.shade(p.ell(x1 + 6, ky + dy, 2.4, 2.6), p.dome(x1 + 6, ky + dy, 2.4, 2.6), R.brass)
        p.fill(p.ell(x1 + 6, ky - 3, 0.8, 0.8) | p.ell(x1 + 6, ky + 3, 0.8, 0.8), R.velvet[1])
    ey = top + h * 0.42
    gap = [7, 8, 9, 9][st]
    e = eyes(p, (50 - gap, ey), (50 + gap - 2, ey), [4.2, 4.0, 3.8, 3.8][st])
    p.cheek(50 - gap - 3, ey + 5, 2.6)
    p.cheek(50 + gap + 1, ey + 5, 2.6)
    # a small smile under the eyes
    p.fill(p.ell(49, ey + 5, 2.2, 1.2) & ~p.ell(49, ey + 4.2, 2.2, 1.2), wood[0])
    return e


# ---------------------------------------------------------------- long-eared bat

def longear(p, st):
    fur = R.dusk
    b = (50, 70, 17, 15) if st else (50, 72, 17, 14)
    hcy = 54 if st else 60
    # wings folded at the sides
    for side in (-1, 1):
        x = 50 + side * 15
        pts = [50 + side * 6, 60, x + side * 9, 62, x + side * 10, 74, x + side * 6, 72, x + side * 4, 78, x, 75, 50 + side * 8, 80]
        p.shade(p.poly(pts), p.face(side * 0.4, 0, 1), Ramp("140c22", "24183a", "3a2a54", "52406e", "6c5a88"))
    feet(p, [45, 55], 86, ramp=R.peach, w=2)
    p.shade(p.ell(*b), p.dome(*b), fur, rim=R.peach[4])
    p.shade(p.ell(50, b[1] + 3, 10, 9) & p.ell(*b), p.dome(50, b[1] + 3, 10, 9), R.peach, outline=False)
    # ears
    eh = [11, 15, 18, 19][st]
    for side in (-1, 1):
        cx, cy, a = 50 + side * 9, hcy - 10 - eh * 0.4, side * 18
        p.shade(p.ell(cx, cy, 6.5 + st * 0.4, eh, ang=a), p.dome(cx, cy, 6.5, eh, ang=a), fur)
        p.shade(p.ell(cx + side * 0.5, cy + 2, 3.6, eh * 0.72, ang=a), p.dome(cx, cy + 2, 3.6, eh * 0.72, ang=a), R.pink, outline=False)
    hr = 13 if st else 13.5
    p.shade(p.ell(50, hcy, hr + 1, hr), p.dome(50, hcy, hr + 1, hr), fur, rim=R.peach[4])
    if st >= 2:
        # the band over the head, joining the ears like headphones
        pts = bez((50 - 16, hcy - 2), (50 - 18, hcy - 26), (50 + 18, hcy - 26), (50 + 16, hcy - 2), 20)
        m, nn = p.stroke(pts, 1.8)
        p.shade(m, nn, R.iron)
        for side in (-1, 1):
            cx = 50 + side * 15.5
            cush = R.peach if st == 2 else Ramp.around("#7ae0e8")
            p.shade(p.ell(cx, hcy + 1, 4, 6), p.dome(cx, hcy + 1, 4, 6), Ramp("1e1a2c", "38344a", "5a586e", "848496", "b6b8c4"))
            p.shade(p.ell(cx - side * 1.2, hcy + 1, 2.4, 5), p.dome(cx - side * 1.2, hcy + 1, 2.4, 5), cush, outline=False)
            if st == 3:
                p.glow(cx, hcy + 1, 8, "#9af4ff", 70)
    if st == 3:
        # a cable tail with a plug
        pts = bez((64, 78), (78, 84), (86, 70), (80, 62), 18)
        m, nn = p.stroke(pts, 1.3)
        p.shade(m, nn, R.iron, outline=False)
        p.shade(p.box(78, 57, 82.5, 63), p.column(80.2, 2.2), R.brass)
        p.fill(p.box(79.3, 54, 80.8, 57), R.iron[4])
    e = eyes(p, (44.5, hcy + 1), (55.5, hcy + 1), [4.0, 3.6, 3.4, 3.4][st])
    # nose and fangs
    p.fill(p.ell(50, hcy + 5.5, 1.6, 1), R.pink[1])
    for x in (48.5, 51):
        p.fill(p.box(x, hcy + 7.4, x + 1, hcy + 9), WHITE)
    p.cheek(42, hcy + 6, 2.4)
    p.cheek(58, hcy + 6, 2.4)
    return e


# ---------------------------------------------------------------- basilisk

def basilisk(p, st):
    char, belly = R.char, R.redb
    # tail
    tl = [10, 16, 20, 22][st]
    pts = bez((64, 80), (74, 86), (68 + tl, 84), (66 + tl, 66 - st * 2), 18)
    m, nn = p.stroke(pts, 5.5, 1.6)
    p.shade(m, nn, char, rim=R.ember[2])
    if st >= 2:
        # a spike at the tail tip like a pick
        tx, ty = pts[-1]
        p.shade(p.poly([tx - 3, ty + 1, tx + 3, ty + 1, tx, ty - 6]), p.face(0.3, -0.3, 1), R.ember)
    b = (52, 72, 19, 14) if st else (52, 74, 18, 12)
    p.shade(p.ell(63, 82, 5, 6), p.dome(63, 82, 5, 6), char)
    p.shade(p.ell(*b), p.dome(*b), char, rim=R.ember[2])
    bm = p.ell(48, b[1] + 6, 13, 7) & p.ell(*b)
    p.shade(bm, p.dome(48, b[1] + 6, 13, 7), belly, outline=False)
    # belly plates
    for i in range(4):
        p.fill(bm & (np.abs(p.U - (40 + i * 5)) < 0.5), belly[1], 180)
    # speaker-grille dots on the back
    grille = p.ell(56, b[1] - 3, 13, 8) & p.ell(*b)
    gm = grille & ((np.floor(p.U * p.k) % 3) == 0) & ((np.floor(p.V * p.k) % 3) == 0)
    p.fill(gm, char[0], 230)
    if st == 3:
        for i, (x, y) in enumerate(((50, 60), (60, 61), (68, 66))):
            pl = p.box(x - 4, y - 3, x + 4, y + 3)
            p.shade(pl, p.face(-0.2, -0.3, 1), R.iron)
            p.shade(p.ell(x - 1.5, y, 1.3, 1.3), p.dome(x - 1.5, y, 1.3, 1.3), R.gold, outline=False)
            p.shade(p.ell(x + 1.8, y, 1.3, 1.3), p.dome(x + 1.8, y, 1.3, 1.3), R.gold, outline=False)
    p.shade(p.ell(40, 82, 5, 6), p.dome(40, 82, 5, 6), char)
    for x in (37, 40, 43):
        p.fill(p.ell(x, 87, 1.2, 1), R.ivory[2])
    # head
    h = (36, 56, 13, 11) if st else (38, 60, 14, 12)
    if st >= 2:
        # mohawk crest along head and back
        for i in range(6):
            x = 42 + i * 4.6
            y = h[1] - h[3] + 1 + i * 2.2
            ht = [0, 0, 7, 9][st] - i * 0.6
            p.shade(p.poly([x - 2.6, y + 3, x + 2.6, y + 3, x + 1.5, y - ht]), p.face(0.2, -0.4, 1), R.ember if i % 2 == 0 else R.redb)
    if st >= 1:
        # horns like guitar headstocks, with tuning pegs
        n = 1 if st == 1 else 2
        for j in range(n):
            ox = j * 6
            pts = [(h[0] + 2 + ox, h[1] - h[3] + 3), (h[0] + 6 + ox, h[1] - h[3] - 6), (h[0] + 11 + ox, h[1] - h[3] - 12 - st)]
            m, nn = p.stroke(pts, 2.6, 2.0)
            p.shade(m, nn, R.ivory)
            for t in (0.55, 0.85):
                x = pts[0][0] + (pts[2][0] - pts[0][0]) * t
                y = pts[0][1] + (pts[2][1] - pts[0][1]) * t
                p.shade(p.ell(x + 2.6, y + 1, 1.2, 1.2), p.dome(x + 2.6, y + 1, 1.2, 1.2), R.gold, outline=False)
    p.shade(p.ell(*h), p.dome(*h), char, rim=R.ember[2])
    sx, sy = h[0] - h[2] + 2, h[1] + 4
    p.shade(p.ell(sx, sy, 7, 5.5), p.dome(sx, sy, 7, 5.5), char)
    p.fill(p.ell(sx - 4, sy - 1, 0.9, 0.7), char[0])
    p.fill(p.ell(sx + 1, sy + 3.2, 5, 0.6), char[0])
    e = eyes(p, (h[0] - 4, h[1] - 2), (h[0] + 5, h[1] - 2), [3.8, 3.4, 3.2, 3.2][st])
    for bx in e:
        if bx:
            cx, cy = (bx[0] + bx[2]) / 2, (bx[1] + bx[3]) / 2
            p.px.put(_pxell(cx, cy, 4.5) & ~p.px.solid(), R.ember[3], 0)
    for (ex, ey) in ((h[0] - 4, h[1] - 2), (h[0] + 5, h[1] - 2)):
        p.fill(p.ell(ex, ey + 1.5, 2.6, 1.2) & p.ell(ex, ey, 3.3, 3.8), R.ember[3], 190)
    return e


def _pxell(cx, cy, r):
    yy, xx = np.mgrid[0:N, 0:N] + 0.5
    return (xx - cx) ** 2 + (yy - cy) ** 2 <= r * r


# ---------------------------------------------------------------- owl conductor

def conductor(p, st):
    slate, ivory = R.slate, R.ivory
    b = (50, 66, 19, 21) if st else (50, 70, 18, 17)
    if st == 3:
        # tailcoat tails
        for side in (-1, 1):
            p.shade(p.poly([50 + side * 6, 70, 50 + side * 18, 74, 50 + side * 14, 88, 50 + side * 10, 84]), p.face(side * 0.3, 0, 1), R.black)
    feet(p, [44, 56], 86, ramp=R.gold)
    p.shade(p.ell(*b), p.dome(*b), slate, rim=ivory[4])
    bm = p.ell(50, b[1] + 6, 12, 13) & p.ell(*b)
    p.shade(bm, p.dome(50, b[1] + 6, 12, 13), ivory, outline=False)
    for i in range(3):
        for j in range(3 - (i % 2)):
            x, y = 44 + j * 5 + (i % 2) * 2.5, b[1] + 2 + i * 4
            p.fill(p.ell(x, y, 1.5, 0.7) & bm & ~p.ell(x, y - 0.8, 1.5, 0.7), ivory[1])
    # wings
    for side in (-1, 1):
        x = 50 + side * 17
        p.shade(p.ell(x, b[1] - 1, 6, 14, ang=-side * 12), p.dome(x, b[1] - 1, 6, 14, ang=-side * 12), slate)
    hy = b[1] - b[3] + 5 if st else b[1] - b[3] + 4
    # ear tufts
    for side in (-1, 1):
        x = 50 + side * 12
        p.shade(p.poly([x - 4 * side, hy - 1, x + 3 * side, hy - 3, x + 4 * side, hy - 11 - st]), p.face(side * 0.2, -0.5, 1), slate)
    # the face: two ivory discs
    for side in (-1, 1):
        cx = 50 + side * 7.5
        p.shade(p.ell(cx, hy + 6, 8, 7.6), p.dome(cx, hy + 6, 8, 7.6, flat=0.6), ivory)
    e = eyes(p, (42.5, hy + 6), (57.5, hy + 6), [4.4, 4.2, 4.0, 4.0][st])
    p.shade(p.poly([48, hy + 9, 52, hy + 9, 50, hy + 14]), p.face(0, -0.3, 1), R.gold)
    if st == 3:
        # bushy white brows
        for side in (-1, 1):
            m, nn = p.stroke([(50 + side * 3, hy + 1), (50 + side * 9, hy - 1), (50 + side * 15, hy + 1)], 1.6, 1.0)
            p.shade(m, nn, R.snow, outline=True)
    if st >= 2:
        # bow tie
        ty = hy + 17
        for side in (-1, 1):
            p.shade(p.poly([50, ty, 50 + side * 6, ty - 3, 50 + side * 6, ty + 3]), p.face(side * 0.2, 0, 1), R.snow if st == 2 else R.black)
        p.shade(p.ell(50, ty, 1.6, 1.6), p.dome(50, ty, 1.6, 1.6), R.snow if st == 2 else R.gold)
    # baton in the left wing
    if st == 0:
        m, nn = p.stroke([(34, b[1] + 2), (24, b[1] - 8), (22, b[1] - 12)], 1.0)
        p.shade(m, nn, R.brown, outline=False)
        p.fill(p.ell(23, b[1] - 10, 1.6, 1), "#6cc46a")
    elif st >= 1:
        m, nn = p.stroke([(34, b[1] + 1), (16, b[1] - 16)], 0.9)
        p.shade(m, nn, R.snow if st >= 2 else R.brown, outline=False)
        p.shade(p.ell(33, b[1], 2, 2), p.dome(33, b[1], 2, 2), R.gold if st >= 2 else R.brown)
    return e


# ---------------------------------------------------------------- jazz cat

def purr(p, st):
    fur = R.black
    # tail curling up behind, ending in a sax bell
    pts = bez((64, 84), (82, 86), (84, 66), (76, 56 - st * 2), 20)
    m, nn = p.stroke(pts, 3.6, 2.6 if st >= 2 else 1.6)
    p.shade(m, nn, fur, rim=R.felt[4])
    if st >= 2:
        tx, ty = pts[-1]
        bell = p.poly([tx - 2.5, ty + 2, tx + 2.5, ty + 2, tx + 5.5, ty - 6, tx - 5.5, ty - 6])
        p.shade(bell, p.column(tx, 5.5), R.brass)
        p.fill(p.ell(tx, ty - 6, 5.5, 1.6), R.brass[1])
        for t in (0.55, 0.7, 0.85):
            k = int(len(pts) * t)
            p.shade(p.ell(pts[k][0], pts[k][1], 1.3, 1.3), p.dome(pts[k][0], pts[k][1], 1.3, 1.3), R.brass, outline=False)
    b = (54, 72, 16, 15) if st else (52, 74, 15, 12)
    p.shade(p.ell(*b), p.dome(*b), fur, rim=R.felt[4])
    if st == 3:
        vest = p.ell(48, b[1] + 1, 9, 11) & p.ell(*b)
        p.shade(vest, p.dome(48, b[1] + 1, 9, 11), Ramp.around("#3a2a7a"), outline=False)
        p.fill(vest & (np.abs(p.U - 48) < 0.6), R.felt[1])
        for y in (b[1] - 4, b[1] + 1, b[1] + 6):
            p.shade(p.ell(49.5, y, 1.1, 1.1), p.dome(49.5, y, 1.1, 1.1), R.gold, outline=False)
    for x in (45, 55):
        p.shade(p.ell(x, 85, 4.5, 3), p.dome(x, 85, 4.5, 3), fur)
    h = (45, 52, 15, 13) if st else (45, 56, 15, 13)
    for side in (-1, 1):
        x = h[0] + side * 9
        y = h[1] - h[3] + 4
        ear = p.poly([x - 6, y + 2, x + 5, y + 1, x + side * 1.5, y - 10 - min(st, 2)])
        p.shade(ear, p.face(side * 0.3, -0.4, 1), fur)
        p.fill(p.poly([x - 3, y + 1, x + 2.5, y + 0.5, x + side * 1, y - 6]), R.pink[1], 220)
    p.shade(p.ell(*h), p.dome(*h), fur, rim=R.felt[4])
    if st >= 2:
        # a felt porkpie hat
        hy = h[1] - h[3] + 1
        p.shade(p.ell(h[0], hy + 1, 12, 2.4), p.dome(h[0], hy + 1, 12, 2.4), R.felt)
        p.shade(p.box(h[0] - 7, hy - 6, h[0] + 7, hy + 0.5), p.column(h[0], 7), R.felt)
        p.fill(p.box(h[0] - 7, hy - 2, h[0] + 7, hy - 0.5), R.gold[3] if st == 3 else R.redb[3])
    # golden eyes with slit pupils
    e = []
    for ex in (h[0] - 5.5, h[0] + 5):
        ey = h[1]
        r = [4.0, 3.7, 3.5, 3.5][st]
        m = p.ell(ex, ey, r, r * 1.1)
        p.shade(m, p.dome(ex, ey, r, r * 1.1), Ramp("4a3a10", "8a7018", "c8aa30", "e6d058", "f8ee9c"), outline=True, dither=0.2)
        p.fill(p.ell(ex + 0.3, ey, r * 0.32, r * 0.95), EYE[0])
        ys, xs = np.nonzero(m)
        p.px.put(_px(xs.min() + 1, ys.min() + 1, 2 if r > 3 else 1) & m, WHITE)
        e.append((int(xs.min()), int(ys.min()), int(xs.max() + 1), int(ys.max() + 1)))
    p.fill(p.poly([h[0] - 1.6, h[1] + 4.5, h[0] + 1.2, h[1] + 4.5, h[0] - 0.2, h[1] + 6]), R.pink[2])
    p.mouth(h[0] - 0.2, h[1] + 7.6, 1.8)
    wc = R.snow[3] if st == 3 else R.black[5]
    wl = 9 if st == 3 else 6
    for side, x0 in ((-1, h[0] - 6), (1, h[0] + 5)):
        for dy in (-0.6, 1.2):
            m, nn = p.stroke([(x0, h[1] + 5 + dy), (x0 + side * wl, h[1] + 4 + dy * 2)], 0.35)
            p.fill(m, wc)
    return e


def _px(x, y, s):
    m = np.zeros((N, N), bool)
    m[y:y + s, x:x + s] = True
    return m


# ---------------------------------------------------------------- axolotl

def axolotl(p, st):
    pink = Ramp("6a2c4e", "a84a72", "d87898", "f2a6bc", "fcd0dc", "fff0f4")
    m, nn = p.stroke(bez((62, 80), (74, 84), (84, 78), (88, 68), 16), 5, 1.5)
    p.shade(m, nn, pink, rim=pink[5])
    b = (54, 76, 16, 10)
    for x in (44, 62):
        p.shade(p.ell(x, 85, 3.4, 2.4), p.dome(x, 85, 3.4, 2.4), pink)
    p.shade(p.ell(*b), p.dome(*b), pink, rim=pink[5])
    if st == 3:
        # piano keys along the back
        for i in range(7):
            x = 46 + i * 3
            col = "#fffaf4" if i % 2 == 0 else "#2a2236"
            p.fill(p.box(x, 66.5, x + 2.4, 70) & p.ell(*b), col)
    h = (40, 62, 16, 12) if st else (42, 66, 16, 12)
    dots = [2, 3, 4, 5][st]
    for side in (-1, 1):
        for k, ang in enumerate((30, 5, -20)):
            base = (h[0] + side * (h[2] - 3), h[1] - 4 + k * 3.5)
            ln = 8 + st * 1.5
            a = np.radians(ang)
            tip = (base[0] + side * ln * np.cos(a), base[1] - ln * np.sin(a) - 2)
            mm, nn = p.stroke([base, tip], 1.8, 1.2)
            p.shade(mm, nn, Ramp.around("#e86aa0"), outline=True)
            for d in range(dots):
                t = (d + 1) / (dots + 0.5)
                x, y = base[0] + (tip[0] - base[0]) * t, base[1] + (tip[1] - base[1]) * t
                c = "#7af4ff" if (d + k) % 2 == 0 else "#ff8ad8"
                p.glow(x, y, 3.2, c, 90)
                p.fill(p.ell(x, y, 0.9, 0.9), c)
    p.shade(p.ell(*h), p.dome(*h), pink, rim=pink[5])
    e = eyes(p, (h[0] - 6, h[1] - 1), (h[0] + 6, h[1] - 1), [3.4, 3.2, 3.0, 3.0][st])
    p.fill(p.ell(h[0], h[1] + 5, 4.5, 1.6) & ~p.ell(h[0], h[1] + 4, 4.5, 1.6), pink[0])
    p.cheek(h[0] - 9, h[1] + 4, 2.4)
    p.cheek(h[0] + 9, h[1] + 4, 2.4)
    return e


# ---------------------------------------------------------------- raccoon

def raccoon(p, st):
    grey = Ramp("1e1c26", "34303c", "4e4a56", "6a6672", "8a8692", "aeaab4", "d4d0d8")
    pts = bez((64, 82), (80, 84), (86, 70), (80, 58), 20)
    m, nn = p.stroke(pts, 5.5, 4)
    p.shade(m, nn, grey, rim=grey[6])
    for k in range(3, len(pts) - 1, 4):
        p.fill(p.ell(pts[k][0], pts[k][1], 5.6, 1.5, ang=40) & m, grey[0], 230)
    b = (52, 72, 16, 14)
    p.shade(p.ell(*b), p.dome(*b), grey, rim=grey[6])
    p.shade(p.ell(50, 76, 9, 9) & p.ell(*b), p.dome(50, 76, 9, 9), R.snow, outline=False)
    if st >= 2:
        for k in range(9):
            a = np.radians(200 + k * 17.5)
            x, y = 50 + np.cos(a) * 9, 66 - np.sin(a) * 5
            p.shade(p.ell(x, y, 1.4, 1.2), p.dome(x, y, 1.4, 1.2), R.gold, outline=False)
        if st == 3:
            p.shade(p.ell(50, 71.5, 2.6, 3), p.dome(50, 71.5, 2.6, 3), R.gold)
    for x in (45, 57):
        p.shade(p.ell(x, 85, 4, 2.6), p.dome(x, 85, 4, 2.6), grey)
    h = (46, 52, 15, 12.5)
    for side in (-1, 1):
        x = h[0] + side * 10
        p.shade(p.ell(x, h[1] - 10, 4.6, 5.6, ang=side * 20), p.dome(x, h[1] - 10, 4.6, 5.6), grey)
        p.fill(p.ell(x, h[1] - 9, 2.4, 3.2, ang=side * 20), R.pink[1])
    p.shade(p.ell(*h), p.dome(*h), grey, rim=grey[6])
    p.fill(p.ell(h[0], h[1] + 4, 10, 6.5) & p.ell(*h), R.snow[4])
    p.fill(p.ell(h[0] - 5, h[1] - 0.5, 5, 3.6, ang=-10) | p.ell(h[0] + 5, h[1] - 0.5, 5, 3.6, ang=10), grey[0])
    if st >= 1:
        cap = Ramp.around("#d0483a")
        cy = h[1] - h[3] + 3
        p.shade(p.ell(h[0], cy + 1, 11, 6) & (p.V < cy + 2), p.dome(h[0], cy + 1, 11, 6), cap)
        p.shade(p.ell(h[0] - 11, cy + 1.6, 7, 1.8), p.dome(h[0] - 11, cy + 1.6, 7, 1.8), cap)
    e = eyes(p, (h[0] - 5, h[1] - 0.5), (h[0] + 5, h[1] - 0.5), [3.0, 2.8, 2.7, 2.7][st])
    p.fill(p.ell(h[0], h[1] + 5, 1.8, 1.2), grey[0])
    p.mouth(h[0], h[1] + 8, 2.2)
    if st == 3:
        bb = p.box(36, 72, 64, 85)
        p.shade(bb, p.face(-0.2, -0.2, 1), R.iron)
        for x in (42, 58):
            p.shade(p.ell(x, 78.5, 4.4, 4.4), p.dome(x, 78.5, 4.4, 4.4), grey)
            p.fill(p.ell(x, 78.5, 1.6, 1.6), grey[0])
        p.fill(p.box(47, 75, 53, 79), "#7af4ff")
        m, nn = p.stroke([(38, 72), (40, 67), (60, 67), (62, 72)], 0.9)
        p.fill(m, R.iron[1])
        for x in (36.5, 63.5):
            p.shade(p.ell(x, 78, 3, 2.6), p.dome(x, 78, 3, 2.6), grey)
    return e


# ---------------------------------------------------------------- hedgehog wanderer

def wanderer(p, st):
    quill = Ramp("1e1210", "3a2218", "5a3822", "7c5430", "a07444", "c49a62")
    face = R.tan
    if st == 3:
        m, nn = p.stroke([(18, 88), (14, 40)], 1.6)
        p.shade(m, nn, R.wood)
        for y in (48, 54, 60):
            p.fill(p.ell(15.6, y, 0.8, 0.8), R.wood[0])
    b = (54, 70, 19, 15)
    for i in range(15):
        a = np.radians(120 - i * 12)
        x, y = 56 + np.cos(a) * 15, 70 - np.sin(a) * 12
        tx, ty = 56 + np.cos(a) * 24, 70 - np.sin(a) * 21
        n = np.array([-np.sin(a), -np.cos(a)]) * 3.6
        p.shade(p.poly([x + n[0], y + n[1], x - n[0], y - n[1], tx, ty]), p.face(np.cos(a) * 0.5, -np.sin(a) * 0.5, 1), quill)
    p.shade(p.ell(*b), p.dome(*b), quill, tex=(np.sin(p.U * 2.2 + p.V) * 0.06).astype(np.float32))
    if st >= 2:
        cols = ["#c0503c", "#4c8a5a", "#d8a040", "#4a6aa8", "#9a5aa8", "#e0c890"]
        cloak = p.ell(58, 74, 17, 12 if st == 2 else 14) & p.ell(*b)
        for k in range(6):
            cx, cy = 49 + (k % 3) * 9, 70 + (k // 3) * 8
            m = p.box(cx - 4.5, cy - 4, cx + 4.5, cy + 4) & cloak
            p.shade(m, p.dome(58, 74, 17, 14), Ramp.around(cols[k]), outline=False)
            p.fill(m & ((np.abs(p.U - (cx - 4.3)) < 0.35) | (np.abs(p.V - (cy - 3.8)) < 0.35)), "#f6e8d0", 160)
    for x in (44, 60):
        p.shade(p.ell(x, 86, 3.6, 2.2), p.dome(x, 86, 3.6, 2.2), face)
    hf = p.ell(38, 68, 12, 11) | p.ell(28, 72, 7, 5)
    p.shade(hf, p.dome(36, 69, 14, 11), face, rim=face[5])
    p.shade(p.ell(22, 72, 2.4, 2.2), p.dome(22, 72, 2.4, 2.2), Ramp("1a0e14", "2c1820", "4a2a34", "6c4a50"))
    if st >= 1:
        m, nn = p.stroke([(64, 52), (72, 46)], 1.3)
        p.shade(m, nn, R.ivory)
    e = eyes(p, (33, 66), (41, 65.5), [3.2, 3.0, 2.8, 2.8][st])
    p.cheek(32, 72, 2.2)
    p.mouth(30, 74, 1.8)
    return e


# ---------------------------------------------------------------- cricket

def cricket(p, st):
    night = Ramp("0c0a24", "161444", "22226a", "34388e", "4c56b0", "7482cc", "a8b4e6")
    green = Ramp.around("#3e7a5e")
    m, nn = p.stroke([(62, 72), (76, 58), (84, 86)], 2.4, 1.4)
    p.shade(m, nn, green)
    for x in (42, 52):
        mm, nn = p.stroke([(x, 76), (x - 3, 86)], 1.2)
        p.shade(mm, nn, green, outline=False)
    b = (56, 72, 18, 10)
    p.shade(p.ell(*b), p.dome(*b), green, rim=green[6])
    wing = p.ell(60, 68, 17, 8, ang=-6)
    p.shade(wing, p.dome(60, 68, 17, 8, ang=-6), night, rim=night[6])
    rng = np.random.default_rng(7)
    for k in range([0, 2, 5, 8][st]):
        x, y = 48 + rng.random() * 24, 64 + rng.random() * 7
        p.fill(p.ell(x, y, 0.7, 0.7) & wing, "#fff2a0")
    if st == 3:
        p.fill(p.ell(64, 67, 3.4, 3.4) & ~p.ell(65.6, 66, 3, 3) & wing, "#fff2a0")
    h = (36, 66, 11, 11)
    for k, (tx, ty) in enumerate(((22, 34 - st * 3), (34, 30 - st * 3))):
        mm, nn = p.stroke(bez((h[0] - 2 + k * 3, h[1] - 9), (h[0] - 2 + k * 3, 48), (tx + 6, ty + 6), (tx, ty), 14), 0.7)
        p.fill(mm, green[1])
    p.shade(p.ell(*h), p.dome(*h), green, rim=green[6])
    e = eyes(p, (h[0] - 4, h[1] - 1), (h[0] + 4, h[1] - 1), [3.4, 3.2, 3.0, 3.0][st])
    p.cheek(h[0] - 6, h[1] + 4, 2)
    p.mouth(h[0], h[1] + 6.5, 1.8)
    if st >= 1:
        mm, nn = p.stroke([(22, 84), (30, 58)], 0.7)
        p.fill(mm, R.wood[4])
        mm, nn = p.stroke([(23, 82), (31, 60)], 0.35)
        p.fill(mm, R.snow[4])
    return e


# ---------------------------------------------------------------- vinyl snail

def snail(p, st):
    body = Ramp("2a4a2a", "3e6c38", "5a9448", "80b85c", "aad47c", "d4eaa8")
    m, nn = p.stroke([(22, 84), (74, 84)], 4.2, 3.2)
    p.shade(m, nn, body, rim=body[5])
    gold = st == 3
    rec = R.gold if gold else Ramp("0c0a10", "18141e", "26222e", "3a3644", "54505e", "726e7c")
    cx, cy, r = 54, 64, [12, 15, 17, 18][st]
    disc = p.ell(cx, cy, r, r)
    p.shade(disc, p.dome(cx, cy, r, r, flat=1.2), rec, rim=rec[5])
    grooves = [2, 4, 6, 7][st]
    d = np.sqrt((p.U - cx) ** 2 + (p.V - cy) ** 2)
    for g in range(grooves):
        rr = r * 0.4 + (r * 0.55) * (g + 1) / (grooves + 1)
        p.fill(disc & (np.abs(d - rr) < 0.32), R.gold[2] if gold else rec[4], 200)
    lab = Ramp.around(GENRE[[0, 4, 6, 1][st]])
    p.shade(p.ell(cx, cy, r * 0.36, r * 0.36), p.dome(cx, cy, r * 0.36, r * 0.36), lab, outline=False)
    p.fill(p.ell(cx, cy, 0.9, 0.9), rec[0])
    p.fill(disc & (np.abs((p.U - cx) + (p.V - cy) * 0.9 + r * 0.4) < 0.8) & (d > r * 0.4), WHITE, 120)
    hx_, hy = 26, 74
    for k, tx in enumerate((18, 27)):
        mm, nn = p.stroke([(hx_ - 2 + k * 4, hy - 4), (tx, hy - 14 - st)], 1.2)
        p.shade(mm, nn, body, outline=True)
    p.shade(p.ell(hx_, hy + 3, 8, 8), p.dome(hx_, hy + 3, 8, 8), body, rim=body[5])
    e = eyes(p, (18, hy - 14 - st), (27, hy - 14 - st), [2.8, 2.6, 2.6, 2.6][st])
    p.cheek(hx_ - 3, hy + 6, 2)
    p.fill(p.ell(hx_ - 2, hy + 6, 2, 0.9) & ~p.ell(hx_ - 2, hy + 5.2, 2, 0.9), body[0])
    return e


# ---------------------------------------------------------------- tortoise

def tortoise(p, st):
    skin = Ramp("2c2a14", "4a4620", "6c6630", "908a44", "b4ae62", "d6d08e")
    shellr = Ramp("3a2a12", "5e4418", "866424", "aa8838", "caa856", "e6cc84")
    for x in (34, 44, 60, 70):
        p.shade(p.ell(x, 84, 4.2, 4), p.dome(x, 84, 4.2, 4), skin)
    p.shade(p.ell(26, 70, 9, 8), p.dome(26, 70, 9, 8), skin, rim=skin[5])
    shell = p.ell(52, 76, 22, 20) & (p.V < 80)
    p.shade(shell, p.dome(52, 76, 22, 20), shellr)
    p.fill(p.box(30, 78, 74, 81) & p.ell(52, 76, 23, 20), shellr[2])
    plates = [(44, 68), (58, 66), (51, 74), (64, 74), (38, 75)][: 2 + st]
    cols = ["#e2584a", "#4cb4d8", "#f2d84c", "#6cc46a", "#b06ad8"]
    for k, (x, y) in enumerate(plates):
        if k == 1 and st >= 1:
            p.shade(p.box(x - 5, y - 3, x + 5, y + 3), p.face(-0.2, -0.2, 1), R.ivory)
            p.fill(p.box(x - 3.5, y - 1.2, x + 3.5, y + 1.2), "#3a3040")
            p.fill(p.ell(x - 2, y, 0.9, 0.9) | p.ell(x + 2, y, 0.9, 0.9), R.ivory[4])
        else:
            p.shade(p.ell(x, y, 4, 4), p.dome(x, y, 4, 4, flat=0.8), Ramp.around(cols[k % 5]))
            p.fill(p.ell(x, y, 0.8, 0.8), "#2a2236")
    if st == 3:
        rng = np.random.default_rng(5)
        moss = shell & (p.V < 63) & (rng.random(p.U.shape) < 0.5)
        p.fill(moss, "#6c9a44")
        horn = p.poly([52, 58, 55, 58, 64, 44, 57, 42])
        p.shade(horn, p.face(-0.3, -0.3, 1), R.brass)
        p.fill(p.ell(60.5, 43, 4.4, 1.6, ang=-15), R.brass[1])
    e = eyes(p, (22, 68), (29, 68), [3.0, 2.8, 2.7, 2.7][st])
    p.cheek(23, 73, 2)
    p.mouth(24, 75, 1.6)
    return e


# ---------------------------------------------------------------- whale

def whale(p, st):
    blue = Ramp("142034", "1e3450", "2e4e6e", "46708e", "6894ae", "94b8cc", "c8dce6")
    pearl = R.ivory
    m, nn = p.stroke(bez((64, 66), (74, 64), (78, 56), (82, 50), 12), 4, 2)
    p.shade(m, nn, blue)
    for side in (-1, 1):
        p.shade(p.ell(82 + side * 4, 48, 5, 2.4, ang=side * 30), p.dome(82 + side * 4, 48, 5, 2.4, ang=side * 30), blue)
    b = (48, 66, 22, 15)
    p.shade(p.ell(*b), p.dome(*b), blue, rim=blue[6])
    belly = p.ell(44, 72, 17, 8) & p.ell(*b)
    p.shade(belly, p.dome(44, 72, 17, 8), pearl, outline=False)
    for k in range(3):
        p.fill(belly & (np.abs(p.V - (70 + k * 2.6 + np.sin(p.U * 0.7) * 0.8)) < 0.35), blue[4])
    p.shade(p.ell(50, 77, 6, 2.8, ang=30), p.dome(50, 77, 6, 2.8, ang=30), blue)
    e = eyes(p, (34, 62), (42, 62), [3.2, 3.0, 2.9, 2.9][st])
    p.cheek(32, 68, 2.4)
    p.fill(p.ell(30, 70, 3, 1.2) & ~p.ell(30, 69.2, 3, 1.2), blue[0])
    for k in range(st):
        x, y = 38 + k * 6, 46 - k * 5
        p.fill(p.ell(x, y, 1.4, 1.1), "#c8f0ff")
        p.fill(p.box(x + 1, y - 4, x + 1.8, y), "#c8f0ff")
    if st == 3:
        for (x, y) in ((52, 56), (58, 60), (46, 54)):
            p.fill(p.ell(x, y, 1.3, 1.0), pearl[2])
        rng = np.random.default_rng(9)
        for k in range(6):
            p.fill(p.ell(40 + rng.random() * 24, 56 + rng.random() * 8, 0.6, 0.6), "#fff2a0")
    return e


# ---------------------------------------------------------------- echo

def echo(p, st):
    lav = Ramp("4a3a6e", "6a5a92", "9284b6", "b8acd6", "dcd4f0", "f6f2ff")

    def body(dx, alpha, outline=True):
        sh = p.ell(50 + dx, 54, 15, 16) | p.poly([35 + dx, 56, 65 + dx, 56, 64 + dx, 78, 58 + dx, 74, 54 + dx, 80, 48 + dx, 74, 42 + dx, 80, 36 + dx, 76])
        p.shade(sh, p.dome(50 + dx, 60, 16, 22), lav, outline=outline, alpha=alpha, rim=lav[5] if outline else None)
        return sh
    copies = [1, 2, 3, 3][st]
    for k in range(copies, 0, -1):
        body(k * 5, 60 + 25 * (copies - k))
    body(0, 255)
    if st == 3:
        for rr in (18, 22):
            d = np.sqrt((p.U - 50) ** 2 + ((p.V - 34) * 3.2) ** 2)
            p.fill((np.abs(d - rr) < 0.9) & (p.V < 40), "#fff2c0", 210)
    e = eyes(p, (45, 54), (55, 54), [3.6, 3.4, 3.2, 3.2][st])
    p.fill(p.ell(50, 61, 2, 2.2), lav[0])
    p.cheek(41, 59, 2.2)
    p.cheek(59, 59, 2.2)
    return e


# ---------------------------------------------------------------- firefly

def firefly(p, st):
    brown = Ramp("1a1010", "2e1c18", "48302a", "644636", "866048", "a8805e")
    amber = Ramp("7a3a10", "c06a18", "ec9a2a", "fcc248", "ffe08a", "fff6cc")
    for k, a in enumerate((-30, -60)):
        m = p.ell(56 + k * 4, 54, 6, 13, ang=a)
        p.shade(m, p.dome(56 + k * 4, 54, 6, 13, ang=a), Ramp("8aa0c0", "b4c8de", "dce8f4", "f4f8ff"), alpha=170)
    glow = [8, 11, 13, 16][st]
    p.glow(68, 70, glow + 6, "#ffd070", 80)
    p.shade(p.ell(66, 70, 9, 7, ang=20), p.dome(66, 70, 9, 7, ang=20), amber, rim=amber[5])
    p.fill(p.poly([70, 66, 76, 62, 78, 52, 74, 58, 72, 54, 71, 62]), "#ffe08a", 220)
    b = (52, 68, 9, 8)
    p.shade(p.ell(*b), p.dome(*b), brown)
    for x in (46, 52, 58):
        mm, nn = p.stroke([(x, 74), (x - 2, 86)], 0.9)
        p.fill(mm, brown[1])
    if st >= 2:
        p.fill(p.box(44.6, 80, 47.4, 82), "#e2584a")
    h = (40, 62, 10, 10)
    for k, tx in enumerate((28, 36)):
        ty = h[1] - 24 - st
        mm, nn = p.stroke(bez((h[0] - 2 + k * 3, h[1] - 8), (h[0] - 3 + k * 2, h[1] - 16), (tx + 4, h[1] - 22), (tx, ty), 12), 0.6)
        p.fill(mm, brown[1])
        p.fill(p.ell(tx, ty, 1.2, 1.2), amber[4])
    p.shade(p.ell(*h), p.dome(*h), brown, rim=amber[3])
    e = eyes(p, (h[0] - 4, h[1] - 0.5), (h[0] + 4, h[1] - 0.5), [3.4, 3.2, 3.0, 3.0][st])
    p.cheek(h[0] - 6, h[1] + 4, 2)
    p.mouth(h[0], h[1] + 6.5, 1.8)
    if st == 3:
        rng = np.random.default_rng(11)
        for k in range(8):
            a = rng.random() * 6.28
            x, y = 68 + np.cos(a) * (14 + rng.random() * 6), 66 + np.sin(a) * (12 + rng.random() * 5)
            p.fill(p.ell(x, y, 0.8, 0.8), "#fff2a0")
    return e


# ---------------------------------------------------------------- potpourri

def potpourri(p, st):
    cols = ["#e2584a", "#4cb4d8", "#f2d84c", "#6cc46a", "#b06ad8", "#f0a63a", "#e86aa8", "#6a7ae0"]
    b = (50, 68, 20, 18)
    shape = p.ell(*b)
    for side in (-1, 1):
        p.shade(p.ell(50 + side * 13, 52, 5.5, 6.5), p.dome(50 + side * 13, 52, 5.5, 6.5), Ramp.around(cols[3 if side < 0 else 5]))
    for x in (40, 60):
        p.shade(p.ell(x, 85, 5, 3), p.dome(x, 85, 5, 3), Ramp.around(cols[1]))
    n = [3, 4, 6, 8][st]
    ang = np.degrees(np.arctan2(p.V - b[1], p.U - b[0])) % 360
    for k in range(n):
        m = shape & (ang >= k * 360 / n) & (ang < (k + 1) * 360 / n)
        p.shade(m, p.dome(*b), Ramp.around(cols[k % 8]), outline=False)
        a = np.radians(k * 360 / n)
        for t in np.linspace(0.3, 0.9, 4):
            x, y = b[0] + np.cos(a) * b[2] * t, b[1] + np.sin(a) * b[3] * t
            p.fill(p.ell(x, y, 0.6, 0.6), "#fff0d8")
    from pix import edges
    r, l, bb, t = edges(shape)
    p.fill(r | l | bb | t, "#3a2a20")
    e = []
    for x in (43, 57):
        m = p.ell(x, 64, 3.6, 3.6)
        p.shade(m, p.dome(x, 64, 3.6, 3.6), EYE)
        p.fill(p.ell(x - 1, 63, 0.6, 0.6) | p.ell(x + 1, 63, 0.6, 0.6) | p.ell(x - 1, 65, 0.6, 0.6) | p.ell(x + 1, 65, 0.6, 0.6), EYE[3])
        ys, xs = np.nonzero(m)
        p.px.put(_px(xs.min() + 1, ys.min() + 1, 1) & m, WHITE)
        e.append((int(xs.min()), int(ys.min()), int(xs.max() + 1), int(ys.max() + 1)))
    p.fill(p.ell(50, 71, 2.6, 1.2) & ~p.ell(50, 70.2, 2.6, 1.2), "#3a2a20")
    p.cheek(39, 70, 2.4)
    p.cheek(61, 70, 2.4)
    if st == 3:
        for side in (-1, 1):
            p.shade(p.poly([60, 50, 60 + side * 6, 46, 60 + side * 6, 54]), p.face(side * 0.2, 0, 1), R.pink)
        p.shade(p.ell(60, 50, 1.6, 1.6), p.dome(60, 50, 1.6, 1.6), R.pink)
    return e


# ---------------------------------------------------------------- eggs

EGG = {
    "lyrebird": ("#c49a72", "feather"), "musicbox": ("#a8683e", "box"), "longear": ("#7e62a0", "cups"),
    "basilisk": ("#40363a", "cracks"), "conductor": ("#e8e2d0", "tie"), "purr": ("#282744", "curl"),
    "lark": ("#f4c0a0", "dawn"), "parrot": ("#6cc858", "notes"), "axolotl": ("#f2a6bc", "dots"),
    "raccoon": ("#8a8692", "stripes"), "wanderer": ("#7c9a5a", "patch"), "cricket": ("#34388e", "stars"),
    "snail": ("#3a3644", "spiral"), "tortoise": ("#908a44", "cassette"), "whale": ("#6894ae", "waves"),
    "echo": ("#dcd4f0", "echo"), "firefly": ("#fcc248", "glow"), "potpourri": ("#ccaa80", "patches"),
}


def egg(p, sp):
    base, pat = EGG.get(sp, ("#c8b8a0", ""))
    ramp = Ramp.around(base)
    shape = p.ell(50, 66, 17, 20) | (p.ell(50, 58, 14.5, 26) & (p.V < 66))
    p.shade(shape, p.dome(50, 62, 17, 24), ramp, rim=ramp[6])
    if pat == "feather":
        m, nn = p.stroke(bez((44, 82), (40, 66), (50, 52), (58, 46)), 1.6, 0.8)
        p.fill(m & shape, GENRE[0])
    elif pat == "box":
        p.fill(shape & (np.abs(p.V - 62) < 1.2), R.brass[4])
        p.fill(p.ell(50, 66, 1.6, 2) & shape, R.velvet[1])
    elif pat == "cups":
        for x in (35, 65):
            p.shade(p.ell(x, 62, 4, 7) & shape, p.dome(x, 62, 4, 7), R.peach, outline=False)
    elif pat == "cracks":
        for pts in ([(40, 50), (45, 58), (42, 64), (48, 72)], [(60, 54), (56, 62), (61, 70)]):
            m, nn = p.stroke(pts, 0.7)
            p.fill(m & shape, R.ember[4])
            p.glow(pts[1][0], pts[1][1], 7, "#fa9e40", 70, only=shape)
    elif pat == "tie":
        for side in (-1, 1):
            p.fill(p.poly([50, 64, 50 + side * 6, 61, 50 + side * 6, 67]) & shape, R.black[3])
    elif pat == "curl":
        m, nn = p.stroke(bez((40, 78), (60, 80), (62, 62), (52, 58)), 1.2)
        p.fill(m & shape, R.brass[4])
    elif pat == "dawn":
        for i, c in enumerate(("#fcd27a", "#f4a88e", "#dc7a7a")):
            p.fill(shape & (p.V > 70 + i * 4) & (p.V < 72 + i * 4), c, 200)
    elif pat == "dots":
        for x, y, c in ((42, 56, "#7af4ff"), (58, 60, "#ff8ad8"), (46, 72, "#7af4ff"), (57, 74, "#ff8ad8")):
            p.glow(x, y, 4, c, 90, only=shape)
            p.fill(p.ell(x, y, 1, 1) & shape, c)
    elif pat == "stripes":
        for y in (56, 64, 72, 80):
            p.fill(shape & (np.abs(p.V - y) < 1.6), "#3a3640")
    elif pat == "patch":
        p.fill(p.box(44, 62, 54, 72) & shape, "#c0503c")
        p.fill(p.box(44, 62, 54, 72) & shape & ((np.abs(p.U - 44.3) < .4) | (np.abs(p.V - 62.3) < .4)), "#f6e8d0")
    elif pat == "stars":
        for x, y in ((42, 52), (56, 60), (48, 72), (60, 76), (40, 66)):
            p.fill(p.ell(x, y, 0.8, 0.8) & shape, "#fff2a0")
    elif pat == "spiral":
        d = np.sqrt((p.U - 50) ** 2 + (p.V - 64) ** 2)
        for rr in (5, 8, 11, 14):
            p.fill(shape & (np.abs(d - rr) < 0.4), "#726e7c")
        p.fill(p.ell(50, 64, 3, 3), GENRE[0])
    elif pat == "cassette":
        p.fill(p.box(42, 60, 58, 68) & shape, R.ivory[3])
        p.fill(p.box(44.5, 62.5, 55.5, 65.5), "#3a3040")
    elif pat == "waves":
        for k in range(3):
            p.fill(shape & (np.abs(p.V - (60 + k * 6 + np.sin(p.U * 0.8) * 1.2)) < 0.5), "#c8dce6")
    elif pat == "echo":
        d = np.sqrt((p.U - 50) ** 2 + (p.V - 64) ** 2)
        for rr in (9, 13):
            p.fill(shape & (np.abs(d - rr) < 0.45), "#9284b6")
    elif pat == "glow":
        p.glow(50, 70, 12, "#fff6cc", 120, only=shape)
    elif pat == "patches":
        ang = np.degrees(np.arctan2(p.V - 64, p.U - 50)) % 360
        cols = ["#e2584a", "#4cb4d8", "#f2d84c", "#6cc46a", "#b06ad8"]
        for k in range(5):
            p.fill(shape & (ang >= k * 72) & (ang < (k + 1) * 72), cols[k], 150)
    elif pat == "notes":
        for x, y in ((42, 56), (56, 66), (46, 74)):
            p.fill(p.ell(x, y, 1.6, 1.3) & shape, "#fff2a0")
            p.fill(p.box(x + 1, y - 5, x + 1.9, y) & shape, "#fff2a0")
    # a shine
    p.fill(p.ell(42, 50, 2.2, 4, ang=20) & shape, WHITE, 170)
    return []


SPECIES = {
    "lyrebird": lyrebird, "musicbox": musicbox, "longear": longear, "basilisk": basilisk,
    "conductor": conductor, "purr": purr, "axolotl": axolotl, "raccoon": raccoon, "wanderer": wanderer,
    "cricket": cricket, "lark": lark, "snail": snail, "parrot": parrot, "tortoise": tortoise,
    "whale": whale, "echo": echo, "firefly": firefly, "potpourri": potpourri,
}
FLOATS = {"whale", "echo", "firefly"}


def paint(sp, stage):
    px = Px()
    p = Pen(px, SCALE[stage], dy=-2 if sp in FLOATS and stage else 0)
    e = egg(p, sp) if stage == 0 else SPECIES[sp](p, stage - 1)
    px.ring(OUT)
    return px, [b for b in e if b]


def preview(rows, path, scale=4):
    cols = len(STAGES)
    cell = N * scale
    sheet = Image.new("RGBA", (cell * cols, cell * len(rows)), (42, 34, 54, 255))
    d = ImageDraw.Draw(sheet)
    for j, (sp, imgs) in enumerate(rows):
        for i, im in enumerate(imgs):
            x, y = i * cell, j * cell
            if (i + j) % 2:
                d.rectangle([x, y, x + cell, y + cell], fill=(48, 40, 62, 255))
            sh = SCALE[i] * 24 * scale
            gy = y + (N - 3.5) * scale
            d.ellipse([x + cell / 2 - sh, gy - 2.2 * scale, x + cell / 2 + sh, gy + 2.2 * scale], fill=(26, 18, 34, 255))
            sheet.alpha_composite(im.resize((cell, cell), Image.NEAREST), (x, y))
    sheet.save(path)


def main(names):
    os.makedirs(OUTDIR, exist_ok=True)
    rows, faces = [], []
    for sp in names:
        imgs = []
        for st in range(len(STAGES)):
            px, e = paint(sp, st)
            im = px.image()
            im.save(os.path.join(OUTDIR, f"{sp}_{STAGES[st]}.png"))
            imgs.append(im)
            if e:
                faces.append(f"{sp}_{STAGES[st]} " + " ".join(" ".join(map(str, b)) for b in e))
        rows.append((sp, imgs))
    with open(os.path.join(OUTDIR, "eyes.txt"), "w") as f:
        f.write("\n".join(faces) + "\n")
    preview(rows, os.path.join(ROOT, "art", "preview.png"))
    print("ok", len(rows))


if __name__ == "__main__":
    main(sys.argv[1:] or list(SPECIES))
