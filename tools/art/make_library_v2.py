"""ME Library v2 — Apothic-Enchanting-style library (bookshelf sides, hanging tapestries,
cloth top, 3D book stacks on top) built in AE2's casing + fluix/certus palette.
Outputs face textures and a preview with the face layout and two 3D views."""
from PIL import Image, ImageDraw, ImageFont
import os

OUT = os.path.dirname(os.path.abspath(__file__))

P = {
    ".": (0, 0, 0, 0),
    "W": (242, 242, 242, 255), "w": (226, 227, 232, 255),          # casing whites
    "O": (52, 50, 70, 255), "o": (77, 77, 103, 255),                # slate
    "s": (188, 190, 204, 255), "S": (160, 163, 184, 255),          # silver
    "B": (124, 128, 152, 255), "D": (33, 32, 52, 255), "K": (22, 20, 36, 255),
    "C": (156, 211, 255, 255), "c": (110, 160, 214, 255), "j": (220, 255, 255, 255),
    "F": (145, 93, 205, 255), "A": (176, 111, 221, 255), "I": (90, 71, 158, 255),
    "i": (64, 50, 120, 255), "G": (226, 163, 227, 255),
    "R": (178, 64, 82, 255), "r": (122, 40, 58, 255),               # crimson books
    "T": (64, 150, 140, 255), "t": (38, 96, 92, 255),               # teal books
    "Y": (226, 186, 88, 255), "y": (160, 120, 46, 255),             # gold trim / tan books
    "P": (236, 228, 206, 255), "p": (200, 190, 166, 255),           # page edges
}


def tex(rows):
    assert len(rows) == 16 and all(len(r) == 16 for r in rows), [(i, len(r)) for i, r in enumerate(rows)]
    im = Image.new("RGBA", (16, 16))
    for y, r in enumerate(rows):
        for x, ch in enumerate(r):
            im.putpixel((x, y), P[ch])
    return im


# Bookshelf side: AE2 casing edges, two shelves of mixed books, a leaning book,
# a gap holding a certus crystal, silver shelf boards with cyan status LEDs.
SHELF = tex([
    "WWWWWWWwWwwwwWWW",
    "WOOOoOooooooOOOW",
    "WODDDDDDDDDDDDOW",
    "WODRTAyDDFRIDDOw",
    "wODRTAYDDFRIGDOW",
    "WODrTAYDTFRIGDOw",
    "WODRtAyFTFrIGDOW",
    "wODRTAYFtFRiGDOW",
    "WOsSCSSSSSsCSSOw",
    "WODDTAKDDDjDYRow",
    "wODITAFDDjCDYROW",
    "WODIGAFDDCcDYrOw",
    "WODIGAFRDcDDYROW",
    "wOsSSSCSSSsSSSOW",
    "WOOOoooooOoOOOOW",
    "WWWwwwwWwWWWWWWW",
])

# Tapestry side: fluix banner hanging from a silver rod over dark casing, folds,
# glowing certus "book" glyph, pointed hem with gold trim.
BANNER = tex([
    "WWWWWWWwWwwwwWWW",
    "WOsWWWWWWWWWWsOW",
    "WODBSSSSSSSSBDOw",
    "WODAFAIFAFIAFDOW",
    "wODAFAIFAFIAFDOW",
    "WODAFCCCCCCAFDOw",
    "WODAFCjjjjCAFDOW",
    "wODAFCjoojCAFDOW",
    "WODAFCjjjjCAFDOw",
    "WODAFCCCCCCAFDOW",
    "wODAFAIFAFIAFDOW",
    "WODYAFIFAFIFYDOw",
    "WODDYAFIFAFYDDOW",
    "wODDDYYAFYYDDDOW",
    "WOOOoooooOoOOOOW",
    "WWWwwwwWwWWWWWWW",
])

# Top: fluix cloth with gold trim and silver casing edge, certus studs in the corners.
TOP = tex([
    "WWWWWWWwWwwwwWWW",
    "WCOOoOooooooOCOW",
    "WOYYYYYYYYYYYYOW",
    "WOYAFAFAFAFAFYOw",
    "wOYFAFAFAFAFAYOW",
    "WOYAFIIIIIIAFYOw",
    "WOYFAIFAFAIFAYOW",
    "wOYAFIAFAFIAFYOW",
    "WOYFAIFAFAIFAYOw",
    "WOYAFIIIIIIAFYOW",
    "wOYFAFAFAFAFAYOW",
    "WOYAFAFAFAFAFYOw",
    "WOYYYYYYYYYYYYOW",
    "wCOOoooooOoOOCOW",
    "WOOOoooooOoOOOOW",
    "WWWwwwwWwWWWWWWW",
])

BOTTOM = tex([
    "WWWWWWWwWwwwwWWW",
    "WOOOoOooooooOOOW",
    "WOSSSSSSSSSSSSOW",
    "WOSBBBBBBBBBBSOw",
    "wOSBoSoSoSoSBSOW",
    "WOSBSSSSSSSSBSOw",
    "WOSBoSoSoSoSBSOW",
    "wOSBSSSSSSSSBSOW",
    "WOSBoSoSoSoSBSOw",
    "WOSBSSSSSSSSBSOW",
    "wOSBoSoSoSoSBSOW",
    "WOSBBBBBBBBBBSOw",
    "WOSSSSSSSSSSSSOW",
    "wOOOoooooOoOOOOW",
    "WOOOoooooOoOOOOW",
    "WWWwwwwWwWWWWWWW",
])


def solid(w, h, ch):
    im = Image.new("RGBA", (w, h), P[ch])
    return im


def book_faces(w, d, h, cover, dark):
    """Textures for a book lying flat: w along x, d along z, h tall (texel units)."""
    top = Image.new("RGBA", (w, d), P[cover])
    dr = ImageDraw.Draw(top)
    dr.rectangle([0, 0, w - 1, d - 1], outline=P[dark])
    if w > 3 and d > 3:
        dr.point([(w // 2, d // 2), (w // 2 - 1, d // 2)], fill=P["Y"])
    def side(length, spine):
        im = Image.new("RGBA", (length, h), P["P"])
        dd = ImageDraw.Draw(im)
        dd.line([(0, 0), (length - 1, 0)], fill=P[dark])
        dd.line([(0, h - 1), (length - 1, h - 1)], fill=P[dark])
        if h > 2:
            for x in range(length):
                if x % 2:
                    dd.point((x, h // 2), fill=P["p"])
        if spine:
            im = Image.new("RGBA", (length, h), P[cover])
            dd = ImageDraw.Draw(im)
            dd.line([(0, h - 1), (length - 1, h - 1)], fill=P[dark])
            dd.point((length // 2, 0), fill=P["Y"])
        return im
    return {"up": top, "down": top, "south": side(w, True), "north": side(w, False),
            "east": side(d, False), "west": side(d, True)}


def crystal_faces(w, d, h):
    face = Image.new("RGBA", (max(w, d), h), P["C"])
    dd = ImageDraw.Draw(face)
    dd.line([(0, 0), (0, h - 1)], fill=P["j"])
    dd.line([(face.width - 1, 0), (face.width - 1, h - 1)], fill=P["c"])
    top = solid(w, d, "j")
    return {"up": top, "down": top, "south": face.crop((0, 0, w, h)), "north": face.crop((0, 0, w, h)),
            "east": face.crop((0, 0, d, h)), "west": face.crop((0, 0, d, h))}


FACES = {"up": TOP, "down": BOTTOM, "south": BANNER, "north": BANNER, "east": SHELF, "west": SHELF}

# Model: the cube plus decorations on top (coordinates in texels, y up).
BOXES = [((0, 0, 0), (16, 16, 16), FACES)]
for frm, size, cover, dark in [
    ((2, 16, 9), (6, 2, 5), "R", "r"),
    ((2, 18, 9), (5, 1, 5), "T", "t"),
    ((3, 19, 10), (5, 2, 4), "A", "i"),
    ((9, 16, 2), (5, 2, 6), "Y", "y"),
    ((9, 18, 3), (4, 1, 5), "F", "I"),
]:
    w, h, d = size
    BOXES.append((frm, (frm[0] + w, frm[1] + h, frm[2] + d), book_faces(w, d, h, cover, dark)))
for frm, size in [((11, 16, 11), (2, 5, 2)), ((13, 16, 12), (1, 3, 1)), ((10, 16, 13), (1, 2, 1))]:
    w, h, d = size
    BOXES.append((frm, (frm[0] + w, frm[1] + h, frm[2] + d), crystal_faces(w, d, h)))


def shade(img, f):
    out = img.copy()
    out.putdata([(int(r * f), int(g * f), int(b * f), a) for r, g, b, a in out.get_flattened_data()])
    return out


def render(boxes, k=8, rotate=False, size=(330, 330), origin=(165, 70)):
    """Isometric view from the south-east (or north-west when rotate=True).
    Screen: X = ox + (x - z)*k, Y = oy + (x + z)*k/2 - y*k."""
    img = Image.new("RGBA", size, (0, 0, 0, 0))
    ox, oy = origin

    def proj(x, y, z):
        return (ox + (x - z) * k, oy + (x + z) * k / 2 - y * k + 16 * k)

    def draw_face(t, p0, pu, pv):
        tw, th = t.size
        S = 8
        big = t.resize((tw * S, th * S), Image.NEAREST)
        a, b = (pu[0] - p0[0]) / (tw * S), (pv[0] - p0[0]) / (th * S)
        d, e = (pu[1] - p0[1]) / (tw * S), (pv[1] - p0[1]) / (th * S)
        det = a * e - b * d
        ia, ib, id_, ie = e / det, -b / det, -d / det, a / det
        coeffs = (ia, ib, -(ia * p0[0] + ib * p0[1]), id_, ie, -(id_ * p0[0] + ie * p0[1]))
        img.alpha_composite(big.transform(size, Image.AFFINE, coeffs, resample=Image.NEAREST))

    items = []
    for (x0, y0, z0), (x1, y1, z1), faces in boxes:
        if rotate:  # spin the model 180 degrees around the block centre
            x0, x1, z0, z1 = 16 - x1, 16 - x0, 16 - z1, 16 - z0
            faces = {**faces, "south": faces["north"], "north": faces["south"],
                     "east": faces["west"], "west": faces["east"],
                     "up": faces["up"].rotate(180), "down": faces["down"]}
        items.append(((x0 + z0 + y0 * 0.01), (x0, y0, z0, x1, y1, z1), faces))
    for _, (x0, y0, z0, x1, y1, z1), f in sorted(items, key=lambda t: t[0]):
        w, d, h = x1 - x0, z1 - z0, y1 - y0
        def fit(t, tw, th):
            if t.size == (tw, th) or (t.width % tw == 0 and t.width * th == t.height * tw):
                return t  # exact size, or a hi-res face with the same aspect
            return t.resize((tw, th), Image.NEAREST)
        # south face (left on screen): u +x, v down
        draw_face(shade(fit(f["south"], w, h), 0.82), proj(x0, y1, z1), proj(x1, y1, z1), proj(x0, y0, z1))
        # east face (right on screen): u -z, v down
        draw_face(shade(fit(f["east"], d, h), 0.64), proj(x1, y1, z1), proj(x1, y1, z0), proj(x1, y0, z1))
        # top: u +x, v +z
        draw_face(fit(f["up"], w, d), proj(x0, y1, z0), proj(x1, y1, z0), proj(x0, y1, z1))
    return img


def main():
    for name, t in [("shelf", SHELF), ("tapestry", BANNER), ("top", TOP), ("bottom", BOTTOM)]:
        t.save(os.path.join(OUT, f"me_library_{name}.png"))
    for old in ["front", "side", "back"]:
        p = os.path.join(OUT, f"me_library_{old}.png")
        if os.path.exists(p):
            os.remove(p)

    font = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf", 14)
    bold = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf", 15)
    sheet = Image.new("RGBA", (1230, 430), (226, 226, 222, 255))
    d = ImageDraw.Draw(sheet)

    t, ox, oy = 76, 26, 66
    layout = [("Top", TOP, 1, 0), ("Left side", SHELF, 0, 1), ("Front", BANNER, 1, 1),
              ("Right side", SHELF, 2, 1), ("Back", BANNER, 3, 1), ("Bottom", BOTTOM, 1, 2)]
    d.text((ox, 22), "All six faces", fill=(30, 30, 30), font=bold)
    for label, tx, cx, cy in layout:
        x, y = ox + cx * (t + 36), oy + cy * (t + 30)
        sheet.alpha_composite(tx.resize((t, t), Image.NEAREST), (x, y))
        tw = d.textlength(label, font=font)
        d.text((x + (t - tw) / 2, y + t + 5), label, fill=(50, 50, 50), font=font)

    v1 = render(BOXES)
    v2 = render(BOXES, rotate=True)
    d.text((500, 22), "Front and right side", fill=(30, 30, 30), font=bold)
    d.text((860, 22), "Back and left side", fill=(30, 30, 30), font=bold)
    sheet.alpha_composite(v1, (480, 40))
    sheet.alpha_composite(v2, (840, 40))
    d.text((500, 395), "3D book stacks and a certus crystal sit on top", fill=(70, 70, 70), font=font)
    sheet.save(os.path.join(OUT, "library_block_v2.png"))
    print("ok")


if __name__ == "__main__":
    main()
