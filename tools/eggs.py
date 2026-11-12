"""
Cuts the eggs, redrawn finer with ChatGPT image generation through Codex (art/eggs/sheets/eggs1.png and eggs2.png,
nine to a sheet in the order of SPECIES, on white; prompts in art/eggs/prompts, made from the painted eggs in
art/eggs/ref*.png), into the 160x160 egg sprites, each where and as big as the painted egg it replaces.

    python tools/eggs.py
"""
import os
import sys

import numpy as np
from PIL import Image
from scipy import ndimage

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from cut import SPECIES, crisp, outline, FINE  # noqa: E402
from icons_cut import boxes  # noqa: E402

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, "art", "eggs", "sheets")
OUT = os.path.join(ROOT, "app", "src", "main", "resources", "pets")
SIZE = 80 * FINE

# Where the painted eggs stood in their 80x80 pictures: x0, y0, x1, y1 (tools/pets.py drew them all alike).
PAINTED = (27, 37, 53, 77)


def load_white(path):
    im = np.array(Image.open(path).convert("RGBA"))
    rgb = im[..., :3].astype(int)
    # Everything but the white page, with the light spots inside an egg kept.
    solid = ndimage.binary_fill_holes(rgb.min(axis=2) < 232)
    lab, n = ndimage.label(solid)
    sizes = ndimage.sum(solid, lab, range(1, n + 1))
    for i, s in enumerate(sizes, start=1):
        if s < 400:
            solid[lab == i] = False
    im[..., 3] = np.where(solid, 255, 0)
    return im, solid


def main():
    x0, y0, x1, y1 = (v * FINE for v in PAINTED)
    w, h = x1 - x0, y1 - y0
    for n in range(2):
        im, solid = load_white(os.path.join(SRC, f"eggs{n + 1}.png"))
        for sp, (a, b, c, d) in zip(SPECIES[n * 9:n * 9 + 9], boxes(solid, 9)):
            part = im[b:d, a:c].copy()
            k = min(w / (c - a), h / (d - b))
            sw, sh = round((c - a) * k), round((d - b) * k)
            sprite = np.zeros((SIZE, SIZE, 4), np.uint8)
            ox, oy = x0 + (w - sw) // 2, y1 - sh
            sprite[oy:oy + sh, ox:ox + sw] = crisp(part, sw, sh)
            outline(sprite)
            Image.fromarray(sprite, "RGBA").save(os.path.join(OUT, f"{sp}_egg.png"))
            print(sp, sw, sh)


if __name__ == "__main__":
    main()
