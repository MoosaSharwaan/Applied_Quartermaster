"""ME Tool Rack and ME Armory: face textures in the same AE2-casing family as the ME Library,
plus a preview with 3D views showing stored items mounted on the front (rendered in game by a
block-entity renderer, so players see their real items, enchant glint included).
Mockup items: AE2's wrench/network tool/memory card (illustration only) and original sprites."""
from PIL import Image, ImageDraw, ImageFont
import os
import make_library_v2 as v2

OUT = os.path.dirname(os.path.abspath(__file__))
AE2 = "/home/claude/refs/ae2/src/main/resources/assets/ae2/textures/item"
P = v2.P
P.update({
    "e": (96, 98, 120, 255),    # pegboard
    "h": (70, 72, 92, 255),     # peg hole
    "v": (74, 46, 120, 255),    # velvet dark
    "V": (98, 64, 150, 255),    # velvet light
    "n": (130, 134, 150, 255),  # iron mid
    "N": (210, 214, 224, 255),  # iron light
    "x": (68, 200, 214, 255),   # diamond
    "X": (150, 236, 240, 255),  # diamond light
    "k": (40, 34, 30, 255),     # netherite dark
    "m": (88, 76, 70, 255),     # netherite
    "q": (120, 84, 50, 255),    # handle wood
    "Q": (160, 116, 70, 255),   # handle light
    "z": (60, 40, 22, 255),     # string / dark wood
})

FRAME_TOP = ["WWWWWWWwWwwwwWWW", "WOOOoOooooooOOOW"]
FRAME_BOT = ["WOOOoooooOoOOOOW", "WWWwwwwWwWWWWWWW"]


def framed(inner12):
    """2-pixel AE2 casing frame around a 12x12 interior."""
    rows = FRAME_TOP + [("W" if i % 3 else "w") + "O" + r + "O" + ("W" if i % 2 else "w") for i, r in enumerate(inner12)] + FRAME_BOT
    return v2.tex(rows)


# ---------------- ME Tool Rack faces
RACK_FRONT = framed([
    "eeeeeeeeeeee",
    "ehehehehehee",
    "eCeeCeeCeeCe",
    "ehehehehehee",
    "eeeeeeeeeeee",
    "ehehehehehee",
    "eeeeeeeeeeee",
    "ehehehehehee",
    "eCeeCeeCeeCe",
    "ehehehehehee",
    "eeeeeeeeeeee",
    "BBBBBBBBBBBB",
])
RACK_SIDE = framed([
    "ssssssssssss",
    "sSSSSSSSSSSB",
    "sSoooooooooB",
    "sSoNnNnNnoSB",
    "sSoooooooooB",
    "sSSSSSSSSSSB",
    "sSSDDDDDDSSB",
    "sSSDCjCCDSSB",
    "sSSDDDDDDSSB",
    "sSSSSSSSSSSB",
    "sSoSSSSSSoSB",
    "BBBBBBBBBBBB",
])
RACK_TOP = framed([
    "SSSSSSSSSSSS",
    "SBBBBBBBBBBS",
    "SBooooooooBS",
    "SBoNNNNNNoBS",
    "SBooooooooBS",
    "SBBBBBBBBBBS",
    "SSSSSSSSSSSS",
    "SoSoSoSoSoSS",
    "SSSSSSSSSSSS",
    "SoSoSoSoSoSS",
    "SSSSSSSSSSSS",
    "BBBBBBBBBBBB",
])

# ---------------- ME Armory faces
ARMORY_FRONT = framed([
    "NnnnnnnnnnnN",
    "vVvVvVvVvVvV",
    "VvVvVvVvVvVv",
    "vVvVvVvVvVvV",
    "VvVvVvVvVvVv",
    "NnnnnnnnnnnN",
    "vVvVvVvVvVvV",
    "VvVvVvVvVvVv",
    "vVvVvVvVvVvV",
    "VvVvVvVvVvVv",
    "NnnnnnnnnnnN",
    "YCYYYYYYYYCY",
])
ARMORY_SIDE = framed([
    "ssssssssssss",
    "sSSSSSSSSSSB",
    "sSSSSDSSSSSB",
    "sSSSSjDSSSSB",
    "sSSSSjCDSSSB",
    "sSSSjCSSSSSB",
    "sSSSjCSSSSSB",
    "sSSYYYYYSSSB",
    "sSSSSqSSSSSB",
    "sSSSSqSSSSSB",
    "sSSSSSSSSSSB",
    "BBBBBBBBBBBB",
])
ARMORY_TOP = framed([
    "vvvvvvvvvvvv",
    "vVVVVVVVVVVv",
    "vVYYYYYYYYVv",
    "vVYvVvVvVYVv",
    "vVYVvVvVvYVv",
    "vVYvVvVvVYVv",
    "vVYVvVvVvYVv",
    "vVYvVvVvVYVv",
    "vVYYYYYYYYVv",
    "vVVVVVVVVVVv",
    "vvvvvvvvvvvv",
    "BBBBBBBBBBBB",
])
BOTTOM = v2.BOTTOM


def unlit(img):
    """Lights off: certus pixels go dark slate."""
    mapping = {P["C"]: P["o"], P["j"]: P["B"]}
    out = img.copy()
    out.putdata([mapping.get(p, p) for p in out.get_flattened_data()])
    return out


# ---------------- original item sprites for the mockup
def _sprite(rows):
    im = Image.new("RGBA", (16, 16))
    for y, r in enumerate(rows):
        for x, ch in enumerate(r):
            if ch != ".":
                im.putpixel((x, y), P[ch])
    return im


SWORD = _sprite([
    "..............oo", ".............oXo", "............oXxo", "...........oXxo.",
    "..........oXxo..", ".........oXxo...", "........oXxo....", "...o...oXxo.....",
    "...oo.oXxo......", "....ooXxo.......", "....oYYo........", "...oqYoo........",
    "..oqqo.oo.......", ".oqqo...........", "oQqo............", "ooo.............",
])
PICK = _sprite([
    "................", "....ooooooo.....", "...oNNNNNNNo....", "..oNnooooonNo...",
    "..oNo..oq..oNo..", "..oo...oq...oo..", ".......oq.......", ".......oq.......",
    ".......oQ.......", ".......oq.......", ".......oq.......", ".......oQ.......",
    ".......oq.......", ".......oq.......", ".......oo.......", "................",
])
AXE = _sprite([
    "................", "........oooo....", ".......oXXXxo...", "......oXxxxxxo..",
    "......oXxxoqqo..", "......oXxo.oqo..", ".......ooo.oqo..", "..........oQo...",
    ".........oqo....", "........oqo.....", ".......oQo......", "......oqo.......",
    ".....oqo........", "....oQo.........", "....oo..........", "................",
])
SHOVEL = _sprite([
    "................", "..........ooo...", ".........okmko..", "........okmmko..",
    ".........okkko..", "........oqoo....", ".......oqo......", "......oQo.......",
    ".....oqo........", "....oqo.........", "...oQo..........", "..oqo...........",
    ".oqqo...........", ".ooo............", "................", "................",
])
BOW = _sprite([
    "................", "....oooo........", "...oqQqzo.......", "..oqo..z.o......",
    "..oo...z..o.....", ".oq....z...o....", ".oq....z...o....", ".oQ....z....o...",
    ".oq....z....o...", ".oq....z...o....", ".oq....z...o....", "..oo...z..o.....",
    "..oqo..z.o......", "...oqQqzo.......", "....oooo........", "................",
])
SCREWDRIVER = _sprite([
    "................", "...........oo...", "..........oNo...", ".........oNo....",
    "........oNo.....", ".......oNo......", "......oNo.......", ".....ooo........",
    "....oFAo........", "...oFAFo........", "..oFAFo.........", ".oFAFo..........",
    ".oFFo...........", "..oo............", "................", "................",
])


def mount(face, items, cols, rows, box, size, angle=0):
    """Compose a hi-res front face with items placed in a grid inside `box` (texel coords)."""
    R = 6  # face upscale
    big = face.resize((16 * R, 16 * R), Image.NEAREST)
    x0, y0, x1, y1 = box
    cw, ch = (x1 - x0) * R / cols, (y1 - y0) * R / rows
    for i, it in enumerate(items):
        if it is None:
            continue
        r, c = divmod(i, cols)
        s = int(size * R)
        im = it.resize((s, s), Image.NEAREST)
        if angle:
            im = im.rotate(angle, resample=Image.NEAREST, expand=True)
        px = int(x0 * R + c * cw + (cw - im.width) / 2)
        py = int(y0 * R + r * ch + (ch - im.height) / 2)
        shadow = Image.new("RGBA", im.size, (0, 0, 0, 0))
        shadow.putalpha(im.getchannel("A").point(lambda a: 90 if a else 0))
        big.alpha_composite(shadow, (px + 2, py + 3))
        big.alpha_composite(im, (px, py))
    return big


def main():
    out = {
        "me_tool_rack_front": RACK_FRONT, "me_tool_rack_side": RACK_SIDE, "me_tool_rack_top": RACK_TOP,
        "me_armory_front": ARMORY_FRONT, "me_armory_side": ARMORY_SIDE, "me_armory_top": ARMORY_TOP,
    }
    for name, im in out.items():
        im.save(os.path.join(OUT, f"{name}.png"))
        unlit(im).save(os.path.join(OUT, f"{name}_off.png"))

    ae = lambda n: Image.open(f"{AE2}/{n}.png").convert("RGBA").crop((0, 0, 16, 16))
    rack_items = [ae("certus_quartz_wrench"), ae("network_tool"), ae("memory_card_base"), SCREWDRIVER,
                  ae("nether_quartz_wrench"), None, ae("memory_card_base"), None]
    arm_items = [SWORD, PICK, AXE, BOW, SHOVEL, SWORD, None, None]
    rack_face = mount(RACK_FRONT, rack_items, 4, 2, (2, 2, 14, 13), 4.2)
    arm_face = mount(ARMORY_FRONT, arm_items, 4, 2, (2, 1, 14, 13), 4.6)

    font = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf", 13)
    bold = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf", 15)
    sheet = Image.new("RGBA", (1240, 510), (226, 226, 222, 255))
    d = ImageDraw.Draw(sheet)
    t = 64

    def tile(img, x, y, label):
        sheet.alpha_composite(img.resize((t, t), Image.NEAREST), (x, y))
        tw = d.textlength(label, font=font)
        d.text((x + (t - tw) / 2, y + t + 5), label, fill=(50, 50, 50), font=font)

    for col, (title, fr, sd, tp, face, sub) in enumerate([
        ("ME Tool Rack", RACK_FRONT, RACK_SIDE, RACK_TOP, rack_face, "Wrenches, memory cards, network tools and other utility tools"),
        ("ME Armory", ARMORY_FRONT, ARMORY_SIDE, ARMORY_TOP, arm_face, "Weapons and mining tools"),
    ]):
        bx = 20 + col * 615
        d.text((bx, 14), title, fill=(30, 30, 30), font=bold)
        d.text((bx, 36), sub, fill=(80, 80, 80), font=font)
        sheet.alpha_composite(face.resize((224, 224), Image.NEAREST), (bx, 64))
        d.text((bx, 294), "Front with 6 of 8 spots filled", fill=(70, 70, 70), font=font)
        view = v2.render([((0, 0, 0), (16, 16, 16),
                           {"up": tp, "down": BOTTOM, "south": face, "north": sd, "east": sd, "west": sd})],
                         k=8, size=(300, 300), origin=(150, 40))
        sheet.alpha_composite(view, (bx + 250, 40))
        for i, (img, lab) in enumerate([(fr, "Front"), (sd, "Sides, back"), (tp, "Top"),
                                         (unlit(fr), "Front, off"), (unlit(sd), "Side, off"), (BOTTOM, "Bottom")]):
            tile(img, bx + i * 96, 330, lab)
    d.text((20, 440), "Stored items hang on the front, rendered as your real items (enchant glint included); empty spots stay bare.", fill=(60, 60, 60), font=font)
    d.text((20, 462), "Tool Rack: 8 hooks. Armory: 8 mounts. Both use 1 channel and connect to touching AE2 blocks, like the ME Library.", fill=(60, 60, 60), font=font)
    d.text((20, 484), "Certus lights glow only with power and a channel. Wrench, network tool and memory card icons are AE2's, shown for illustration.", fill=(60, 60, 60), font=font)
    sheet.save(os.path.join(OUT, "tool_rack_and_armory.png"))
    print("ok")


if __name__ == "__main__":
    main()
