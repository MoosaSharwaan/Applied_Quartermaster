"""Mockup of the ME Tablet's Library app (inside the 3D tablet's screen) and the ME Library block's own screen."""
from PIL import Image, ImageDraw, ImageFont
import os
import make_open_gui as g

OUT = os.path.dirname(os.path.abspath(__file__))
C = g.C
C.update({"R": (178, 64, 82), "r": (122, 40, 58), "T": (64, 150, 140), "t2": (38, 96, 92),
          "Yd": (160, 120, 46), "P": (236, 228, 206), "Gr": (96, 160, 80), "gr": (60, 110, 50),
          "Bl": (70, 110, 190), "bl": (44, 70, 130), "sel": (98, 78, 170)})
S = g.S


def book(cover, dark, mark="Y"):
    im = Image.new("RGBA", (16, 16))
    d = ImageDraw.Draw(im)
    c, k, m, p = C[cover] + (255,), C[dark] + (255,), C[mark] + (255,), C["P"] + (255,)
    d.rectangle([3, 1, 13, 14], fill=k)          # outline
    d.rectangle([4, 2, 12, 12], fill=c)          # cover
    d.rectangle([4, 13, 12, 13], fill=p)         # page edge
    d.line([(5, 2), (5, 12)], fill=k)            # spine line
    d.rectangle([7, 5, 10, 8], fill=m)           # emblem
    d.point((8, 6), fill=k)
    return im


AE2_GUIDE = g.AE["guide"]
BOOKS = [  # (icon, title, system, library)
    (AE2_GUIDE, "AE2 Guide", "GuideME", "A1"),
    (book("F", "I", "C"), "Dictionary of Spirits", "Modonomicon", "A1"),
    (book("R", "r"), "Theurgy", "Modonomicon", "A1"),
    (book("Gr", "gr"), "Productive Bees", "Patchouli", "A2"),
    (book("T", "t2", "C"), "Integrated Dynamics", "Other", "A2"),
    (book("Bl", "bl"), "Mystical Agriculture", "Patchouli", "A2"),
    (book("R", "r", "C"), "EvilCraft", "Other", "A3"),
    (book("Yd", "dark", "P"), "My base notes", "Written", "A3"),
    (book("F", "I"), "Forbidden & Arcanus", "Modonomicon", "A3"),
]


def library_app():
    W, H = 252, 150
    c = g.Canvas(W, H + 10)
    oy = 10
    c.rect(226, 0, 228, 2, "A"); c.rect(227, 3, 227, oy, "O")
    c.rect(1, oy, W - 2, oy + H - 1, "O"); c.rect(0, oy + 1, W - 1, oy + H - 2, "O")
    c.rect(2, oy + 1, W - 3, oy + H - 2, "s")
    c.rect(2, oy + 1, W - 4, oy + 2, "W"); c.rect(2, oy + 1, 3, oy + H - 4, "W")
    c.rect(W - 4, oy + 3, W - 3, oy + H - 2, "S"); c.rect(3, oy + H - 4, W - 3, oy + H - 2, "B")
    sx0, sy0, sx1, sy1 = 9, oy + 8, W - 10, oy + H - 17
    c.rect(sx0 - 1, sy0 - 1, sx1 + 1, sy1 + 1, "F"); c.rect(sx0, sy0, sx1, sy1, "I")
    # header: back arrow, title, store button
    c.rect(sx0, sy0, sx1, sy0 + 11, "i")
    c.rect(sx0 + 4, sy0 + 5, sx0 + 9, sy0 + 5, "txt"); c.rect(sx0 + 5, sy0 + 4, sx0 + 5, sy0 + 6, "txt")
    c.rect(sx0 + 4, sy0 + 5, sx0 + 4, sy0 + 5, "txt")
    c.icon(g.ICON["library"], sx0 + 13, sy0 - 2)
    c.text(sx0 + 31, sy0 + 2, "Library", font=g.FONTB)
    c.text(sx0 + 70, sy0 + 2, "3 libraries · 19 of 24 slots", "dim")
    c.rect(sx1 - 34, sy0 + 2, sx1 - 3, sy0 + 9, "F"); c.text(sx1 - 30, sy0 + 2, "Store", "txt", g.SMALL)
    # search + filter chips
    y = sy0 + 15
    c.rect(sx0 + 4, y, sx0 + 62, y + 9, "i"); c.rect(sx0 + 4, y, sx0 + 62, y, "t")
    c.text(sx0 + 7, y + 1, "Search books…", "dim", g.SMALL)
    x = sx0 + 67
    for i, chip in enumerate(["All", "Patchouli", "Modonomicon", "GuideME", "Other"]):
        w = c.textw(chip, g.SMALL) + 6
        c.rect(x, y, x + w, y + 9, "F" if i == 0 else "t")
        c.text(x + 3, y + 1, chip, "txt" if i == 0 else "dim", g.SMALL)
        x += w + 3
    # book grid 3 x 3 cards
    cw, ch, gap = 74, 25, 3
    gx0, gy0 = sx0 + 4, y + 13
    for i, (icon, title, system, lib) in enumerate(BOOKS):
        r, col = divmod(i, 3)
        bx, by = gx0 + col * (cw + gap), gy0 + r * (ch + gap)
        selected = i == 1
        c.rect(bx, by, bx + cw - 1, by + ch - 1, "sel" if selected else "t")
        if selected:
            c.rect(bx, by, bx + cw - 1, by, "G")
        c.icon(icon, bx + 3, by + 4)
        c.text(bx + 21, by + 3, title if len(title) < 17 else title[:16] + "…", "txt", g.SMALL)
        tag = "Universal" if system == "Other" else system
        c.text(bx + 21, by + 13, f"{tag} · {lib}", "dim", g.SMALL)
    # action bar for the selected book
    ay = gy0 + 3 * (ch + gap) + 1
    c.rect(sx0, ay, sx1, sy1, "i")
    c.text(sx0 + 4, ay + 2, "Dictionary of Spirits", "txt", g.SMALL)
    bx = sx1 - 92
    for label, col in [("Read", "F"), ("Take", "t"), ("Take all copies", "t")]:
        w = c.textw(label, g.SMALL) + 6
        c.rect(bx, ay + 1, bx + w, ay + 8, col)
        c.text(bx + 3, ay + 1, label, "txt", g.SMALL)
        bx += w + 3
    c.rect(W // 2 - 8, oy + H - 12, W // 2 + 7, oy + H - 9, "F")
    c.rect(W // 2 - 8, oy + H - 12, W // 2 + 7, oy + H - 12, "A")
    return c.img


def library_block_gui():
    W, H = 176, 146
    c = g.Canvas(W, H)
    c.bevel(0, 0, W - 1, H - 1, "s")
    c.text(8, 5, "ME Library", "dark", g.FONTB)
    c.text(W - 64, 6, "Online · 1 channel", "b", g.SMALL)
    filled = [BOOKS[0][0], BOOKS[1][0], BOOKS[2][0], None, None, None, None, None]
    for i in range(8):
        c.slot(16 + i * 18, 20, item=filled[i])
    c.text(16, 41, "3 of 8 books", "b", g.SMALL)
    c.text(8, 52, "Inventory", "dark", g.SMALL)
    for r in range(3):
        for k in range(9):
            c.slot(8 + k * 18, 61 + r * 18)
    for k in range(9):
        c.slot(8 + k * 18, 61 + 3 * 18 + 4)
    return c.img


def main():
    a, b = library_app(), library_block_gui()
    font = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf", 16)
    small = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf", 14)
    pad = 28
    sheet = Image.new("RGBA", (pad * 3 + a.width + b.width, max(a.height, b.height) + 150), (52, 54, 66, 255))
    d = ImageDraw.Draw(sheet)
    d.text((pad, 18), "Library app on the ME Tablet", fill=(240, 240, 245), font=font)
    d.text((pad * 2 + a.width, 18), "ME Library block screen", fill=(240, 240, 245), font=font)
    sheet.alpha_composite(a, (pad, 50))
    sheet.alpha_composite(b, (pad * 2 + a.width, 50))
    notes = ["Click a book to select it; Read opens it over the tablet, Take moves it to your inventory.",
             "Store puts a guide from your inventory into the first library with space.",
             "A1-A3 = which library a book is in (you can rename libraries).",
             "Universal = books from other systems, opened by the right-click fallback."]
    for i, n in enumerate(notes):
        d.text((pad, 50 + max(a.height, b.height) + 14 + i * 21), n, fill=(210, 210, 222), font=small)
    sheet.save(os.path.join(OUT, "library_view.png"))
    print(sheet.size)


if __name__ == "__main__":
    main()
