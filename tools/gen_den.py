"""Draws the den sheets that aren't there yet (art/den/sheets/<sheet>.png) with the Codex CLI image tool, from
art/den/prompts/<sheet>.txt, with the den and a pet attached as style references.

    python tools/gen_den.py items_music

Then `python tools/den.py` cuts them into resources/den.
"""
import shutil
import subprocess
import sys
from pathlib import Path

from gen_raven import image_of

ROOT = Path(__file__).resolve().parent.parent
PROMPTS = ROOT / "art" / "den" / "prompts"
SHEETS = ROOT / "art" / "den" / "sheets"
REFS = [ROOT / "art" / "den" / "ref_den.png", ROOT / "art" / "sheets" / "purr.png"]


def prompt(sheet):
    text = (PROMPTS / f"{sheet}.txt").read_text(encoding="utf-8")
    # The image is fetched from Codex's own folder: the agent only draws it.
    text = text.replace("Use your image generation tool to create ONE image, then save it as PNG with transparency to the path given at the end (create folders if needed).",
                        "Use your image generation tool to create ONE image. Do nothing else (no file copying, no code).")
    return text.split("\n\nSave the image to:")[0]


def main():
    for sheet in sys.argv[1:]:
        dst = SHEETS / f"{sheet}.png"
        if dst.exists():
            print("skip", dst.name)
            continue
        args = [shutil.which("codex.cmd") or "codex", "exec", "--skip-git-repo-check", "--sandbox", "read-only"]
        for r in REFS:
            if r.exists():
                args += ["-i", str(r)]
        r = subprocess.run(args + ["-"], input=prompt(sheet), text=True, encoding="utf-8", capture_output=True, cwd=str(ROOT))
        p = image_of(r.stdout + r.stderr)
        if p is None:
            print("FAIL", dst.name, (r.stdout + r.stderr)[-300:])
            continue
        shutil.copy(p, dst)
        print("ok", dst.name, p)


if __name__ == "__main__":
    main()
