"""ME Armory and ME Tool Rack, v2.
- Fronts no longer show stored items: fixed artwork.
- Armory looks like an armory: riveted steel plates, fluix shield with crossed swords, certus lock light.
- Tool Rack is a workshop shadow board: pegboard with painted tool outlines.
- Connected textures: the casing frame disappears on any edge touching another block of the same kind,
  so stacks and walls read as one cabinet. Each face needs 16 variants (4 edges open/closed)."""
from PIL import Image, ImageDraw, ImageFont
import os, itertools
import make_racks  # registers extra palette entries in v2.P
import make_library_v2 as v2

OUT = os.path.dirname(os.path.abspath(__file__))
P = v2.P


def plate(x, y):
    """Riveted steel plates on an 8px grid (tiles seamlessly across blocks)."""
    if x % 8 == 7 or y % 8 == 7:
        return "O"
    if (x % 8, y % 8) in [(1, 1), (5, 1), (1, 5), (5, 5)]:
        return "s"
    if x % 8 == 0 or y % 8 == 0:
        return "B"
    return "o"


def pegboard(x, y):
    if x % 4 == 1 and y % 4 == 1:
        return "h"
    return "e"


def casing_side(x, y):
    """Plain AE2 panel for sides (tiles)."""
    if y % 8 == 7:
        return "B"
    return "S" if (x + y) % 11 else "s"


def velvet(x, y):
    return "V" if (x + y) % 2 else "v"


ARMORY_EMBLEM = [  # 12x12: fluix shield over two crossed swords, certus lock light below
    "N..........N",
    ".N........N.",
    "..Nn....nN..",
    "...Nn..nN...",
    "...OOOOOO...",
    "...OAFFAO...",
    "...OFAAFO...",
    "..YOAFFAOY..",
    ".YqOFAAFOqY.",
    "Yq..OAAO..qY",
    "q....OO....q",
    ".....CC.....",
]

RACK_OUTLINES = [  # 12x12: painted silhouettes of a wrench, hammer and screwdriver under glowing certus pegs
    ".C....C...C.",
    "D.D.DDDDD.D.",
    "DDD.DDDDD.D.",
    ".D....D...D.",
    ".D....D...D.",
    ".D....D..DDD",
    ".D....D..DDD",
    ".D....D..DDD",
    ".D....D..DDD",
    "DDD...D...D.",
    "D.D...D.....",
    "............",
]

ARMORY_SIDE_EMBLEM = [
    "............",
    "............",
    ".....N......",
    ".....Nn.....",
    ".....Nn.....",
    ".....Nn.....",
    ".....Nn.....",
    "...YYYYYY...",
    ".....qq.....",
    ".....qq.....",
    "............",
    "............",
]


def face(bg, emblem=None, up=False, down=False, left=False, right=False, lit=True, frame=True):
    """16x16 face. up/down/left/right = True where a same-kind block touches (frame removed there)."""
    rows = [[bg(x, y) for x in range(16)] for y in range(16)]
    if emblem:
        for j, r in enumerate(emblem):
            for i, ch in enumerate(r):
                if ch != ".":
                    rows[2 + j][2 + i] = ch
    if frame:
        def edge(x, y):
            return ("W" if (x + y) % 5 else "w")
        for i in range(16):
            if not up:
                rows[0][i] = edge(i, 0); rows[1][i] = "O" if (left or i > 0) and (right or i < 15) else rows[1][i]
            if not down:
                rows[15][i] = edge(i, 15); rows[14][i] = "O" if (left or i > 0) and (right or i < 15) else rows[14][i]
            if not left:
                rows[i][0] = edge(0, i); rows[i][1] = "O" if (up or i > 0) and (down or i < 15) else rows[i][1]
            if not right:
                rows[i][15] = edge(15, i); rows[i][14] = "O" if (up or i > 0) and (down or i < 15) else rows[i][14]
        # re-draw outer white where two closed edges meet
        for (cx, cy, a, b) in [(0, 0, up, left), (15, 0, up, right), (0, 15, down, left), (15, 15, down, right)]:
            if not a or not b:
                rows[cy][cx] = "W"
    if not lit:
        off = {"C": "o", "j": "B", "c": "O"}
        rows = [[off.get(c, c) for c in r] for r in rows]
    return v2.tex(["".join(r) for r in rows])


KINDS = {
    "armory": {
        "front": (plate, ARMORY_EMBLEM), "side": (plate, ARMORY_SIDE_EMBLEM), "top": (velvet, None),
    },
    "tool_rack": {
        "front": (pegboard, RACK_OUTLINES), "side": (casing_side, None), "top": (casing_side, None),
    },
}


def write_all():
    n = 0
    for kind, faces in KINDS.items():
        for fname, (bg, em) in faces.items():
            for up, down, left, right in itertools.product([False, True], repeat=4):
                for lit in (True, False):
                    key = "".join(k for k, v in zip("udlr", (up, down, left, right)) if v) or "single"
                    img = face(bg, em, up, down, left, right, lit)
                    img.save(os.path.join(OUT, "ctm", f"me_{kind}_{fname}_{key}{'' if lit else '_off'}.png"))
                    n += 1
    return n


def wall(kind, cols=3, rows_=2, lit=True):
    """Boxes for a wall of blocks, cols wide (x) and rows_ high (y), facing south."""
    bg_f, em_f = KINDS[kind]["front"]
    bg_s, em_s = KINDS[kind]["side"]
    bg_t, em_t = KINDS[kind]["top"]
    boxes = []
    for r in range(rows_):
        for c in range(cols):
            up, down = r < rows_ - 1, r > 0
            left, right = c > 0, c < cols - 1
            f = {
                "south": face(bg_f, em_f, up, down, left, right, lit),
                "east": face(bg_s, em_s, up, down, False, False, lit),
                "north": face(bg_s, em_s, up, down, False, False, lit),
                "west": face(bg_s, em_s, up, down, False, False, lit),
                "up": face(bg_t, em_t, False, False, left, right, lit),
                "down": v2.BOTTOM,
            }
            x0, y0 = c * 16, r * 16
            boxes.append(((x0, y0, 0), (x0 + 16, y0 + 16, 16), f))
    return boxes


def main():
    os.makedirs(os.path.join(OUT, "ctm"), exist_ok=True)
    for old in ["me_tool_rack_front", "me_tool_rack_side", "me_tool_rack_top", "me_armory_front", "me_armory_side", "me_armory_top"]:
        for suf in ("", "_off"):
            p = os.path.join(OUT, f"{old}{suf}.png")
            if os.path.exists(p):
                os.remove(p)
    count = write_all()

    font = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf", 13)
    bold = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf", 15)
    sheet = Image.new("RGBA", (1240, 620), (226, 226, 222, 255))
    d = ImageDraw.Draw(sheet)
    t = 64

    def tile(img, x, y, label):
        sheet.alpha_composite(img.resize((t, t), Image.NEAREST), (x, y))
        tw = d.textlength(label, font=font)
        d.text((x + (t - tw) / 2, y + t + 5), label, fill=(50, 50, 50), font=font)

    for col, (kind, title, sub) in enumerate([
        ("armory", "ME Armory", "Riveted steel, fluix shield and crossed swords, certus lock light"),
        ("tool_rack", "ME Tool Rack", "Workshop shadow board: painted tool outlines under glowing certus pegs"),
    ]):
        bx = 20 + col * 615
        d.text((bx, 14), title, fill=(30, 30, 30), font=bold)
        d.text((bx, 36), sub, fill=(80, 80, 80), font=font)
        bf, ef = KINDS[kind]["front"]; bs, es = KINDS[kind]["side"]; bt, et = KINDS[kind]["top"]
        single = [(face(bf, ef), "Front"), (face(bf, ef, lit=False), "Front, off"),
                  (face(bs, es), "Side"), (face(bt, et), "Top"), (v2.BOTTOM, "Bottom")]
        for i, (img, lab) in enumerate(single):
            tile(img, bx + i * 90, 64, lab)
        one = v2.render([((0, 0, 0), (16, 16, 16), {"south": face(bf, ef), "east": face(bs, es), "north": face(bs, es),
                                                      "west": face(bs, es), "up": face(bt, et), "down": v2.BOTTOM})],
                        k=6, size=(200, 210), origin=(100, 15))
        sheet.alpha_composite(one, (bx - 10, 170))
        d.text((bx + 40, 390), "One block", fill=(70, 70, 70), font=font)
        wall_img = v2.render(wall(kind, 3, 2), k=6, size=(420, 400), origin=(100, 100))
        sheet.alpha_composite(wall_img, (bx + 180, 150))
        d.text((bx + 270, 540), "Stacked 3 wide, 2 high: frames merge", fill=(70, 70, 70), font=font)
    d.text((20, 580), f"Neither block shows its contents. Each face has 16 connected versions (lit and unlit): {count} texture files, generated by script.",
           fill=(60, 60, 60), font=font)
    sheet.save(os.path.join(OUT, "armory_and_tool_rack_v2.png"))
    print(count)


if __name__ == "__main__":
    main()
