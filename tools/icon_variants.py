"""Draws launcher icon ideas without pets side by side: art/icon_variants.png."""
import os

from PIL import Image, ImageDraw, ImageFont

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
S = 384
OAT = (243, 234, 219)
TERRA = (200, 85, 61)
VINYL = (38, 33, 32)
GROOVE = (60, 53, 51)
BRASS = (226, 176, 92)


def rounded(im, r=84):
    m = Image.new("L", im.size, 0)
    ImageDraw.Draw(m).rounded_rectangle([0, 0, im.width - 1, im.height - 1], radius=r, fill=255)
    im.putalpha(m)
    return im


def record(d, cx, cy, r, label, hole=OAT):
    d.ellipse([cx - r, cy - r, cx + r, cy + r], fill=VINYL)
    for k in range(1, 7):
        rr = r * (0.48 + k * 0.075)
        d.ellipse([cx - rr, cy - rr, cx + rr, cy + rr], outline=GROOVE, width=3)
    d.arc([cx - r * .84, cy - r * .84, cx + r * .84, cy + r * .84], 205, 240, fill=(255, 255, 255, 46), width=int(r * .1))
    lr = r * 0.36
    d.ellipse([cx - lr, cy - lr, cx + lr, cy + lr], fill=label)
    hr = r * 0.05
    d.ellipse([cx - hr, cy - hr, cx + hr, cy + hr], fill=hole)


def note(d, cx, cy, h, col):
    """An eighth note, drawn on a pixel grid of step h/8."""
    p = h / 8
    rows = ["...XXX..", "...X.XX.", "...X..X.", "...X....", "...X....", ".XXX....", "XXXX....", ".XX....."]
    for j, row in enumerate(rows):
        for i, ch in enumerate(row):
            if ch == "X":
                x0 = cx - 4 * p + i * p; y0 = cy - 4 * p + j * p
                d.rectangle([x0, y0, x0 + p - 1, y0 + p - 1], fill=col)


def v1():
    """A record on terracotta, a pixel note on the label."""
    im = Image.new("RGBA", (S, S), TERRA + (255,))
    d = ImageDraw.Draw(im, "RGBA")
    record(d, S / 2, S / 2, S * .38, OAT, TERRA)
    note(d, S / 2 + S * .02, S / 2, S * .2, TERRA)
    return rounded(im)


def v2():
    """A record on oatmeal with a brass tonearm resting on it."""
    im = Image.new("RGBA", (S, S), OAT + (255,))
    d = ImageDraw.Draw(im, "RGBA")
    record(d, S * .45, S * .55, S * .34, TERRA)
    d.line([(S * .84, S * .14), (S * .8, S * .5), (S * .6, S * .66)], fill=BRASS, width=14, joint="curve")
    d.ellipse([S * .79, S * .09, S * .89, S * .19], fill=(90, 80, 74))
    d.rounded_rectangle([S * .54, S * .62, S * .64, S * .7], radius=4, fill=(90, 80, 74))
    return rounded(im)


def v3():
    """A cassette in the app's colours."""
    im = Image.new("RGBA", (S, S), OAT + (255,))
    d = ImageDraw.Draw(im, "RGBA")
    x0, y0, x1, y1 = S * .12, S * .24, S * .88, S * .76
    d.rounded_rectangle([x0, y0, x1, y1], radius=22, fill=TERRA)
    d.rounded_rectangle([x0 + 24, y0 + 22, x1 - 24, y0 + 120], radius=10, fill=(255, 248, 238))
    for k in range(2):
        d.line([(x0 + 44, y0 + 52 + k * 26), (x1 - 44, y0 + 52 + k * 26)], fill=(185, 199, 214), width=3)
    d.rounded_rectangle([S * .3, y0 + 132, S * .7, y0 + 176], radius=20, fill=VINYL)
    for cx in (S * .38, S * .62):
        d.ellipse([cx - 16, y0 + 138, cx + 16, y0 + 170], fill=OAT)
        d.ellipse([cx - 6, y0 + 148, cx + 6, y0 + 160], fill=VINYL)
    d.polygon([(S * .3, y1), (S * .36, y1 - 26), (S * .64, y1 - 26), (S * .7, y1)], fill=(160, 64, 46))
    return rounded(im)


def v4():
    """A sleeve with the record half out, nothing else."""
    im = Image.new("RGBA", (S, S), OAT + (255,))
    d = ImageDraw.Draw(im, "RGBA")
    record(d, S * .6, S * .5, S * .3, (110, 155, 106))
    d.rounded_rectangle([S * .12, S * .2, S * .12 + S * .6, S * .2 + S * .6], radius=12, fill=TERRA)
    d.rounded_rectangle([S * .2, S * .28, S * .2 + S * .44, S * .28 + S * .44], radius=8, outline=(243, 234, 219, 150), width=5)
    note(d, S * .42, S * .5, S * .22, OAT)
    return rounded(im)


def main():
    vs = [v1(), v2(), v3(), v4()]
    pad = 30
    out = Image.new("RGBA", (len(vs) * (S + pad) + pad, S + pad * 2 + 40), (52, 46, 44, 255))
    d = ImageDraw.Draw(out)
    try:
        f = ImageFont.truetype("arial.ttf", 30)
    except OSError:
        f = None
    for i, v in enumerate(vs):
        x = pad + i * (S + pad)
        out.alpha_composite(v, (x, pad))
        d.text((x + S // 2 - 8, S + pad + 6), str(i + 1), fill=(240, 230, 210), font=f)
    out.save(os.path.join(ROOT, "art", "icon_variants.png"))
    print("ok")


if __name__ == "__main__":
    main()
