"""
Writes the prompts for the icon sheets drawn with ChatGPT image generation through Codex (art/icons/prompts),
the same way as the den's: each sheet is a 3x3 grid of icons, cut by tools/icons_cut.py.

    python tools/icon_prompts.py
"""
import os

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, "art", "icons", "prompts")

# The app's own buttons: the dock and the den's actions.
UI = [
    ("dock_den", "A cosy little wooden house with a warm lit round window and a golden music note curling up out of the chimney like smoke."),
    ("dock_shelf", "A small wooden shelf with three vinyl records in colourful sleeves standing on it and one audio cassette lying beside them."),
    ("dock_saga", "An open leather-bound storybook; its pages show a tiny bar chart of colourful bars on one page and music notes on the other."),
    ("dock_goals", "A little red pennant flag on a wooden pole with a golden star at the top; the flag has a white music note on it."),
    ("act_add", "A black vinyl record with a red label, and a big round amber plus badge on its lower right."),
    ("act_log", "A cream notepad with a few music staff lines and notes, a yellow pencil lying across it, and a green tick mark."),
    ("act_concert", "A vintage silver stage microphone on a short stand with warm spotlight beams and two tiny sparkles."),
    ("act_egg", "A big speckled pastel egg with a music note pattern on its shell, a small crack of light at the top."),
    ("act_lamp", "A retro table lamp with a mustard fabric shade glowing warmly, on a small round wooden base."),
]

# One per achievement (domain/Feats.kt), by name.
FEATS = [
    ("hours", "An hourglass whose falling sand is made of tiny golden music notes."),
    ("listens", "Big round over-ear headphones with a small heart floating above them."),
    ("days", "A tear-off desk calendar page with a music note instead of the date number."),
    ("streak", "A flame shaped like a treble clef, orange and yellow."),
    ("big_day", "A smiling sun wearing small headphones."),
    ("sitting", "A comfy armchair with a vinyl record resting on its seat."),
    ("marathon", "A running sneaker with motion lines and music notes trailing behind it."),
    ("veteran", "An old pocket watch with a music note on its face."),
    ("seasons", "A round wreath made of four parts: a snowflake, a spring flower, a sun, and an autumn leaf."),
    ("artists", "A retro microphone with a small speech bubble that holds a star."),
    ("tracks", "An open songbook with a ribbon bookmark and notes on its pages."),
    ("whole", "A vinyl record half out of its sleeve, with a golden ribbon tied around the sleeve."),
    ("discography", "A neat stack of five vinyl records seen from the side, each spine a different colour."),
    ("countries", "A small globe on a stand with music notes orbiting it."),
    ("decades", "A vintage radio receiver with a big round dial and a glowing tuning window."),
    ("genres", "A painter's palette whose paint blobs are shaped like music notes, in many colours."),
    ("omnivore", "A bento box with compartments holding a small guitar, a drum, a saxophone and a keyboard key."),
    ("collabs", "Two hands in a handshake with music notes around them."),
    ("fresh", "A green sprout growing out of the hole of a vinyl record."),
    ("vintage", "A gold-framed classic album cover hanging on a nail, showing a little silhouette of a band."),
    ("antique", "A brass phonograph with a big flower-shaped horn."),
    ("night", "An owl wearing headphones, sitting on a crescent moon."),
    ("morning", "A small songbird singing on a branch, with a rising sun behind it."),
    ("repeat", "A circular repeat arrow wrapped around a music note."),
    ("stuck", "A cute little green caterpillar curled into the shape of a music note, with tiny headphones."),
    ("album_again", "A vinyl record with a red heart-shaped label."),
    ("loyal", "A gold ring with a music note shaped gem."),
    ("no_skips", "An archery target with an arrow in the bullseye, and a small music note on the arrow's feathers."),
    ("headphones", "A pair of headphones around a small glowing planet, like a tiny private world."),
    ("epic", "A long unrolled parchment scroll covered in music notes."),
    ("comeback", "A wooden boomerang with music notes trailing behind it."),
    ("players", "A smartphone showing a play button, with a cassette and a vinyl record beside it."),
    ("storm", "A lightning bolt striking an electric guitar."),
    ("neon", "A glowing neon tube sign shaped like a music note, cyan and pink."),
    ("velvet", "A saxophone resting on a deep purple velvet cushion."),
    ("choir", "A crescent moon and stars with a small choir of three singing candles beneath."),
    ("street", "A skateboard with a boombox standing on it."),
    ("roots", "An acoustic guitar with a wheat stalk and leaves growing around its neck."),
    ("own_genre", "A round glass potion flask with swirling colourful liquid and music notes bubbling out."),
    ("new_year", "A small decorated Christmas tree with a golden star, baubles shaped like music notes."),
    ("midnight", "Two champagne glasses clinking, with a clock face showing midnight behind them."),
    ("live", "A small concert stage with red curtains and spotlights."),
    ("vinyl", "A vinyl record on a turntable platter with the tone arm resting on it."),
    ("radio", "A radio antenna tower sending out curved signal waves with music notes in them."),
    ("pets", "A nest with three colourful speckled eggs in it."),
    ("grown", "A friendly small dragon proudly standing, wearing headphones round its neck."),
    ("ancient", "A golden crown with a music note on its front jewel."),
    ("shiny", "A sparkling iridescent egg surrounded by twinkles."),
    ("cosy", "A small sofa with a cushion and a lit lava lamp beside it."),
    ("quests", "A clipboard with a checklist of three ticked boxes and a music note."),
    ("goals", "A chequered finish flag crossed with a golden trophy cup."),
]

HEAD = """Use your image generation tool to create ONE image, then save it as PNG with transparency to the path given at the end (create folders if needed). The attached pictures are style references from this game: a pet and the room it lives in. Match their style: detailed pixel art with fine pixels (like a 64x64 game icon, not chunky), crisp dark outline, soft cel shading with 3-4 tones per colour, rich saturated cosy colours, warm lamp-lit evening mood. Everything is about music.

A sheet of {n} separate game icons in a 3x3 grid, read left to right, row by row, in the order listed. Each icon sits in its own cell, centred, about the same size as the others, filling most of its cell, with wide empty space between icons: they must not touch or overlap. Each icon is a single compact object or a small tight group, drawn complete and alone, seen from the front.
Transparent background. No text, no letters, no numbers, no labels, no frames or circles around the icons, no grid lines, no ground shadow, nothing else in the picture.

"""


def write(name, items):
    body = "\n".join(f"{i + 1}. {d}" for i, (_, d) in enumerate(items))
    with open(os.path.join(OUT, name + ".txt"), "w", encoding="utf-8") as f:
        f.write(HEAD.format(n=len(items)) + body + f"\n\nSave the image to: art/icons/sheets/{name}.png\n")


def sheets():
    """Sheet name -> the icons on it, in order."""
    out = {"ui": UI}
    for i in range(0, len(FEATS), 9):
        out[f"feats{i // 9 + 1}"] = FEATS[i:i + 9]
    return out


if __name__ == "__main__":
    os.makedirs(OUT, exist_ok=True)
    for k, v in sheets().items():
        # The menu's icons are plain glyphs, not drawn pictures: their prompt (ui.txt) is written by hand.
        if k != "ui":
            write(k, v)
    print(", ".join(f"{k}:{len(v)}" for k, v in sheets().items()))
