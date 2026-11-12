"""
Cuts the icon sheets (art/icons/sheets, drawn with ChatGPT image generation through Codex from the prompts
tools/icon_prompts.py writes; the menu's sheet from art/icons/prompts/ui.txt) into icons: the menu and the den's
buttons as plain 72x72 masks into res/drawable-nodpi (black on white turned into white on clear, so the app
colours them), one 48x48 drawn icon per achievement into resources/feats.

    python tools/icons_cut.py

Also writes a look at all of them, 3x, to app/build/art/icons.png.
"""
import os
import sys

import numpy as np
from PIL import Image
from scipy import ndimage

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from cut import load, crisp  # noqa: E402
from icon_prompts import sheets  # noqa: E402

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, "art", "icons", "sheets")
RES = os.path.join(ROOT, "app", "src", "main", "res", "drawable-nodpi")
FEATS = os.path.join(ROOT, "app", "src", "main", "resources", "feats")
LOOK = os.path.join(ROOT, "app", "build", "art", "icons.png")
SIZE = 48
PAD = 1


def boxes(solid, n):
    """The n icons' boxes, row by row, left to right. Loose bits (notes, sparks) join the icon they float by."""
    h, w = solid.shape
    near = ndimage.binary_dilation(solid, iterations=max(6, w // 90))
    lab, k = ndimage.label(near)
    found = []
    for i, sl in enumerate(ndimage.find_objects(lab), start=1):
        m = (lab[sl] == i) & solid[sl]
        if m.sum() < solid.sum() * 0.004:
            continue
        ys, xs = np.nonzero(m)
        found.append((sl[1].start + xs.min(), sl[0].start + ys.min(), sl[1].start + xs.max() + 1, sl[0].start + ys.max() + 1, m.sum()))
    if len(found) != n:
        # Fall back to the grid: what is in each third.
        found = []
        for j in range(3):
            for i in range(3):
                if len(found) == n:
                    break
                y0, y1, x0, x1 = h * j // 3, h * (j + 1) // 3, w * i // 3, w * (i + 1) // 3
                ys, xs = np.nonzero(solid[y0:y1, x0:x1])
                found.append((x0 + xs.min(), y0 + ys.min(), x0 + xs.max() + 1, y0 + ys.max() + 1, len(xs)))
        print("  (cut by the grid)")
    rows = sorted(found, key=lambda b: (b[1] + b[3]) / 2)
    out, line = [], []
    for b in rows:
        if line and (b[1] + b[3]) / 2 - (line[0][1] + line[0][3]) / 2 > h / 6:
            out += sorted(line, key=lambda c: c[0])
            line = []
        line.append(b)
    out += sorted(line, key=lambda c: c[0])
    return [b[:4] for b in out]


def icon(im, solid, box):
    x0, y0, x1, y1 = box
    part = im[y0:y1, x0:x1].copy()
    part[..., 3] = np.where(solid[y0:y1, x0:x1], part[..., 3], 0)
    room = SIZE - 2 * PAD
    k = room / max(x1 - x0, y1 - y0)
    w, h = max(1, round((x1 - x0) * k)), max(1, round((y1 - y0) * k))
    arr = crisp(part, w, h, colors=48)
    out = np.zeros((SIZE, SIZE, 4), np.uint8)
    ox, oy = (SIZE - w) // 2, (SIZE - h) // 2
    out[oy:oy + h, ox:ox + w] = arr
    return out


MASK = 72


def masks(path, items):
    """The minimal menu icons: dark glyphs on white become white with the darkness as alpha, softly shrunk."""
    g = np.array(Image.open(path).convert("L")).astype(float)
    ink = np.clip((235 - g) / 200, 0, 1)
    solid = ink > 0.5
    out = []
    for (name, _), (x0, y0, x1, y1) in zip(items, boxes(solid, len(items))):
        side = max(x1 - x0, y1 - y0)
        pad = side // 14
        cx, cy = (x0 + x1) // 2, (y0 + y1) // 2
        half = side // 2 + pad
        crop = np.zeros((2 * half, 2 * half))
        sy0, sx0 = max(0, cy - half), max(0, cx - half)
        part = ink[sy0:cy + half, sx0:cx + half]
        crop[sy0 - (cy - half):sy0 - (cy - half) + part.shape[0], sx0 - (cx - half):sx0 - (cx - half) + part.shape[1]] = part
        a = np.array(Image.fromarray((crop * 255).astype(np.uint8)).resize((MASK, MASK), Image.LANCZOS))
        img = np.zeros((MASK, MASK, 4), np.uint8)
        img[..., :3] = 255
        img[..., 3] = a
        Image.fromarray(img, "RGBA").save(os.path.join(RES, name + ".png"))
        out.append(img)
    return out


def main():
    os.makedirs(FEATS, exist_ok=True)
    os.makedirs(os.path.dirname(LOOK), exist_ok=True)
    made = []
    for sheet, items in sheets().items():
        path = os.path.join(SRC, sheet + ".png")
        if not os.path.exists(path):
            print("no sheet:", sheet)
            continue
        print(sheet)
        if sheet == "ui":
            for m in masks(path, items):
                dark = m.copy()
                dark[..., :3] = 60
                made.append(np.array(Image.fromarray(dark, "RGBA").resize((SIZE, SIZE), Image.LANCZOS)))
            continue
        im, solid = load(path)
        for (name, _), box in zip(items, boxes(solid, len(items))):
            arr = icon(im, solid, box)
            dest = os.path.join(RES, name + ".png") if sheet == "ui" else os.path.join(FEATS, name + ".png")
            Image.fromarray(arr, "RGBA").save(dest)
            made.append(arr)
    cols = 9
    look = Image.new("RGBA", (cols * SIZE * 3, -(-len(made) // cols) * SIZE * 3), (58, 44, 40, 255))
    for i, a in enumerate(made):
        look.alpha_composite(Image.fromarray(a, "RGBA").resize((SIZE * 3, SIZE * 3), Image.NEAREST), ((i % cols) * SIZE * 3, (i // cols) * SIZE * 3))
    look.save(LOOK)
    print("ok", len(made))


if __name__ == "__main__":
    main()
