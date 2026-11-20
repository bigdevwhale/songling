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
    "fox": (
        """A clever little fox who is a music critic – it listens closely and gives stars to albums. Witty and charming.
Baby: a tiny round orange fox kit with a fluffy white-tipped tail and a single small gold star stuck to its ear. Young: a small fox with round reading glasses and a tiny notepad tucked under its arm. Adult: a fox in a little beret with a row of five golden stars along its fluffy tail, round glasses, a pencil behind its ear. Ancient: a distinguished old fox with a silver-tipped muzzle, a velvet scarf, a beret, and a tiny golden award trophy shaped like a gramophone standing on the ground beside it (not held). Exactly two front paws and two hind paws, like the other stages – no extra limbs.
Bright orange and cream fur, black paws, gold stars, a burgundy beret.""",
        "The egg of a little critic fox: warm orange shell with a cream tip and five small golden stars in a row round the middle."),
    "penguin": (
        """A cheerful little penguin who keeps a list of music to hear and loves ticking things off it. Neat and determined.
Baby: a tiny round grey fluffy penguin chick. Young: a small black-and-white penguin with a little red bow tie. Adult: the same penguin, bigger, with the red bow tie and a small square note card with one green tick held in one flipper. Ancient: an old penguin with white bushy eyebrows, small round spectacles, the red bow tie and the note card.
Black and white feathers, a yellow-orange beak and feet, a red bow tie, a cream note card.
Keep it simple and clean, like a classic cosy pet sheet: the same calm standing pose in all four stages, a small palette, one new accessory per stage and nothing else – no scrolls, bags, records, music symbols or clutter.""",
        "The egg of a little penguin: glossy black-and-white shell like a tuxedo with a tiny red bow tie and a small paper checklist with a green tick."),
    "moth": (
        """A soft gentle moth who only comes out under the full moon to dance to music. Dreamy and cosy, not creepy.
Baby: a tiny round fuzzy cream moth with two small feathery antennae and a little crescent moon mark on its forehead. Young: a small fluffy moth with pale wings marked with moon phases (crescent, half, full). Adult: a fluffy moth with big pale lavender wings patterned like a night sky with stars and a full moon on each wing, a fuzzy collar, feathery antennae. Ancient: a luminous moth whose wings glow softly silver, a little full moon floating above its head like a halo, stardust and tiny music notes falling from its wings.
Cream and pale lavender fuzz, silver-blue moonlight, soft gold stars, navy night-sky patterns.""",
        "The egg of a little moon moth: pale lavender shell with a soft silver glow, a big full moon on its front and tiny stars around it."),
    "phoenix": (
        """A small bright phoenix who brings forgotten music back to life – it rises from old songs. Warm and joyful, not scary.
Baby: a tiny round red-orange fluffball chick with a single little flame-shaped crest feather, sitting on a bit of grey ash and an old cassette tape. Young: a small phoenix with a flickering flame crest and tail feathers curling like ribbons of cassette tape. Adult: a radiant phoenix with wings of red, orange and gold feathers like flames, a long fiery tail with tiny music notes in the sparks. Ancient: a majestic phoenix with golden flame wings spread wide, a small crown, and a vinyl-record disc behind its head.
The flames are drawn as solid pixel-art feathers with a crisp dark outline. Plain transparent background: no glow, haze, smoke or loose sparks around the figures.
Crimson, orange and gold feathers, yellow flames, warm sparks, a touch of grey ash.""",
        "The egg of a little phoenix: glowing red-orange shell with golden flame patterns and a few warm sparks, slightly cracked with light coming out."),
    "unicorn": (
        """A rare magical little unicorn who appears only for someone who has raised many pets. Sweet and dreamy.
Baby: a tiny round white foal with a short pastel rainbow mane and a little bud of a horn. Young: a small unicorn with a short pearly spiral horn and a rainbow mane. Adult: a graceful unicorn with a long flowing rainbow mane and tail, a classic pointed spiral horn (a single cone, not forked), little music-note marks on its flank. Ancient: a radiant unicorn with a long pointed golden spiral horn (a single cone, not forked), a mane of shimmering aurora colours, a small golden harp-shaped wing on its side.
Plain transparent background: no glow, haze, sparkles or floating notes around the figures.
White coat with a pearly sheen, pastel rainbow mane, gold horn and hooves.""",
        "The egg of a little unicorn: pearly white shell with a pastel rainbow swirl and a tiny golden spiral horn on top."),
    "squirrel": (
        """A quick lively red squirrel (a squirrel, not a chipmunk: no stripes on its face or back) who loves short, snappy songs under two minutes. Energetic and bouncy. Whenever it holds drumsticks, each paw grips one stick by its end, like a real drummer.
Baby: a tiny round red squirrel kit with a huge fluffy tail, holding a little acorn. Young: a small squirrel hugging an acorn-shaped little drum in both front paws (no drumsticks at this stage), a stopwatch pendant. Adult: a squirrel in mid-hop with a big fluffy tail with a lightning-bolt stripe, small drumsticks in its paws, a stopwatch on its chest. Ancient: an old squirrel with grey ear tufts, a racing scarf flying behind, drumsticks, and a golden acorn medal.
Warm red-brown fur, cream belly, a yellow lightning stripe, a silver stopwatch, a golden acorn.""",
        "The egg of a little squirrel: red-brown shell with a cream belly patch, a yellow lightning stripe and a tiny acorn on top."),
    "bee": (
        """A hardworking little bee who loves listening to music every single day without a break. Busy and happy.
Baby: a tiny round fuzzy yellow-and-black bee with small translucent wings. Young: a small bee holding a tiny calendar page where every day is ticked, a honey drop on its antenna. Adult: a round fuzzy bee with honeycomb-patterned wings, each hexagon cell holding a tiny music note, a little honey pot. Ancient: a queenly bee with a small golden crown, honeycomb wings glowing gold, a little wooden honey dipper held like a conductor's baton. A normal round bee body with a small stinger – no tail, no trail of cells behind it.
Plain transparent background with no glow, haze or vignette around the figures.
Bright yellow and soft black fur, golden honey, amber honeycomb, translucent wings.""",
        "The egg of a little bee: golden honeycomb-patterned shell with soft yellow and black stripes and a drop of honey."),
    "sloth": (
        """A slow, content sloth who loves coming back to the same album again and again over many days. Calm and cuddly.
Baby: a tiny round brown sloth baby hugging a small vinyl record like a teddy. Young: a small sloth hanging from a branch with one arm, a record hugged in the other, a sleepy smile. Adult: a sloth hanging from a branch with small headphones on, a well-worn vinyl record with a heart sticker in its claws. Ancient: an old sloth with a grey face, a cosy knitted cardigan, a little potted fern, and a stack of the same record worn shiny.
Soft warm brown and cream fur, dark eye patches, a green leafy branch, a black record with a pink heart label.""",
        "The egg of a little sloth: soft brown shell with cream patches, a small green leaf and a tiny vinyl record with a pink heart."),
    "chameleon": (
        """A playful chameleon whose colours change with the music of different decades. Curious and colourful.
Baby: a tiny round mint-green chameleon with a curly tail and big eyes. Young: the same chameleon, a little bigger, with one soft orange band on its curled tail. Adult: a mint-green chameleon whose curled tail has three soft bands (orange, rose, teal), small round sunglasses pushed up on its head. Ancient: an old chameleon with a pale wrinkled brow, the three tail bands, the sunglasses, and a tiny golden cassette pendant.
Mostly mint green with a cream belly; soft muted orange, rose and teal only on the tail.
Keep it simple and clean, like a classic cosy pet sheet: the same calm pose in all four stages, a small palette, one new detail per stage and nothing else – no neon, no patterns all over the body, no jukebox, no music symbols, no clutter.""",
        "The egg of a little chameleon: shell with bands of different retro colours – mint, orange, neon pink, teal – and a tiny cassette pattern."),
    "octopus": (
        """A friendly little octopus DJ who loves mixes where every song is by a different artist. Fun and party-loving.
Baby: a tiny round pink octopus with little curly tentacles and big eyes. Young: a small octopus holding a different tiny instrument in several tentacles: a maraca, a drumstick, a tiny guitar. Adult: an octopus wearing big DJ headphones, its eight tentacles each holding something musical (a record, a microphone, a cassette, a drumstick, a trumpet, a maraca, a tambourine, a tiny keyboard). Ancient: an old octopus with a captain's hat, tentacles spinning records on two turntables, little glowing suckers like disco lights.
Coral pink and violet skin, teal suckers, gold and silver instruments.""",
        "The egg of a little octopus: coral pink shell with violet spots, a teal band like a row of suckers round the middle and tiny music notes. Nothing sticks out beyond the egg shape."),
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
