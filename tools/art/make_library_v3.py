"""ME Library v3: no book stacks on top; shelves fill in 4 stages as books are stored;
lights (shelf LEDs, shelf crystal, tapestry emblem, top studs, top crystal) glow only
when the network is powered. Writes every texture variant plus a preview sheet."""
from PIL import Image, ImageDraw, ImageFont
import os, random
import make_library_v2 as v2

OUT = os.path.dirname(os.path.abspath(__file__))
P = v2.P
BOOK_CHARS = set("RrTtAFIiGYyK")
LIGHT_CHARS = set("Ccj")
SHELF_ROWS = [range(3, 8), range(9, 13)]   # upper and lower shelf interiors
COLS = range(2, 14)
OFF_MAP = {"C": "o", "c": "O", "j": "B"}       # lights switched off -> dark slate
EMBLEM_OFF = {"C": "i", "c": "i", "j": "I"}    # tapestry emblem: dark embroidery when off


def rows_of(img):
    inv = {v: k for k, v in P.items()}
    return ["".join(inv[img.getpixel((x, y))] for x in range(16)) for y in range(16)]


SHELF_ROWS_FULL = rows_of(v2.SHELF)
BANNER_ROWS = rows_of(v2.BANNER)
TOP_ROWS = rows_of(v2.TOP)

# Book columns per shelf, revealed in a fixed shuffled order so filling looks natural
book_cols = [(si, x) for si, rr in enumerate(SHELF_ROWS) for x in COLS
             if any(SHELF_ROWS_FULL[y][x] in BOOK_CHARS for y in rr)]
book_cols.sort()
SLOTS = 8
_groups = [[] for _ in range(SLOTS)]
for i, col in enumerate(book_cols):
    _groups[min(SLOTS - 1, i * SLOTS // len(book_cols))].append(col)


def remap(rows, mapping):
    return ["".join(mapping.get(ch, ch) for ch in r) for r in rows]


def shelf(fill, powered):
    """fill: number of books stored, 0-8; each stored book reveals its spot on the shelf."""
    keep = {c for g in _groups[:fill] for c in g}
    rows = [list(r) for r in SHELF_ROWS_FULL]
    for si, rr in enumerate(SHELF_ROWS):
        for x in COLS:
            if (si, x) not in keep:
                for y in rr:
                    if rows[y][x] in BOOK_CHARS:
                        rows[y][x] = "D"
    rows = ["".join(r) for r in rows]
    return v2.tex(rows if powered else remap(rows, OFF_MAP))


def tapestry(powered):
    return v2.tex(BANNER_ROWS if powered else remap(BANNER_ROWS, EMBLEM_OFF))


def top(powered):
    return v2.tex(TOP_ROWS if powered else remap(TOP_ROWS, OFF_MAP))


def lights_only(img):
    """Keep only glowing pixels (used as the emissive overlay layer)."""
    lit = {P[c] for c in LIGHT_CHARS}
    out = Image.new("RGBA", img.size)
    for y in range(img.height):
        for x in range(img.width):
            p = img.getpixel((x, y))
            if p in lit:
                out.putpixel((x, y), p)
    return out


def dim(img, f):
    out = img.copy()
    out.putdata([(int(r * f), int(g * f), int(b * f), a) for r, g, b, a in out.get_flattened_data()])
    return out


def model(fill, powered, emissive_only=False):
    faces = {"up": top(powered), "down": v2.BOTTOM, "south": tapestry(powered), "north": tapestry(powered),
             "east": shelf(fill, powered), "west": shelf(fill, powered)}
    boxes = []
    if emissive_only:
        faces = {k: lights_only(v) if powered else Image.new("RGBA", (16, 16)) for k, v in faces.items()}
    boxes.append(((0, 0, 0), (16, 16, 16), faces))
    return boxes


def night_render(fill, powered):
    """Scene at night: base model darkened, then glowing pixels added back at full brightness."""
    base = v2.render(model(fill, powered))
    base = dim(base, 0.42)
    if powered:
        real_shade = v2.shade
        v2.shade = lambda img, f: img           # lights ignore face shading
        glow = v2.render(model(fill, powered, emissive_only=True))
        v2.shade = real_shade
        base.alpha_composite(glow)
    return base


def main():
    import glob
    for old in glob.glob(os.path.join(OUT, "me_library_shelf_*.png")):
        os.remove(old)
    for f in range(SLOTS + 1):
        for pw in (True, False):
            shelf(f, pw).save(os.path.join(OUT, f"me_library_shelf_{f}{'' if pw else '_off'}.png"))
    for pw in (True, False):
        tapestry(pw).save(os.path.join(OUT, f"me_library_tapestry{'' if pw else '_off'}.png"))
        top(pw).save(os.path.join(OUT, f"me_library_top{'' if pw else '_off'}.png"))
    for old in ["me_library_shelf.png"]:
        p = os.path.join(OUT, old)
        if os.path.exists(p):
            os.remove(p)

    font = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf", 13)
    bold = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf", 15)
    sheet = Image.new("RGBA", (1250, 470), (226, 226, 222, 255))
    d = ImageDraw.Draw(sheet)
    t, gap = 76, 30

    def tile(img, x, y, label):
        sheet.alpha_composite(img.resize((t, t), Image.NEAREST), (x, y))
        tw = d.textlength(label, font=font)
        d.text((x + (t - tw) / 2, y + t + 5), label, fill=(50, 50, 50), font=font)

    x0 = 26
    d.text((x0, 20), "Shelves fill as you store books", fill=(30, 30, 30), font=bold)
    for i in range(SLOTS + 1):
        lab = "Empty" if i == 0 else f"{i} book{'s' if i > 1 else ''}"
        r, c = divmod(i, 5)
        tile(shelf(i, True), x0 + c * (t + 12), 50 + r * (t + 30), lab)

    d.text((x0, 290), "Lights off (no power) / on (powered)", fill=(30, 30, 30), font=bold)
    pairs = [("Shelf off", shelf(8, False)), ("Shelf on", shelf(8, True)),
             ("Tapestry off", tapestry(False)), ("Tapestry on", tapestry(True))]
    for i, (lab, im) in enumerate(pairs):
        tile(im, x0 + i * (t + 12), 320, lab)

    # Night scene: unpowered half-full vs powered full
    bx, by, bw, bh = 470, 20, 760, 380
    d.rounded_rectangle([bx, by, bx + bw, by + bh], radius=10, fill=(24, 26, 40, 255))
    d.text((bx + 20, by + 14), "At night: no power, 4 books", fill=(220, 220, 230), font=bold)
    d.text((bx + 400, by + 14), "At night: powered, 8 books", fill=(220, 220, 230), font=bold)
    sheet.alpha_composite(night_render(4, False), (bx + 20, by + 50))
    sheet.alpha_composite(night_render(8, True), (bx + 400, by + 50))
    d.text((bx + 20, by + bh + 14), "Glowing parts: shelf lights, shelf crystal, tapestry emblem, and top corner studs.",
           fill=(60, 60, 60), font=font)
    d.text((bx + 20, by + bh + 34), "They stay lit in the dark like AE2's drive lights, and switch off when power is lost.",
           fill=(60, 60, 60), font=font)
    sheet.save(os.path.join(OUT, "library_block_v3.png"))
    print("books columns:", len(book_cols))


if __name__ == "__main__":
    main()
