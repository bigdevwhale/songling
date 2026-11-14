"""Draws the award icon sheets that aren't there yet (art/icons/sheets/<sheet>.png) with the Codex CLI image tool,
from the prompts tools/icon_prompts.py writes, with the den and a pet attached as style references.

    python tools/gen_feats.py [sheet ...]

Sheets already there are skipped. Then `python tools/icons_cut.py` cuts them into resources/feats.
"""
import shutil
import subprocess
import sys
from pathlib import Path

from gen_raven import image_of

ROOT = Path(__file__).resolve().parent.parent
PROMPTS = ROOT / "art" / "icons" / "prompts"
SHEETS = ROOT / "art" / "icons" / "sheets"
REFS = [ROOT / "art" / "den" / "ref_den.png", ROOT / "art" / "sheets" / "purr.png"]


def prompt(sheet):
    text = (PROMPTS / f"{sheet}.txt").read_text(encoding="utf-8")
    # The image is fetched from Codex's own folder: the agent only draws it.
    text = text.replace("Use your image generation tool to create ONE image, then save it as PNG with transparency to the path given at the end (create folders if needed).",
                        "Use your image generation tool to create ONE image. Do nothing else (no file copying, no code).")
    return text.split("\n\nSave the image to:")[0]


def main():
    names = sys.argv[1:] or sorted(p.stem for p in PROMPTS.glob("feats*.txt") if not (SHEETS / f"{p.stem}.png").exists() and p.stem != "ui")
    for sheet in names:
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
