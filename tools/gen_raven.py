"""Draws the raven's sprite sheet and egg with the Codex CLI image tool, in the style of the other sheets.

    python tools/gen_raven.py

Writes art/sheets/raven.png (four stages in a row, for tools/cut.py) and art/eggs/sheets/raven_egg.png
(one egg on white, for tools/eggs.py's single-egg mode). Sheets already there are skipped.
"""
import re
import shutil
import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
GEN = Path.home() / ".codex" / "generated_images"

STYLE = """Pixel art sprite sheet for a cute cozy pet game, in a chibi 16-bit style.
One creature shown growing up in FOUR stages in a single horizontal row, left to right: baby, young, adult, ancient.
Each stage is clearly bigger and more detailed than the one before; all four stand on the same baseline, evenly spaced, with empty space between them – they must not touch or overlap.
Chunky pixel art with a crisp thick dark outline, soft cel shading with 3–4 tones per colour, rich saturated colours.
Big round glossy dark eyes, each with a bright white highlight; pink blush cheeks; a happy open-mouth smile.
Full body, facing the viewer in a slight three-quarter view, sitting or standing.
Transparent background. No ground shadow, no text, no frames, no grid, nothing else in the picture.
The creature loves music: small musical details are part of its body (not separate objects), and they grow richer with each stage."""

RAVEN = """A cute, gentle little raven – a keeper of old songs who remembers musicians who are gone. Kind and cosy, not scary or gloomy.
Baby: a tiny round black fluffball chick with a stubby beak and one silver feather. Young: a small raven with a little knitted grey scarf and a tiny brass key on a string. Adult: a proud raven with a long dark coat-like wing cape, a small lantern-shaped pendant glowing warm gold, and a vinyl record pattern on its chest feathers. Ancient: silver-tipped feathers, small round spectacles, a tiny top hat with a white flower on it, the warm lantern glow brighter and a few little golden music notes resting on its wings.
Glossy blue-black feathers with violet sheen, warm gold accents, a cream-white flower."""

EGG = """A single cute pixel-art egg for a cozy pet game, in a chibi 16-bit style, centred on a plain pure white background, nothing else.
Chunky pixel art with a crisp thick dark outline, soft cel shading with 3–4 tones per colour.
The egg of a little raven: glossy blue-black shell with a violet sheen, a few silver speckles shaped like tiny feathers, and a thin warm gold band with a small music note.
Upright egg, seen from the front, about two thirds of the picture's height. No text, no shadow, no frame."""

JOBS = {
    ROOT / "art" / "sheets" / "raven.png": "Use your image generation tool to create ONE image. Do nothing else (no file copying, no code).\n\n" + STYLE + "\n\n" + RAVEN + "\nWide landscape image.",
    ROOT / "art" / "eggs" / "sheets" / "raven_egg.png": "Use your image generation tool to create ONE image. Do nothing else (no file copying, no code).\n\n" + EGG + "\nSquare image.",
}


def image_of(output):
    m = re.search(r"session id:\s*([0-9a-f-]+)", output)
    if not m:
        return None
    pngs = sorted((GEN / m.group(1)).glob("*.png"), key=lambda p: p.stat().st_mtime)
    return pngs[-1] if pngs else None


def main():
    for dst, prompt in JOBS.items():
        if dst.exists():
            print("skip", dst.name)
            continue
        dst.parent.mkdir(parents=True, exist_ok=True)
        r = subprocess.run([shutil.which("codex.cmd") or "codex", "exec", "--skip-git-repo-check", "--sandbox", "read-only", "-"],
                           input=prompt, text=True, encoding="utf-8", capture_output=True, cwd=str(ROOT))
        p = image_of(r.stdout + r.stderr)
        if p is None:
            print("FAIL", dst.name, (r.stdout + r.stderr)[-300:])
            continue
        shutil.copy(p, dst)
        print("ok", dst.name, p)


if __name__ == "__main__":
    main()
