"""Draws the sprite sheets and eggs of the pets added after the raven with the Codex CLI image tool, in the
style of the other sheets (tools/gen_raven.py did the same for the raven alone).

    python tools/gen_pets.py [species ...]

Writes art/sheets/<species>.png (four stages in a row, for tools/cut.py) and art/eggs/sheets/<species>_egg.png
(one egg on white, for tools/eggs.py --one). Sheets already there are skipped. Then:

    python tools/cut.py hummingbird magpie bullfinch bat dragon hound
    python tools/eggs.py --one

A species shows up in the app once its egg and its baby are in resources/pets (Species.drawn).
"""
import shutil
import subprocess
import sys
from pathlib import Path

from gen_raven import STYLE, image_of

ROOT = Path(__file__).resolve().parent.parent

EGG_STYLE = """A single cute pixel-art egg for a cozy pet game, in a chibi 16-bit style, centred on a plain pure white background, nothing else.
Chunky pixel art with a crisp thick dark outline, soft cel shading with 3–4 tones per colour.
{egg}
Upright egg, seen from the front, about two thirds of the picture's height. No text, no shadow, no frame."""

# species -> (the creature through its four stages, its egg). The stages match Words.growth().
PETS = {
    "hummingbird": (
        """A tiny cheerful hummingbird who loves pop music – a little pop star. Bright, sweet and sparkly.
Baby: a tiny round pink-and-teal fluffball chick with a short beak and one glittering feather. Young: a small hummingbird hovering, its long beak ending in a tiny round microphone head, a few glitter specks on its wings. Adult: a bright hummingbird with wings covered in glitter and little stars, a microphone beak, a small heart-shaped pendant. Ancient: a dazzling hummingbird with a small golden crown topped with a star, long shimmering tail ribbons, sparkles and tiny music notes around its wings.
Iridescent pink, teal and violet feathers, gold accents, white sparkles.""",
        "The egg of a little pop-star hummingbird: glossy pink shell fading to teal, sprinkled with tiny white stars and glitter, with a small golden heart."),
    "magpie": (
        """A clever friendly magpie who loves duets and songs sung by two artists together – a collector of shiny things.
Baby: a small round black-and-white chick with a stubby tail and one shiny button held in its beak. Young: a small magpie whose long tail ends in two feathers shaped like a pair of joined eighth notes, a shiny badge on its chest. Adult: a sleek magpie with a long two-note tail, several small shiny badges and pins on its wing, a blue-green sheen on the feathers. Ancient: an elegant magpie wearing a necklace of colourful guitar plectrums, a monocle-like shiny ring over one eye, the two-note tail long and glossy.
Black and white feathers with a blue-green iridescent sheen, silver and gold shiny trinkets.""",
        "The egg of a little magpie: a shell split diagonally black and white with a blue-green sheen, a few shiny silver dots and two tiny joined music notes."),
    "bullfinch": (
        """A plump cosy bullfinch who loves winter and Christmas songs. Warm and festive.
Baby: a tiny round grey chick with a faint rosy chest, a snowflake resting on its head. Young: a small bullfinch with a red chest, wearing a little knitted bobble hat. Adult: a round bullfinch with a bright red chest, a knitted bobble hat, and a small golden jingle bell on a ribbon round its neck. Ancient: a stately bullfinch with a long striped knitted scarf, the bobble hat, the golden bell, and white snowflake patterns on its dark wings.
Red chest, soft grey back, black cap and wings, white snowflakes, a gold bell, a cream and red knit.""",
        "The egg of a little bullfinch: soft grey-blue shell with a red knitted band round the middle, white snowflakes and a tiny golden bell."),
    "bat": (
        """A cute friendly little bat who loves the radio and vinyl records – a tiny radio operator. Sweet, not scary.
Baby: a tiny round brown fluffball bat with small folded wings and big ears. Young: a small bat whose big ears are tipped with little radio antenna balls, tiny curved signal waves beside one ear. Adult: a bat with antenna ears and open wings whose membranes look like an old radio dial with tick marks and a red tuning needle, a small speaker grille pattern on its chest. Ancient: an old bat with a glowing warm orange radio valve tube pendant, antenna ears, radio-dial wings, and a tiny vinyl record held in its feet.
Warm brown fur, cream belly, amber and orange glow, brass details.""",
        "The egg of a little bat: warm brown shell with a cream radio-dial band of tick marks, a small red tuning needle and two tiny antenna dots on top."),
    "dragon": (
        """A small friendly dragon who loves very long songs and epics. Gentle and proud, not fierce.
Baby: a tiny round green dragon hatchling with stubby wings and a short tail. Young: a small dragon with a row of black and white piano keys running along its back like a crest. Adult: a dragon with a piano-key crest and a long tail that unrolls like a parchment scroll covered in music notes. Ancient: a grand dragon whose wings are made of golden organ pipes, the piano-key crest, the long scroll tail, small round spectacles.
Emerald green scales, cream belly, gold organ pipes, ivory and black keys, parchment tail.""",
        "The egg of a little dragon: emerald green scaly shell with a band of tiny black and white piano keys and a few golden music notes."),
    "hound": (
        """A loyal gentle dog who loves one favourite artist above all and waits for their songs. Warm and devoted.
Baby: a tiny round floppy-eared puppy with a small red collar. Young: a puppy with a red collar whose tag is a tiny vinyl record. Adult: a dog whose big floppy ears look like over-ear headphones, a red collar with a vinyl record tag, a happy wagging tail. Ancient: an old dog with a grey muzzle, the headphone ears, the record tag, and a golden medal with a music note on a blue ribbon.
Warm caramel and cream fur, a red collar, a black vinyl tag with a gold label, a gold medal.""",
        "The egg of a loyal little dog: warm caramel shell with cream patches, a small red collar band round it and a tiny vinyl record tag."),
}


def jobs(names):
    out = {}
    for sp in names:
        body, egg = PETS[sp]
        out[ROOT / "art" / "sheets" / f"{sp}.png"] = \
            "Use your image generation tool to create ONE image. Do nothing else (no file copying, no code).\n\n" + STYLE + "\n\n" + body + "\nWide landscape image."
        out[ROOT / "art" / "eggs" / "sheets" / f"{sp}_egg.png"] = \
            "Use your image generation tool to create ONE image. Do nothing else (no file copying, no code).\n\n" + EGG_STYLE.format(egg=egg) + "\nSquare image."
    return out


def main():
    names = [a for a in sys.argv[1:] if a in PETS] or list(PETS)
    for dst, prompt in jobs(names).items():
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
