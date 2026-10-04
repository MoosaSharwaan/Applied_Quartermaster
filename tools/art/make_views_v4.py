"""Views v4: 5 items per row, triple-size items, item name under each item.
Stored items sit on the panel (no box); free spots are slot boxes. Grid shows 3 rows, then scrolls."""
from PIL import Image, ImageDraw, ImageFont
import os
import make_open_gui as g
import make_views_v2 as v
import make_racks as mr
import make_library_view as lv

OUT = os.path.dirname(os.path.abspath(__file__))
COLS, MAXROWS = 5, 3
ITEM = 3                     # 16px -> 48px
CW, CH = 66, 72              # cell size in GUI px
GX, GY = 10, 26
PANEL_X, PANEL_Y = 24, 18
W = GX + COLS * CW + 20
NAME = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf", 5 * g.S + 1)


def wrap(c, text, width):
    words, lines, cur = text.split(), [], ""
    for w_ in words:
        t = (cur + " " + w_).strip()
        if c.textw(t, NAME) <= width:
            cur = t
        else:
            lines.append(cur); cur = w_
    lines.append(cur)
    if len(lines) > 2:
        lines = lines[:2]
        while c.textw(lines[1] + "…", NAME) > width:
            lines[1] = lines[1][:-1]
        lines[1] += "…"
    return lines


def box(c, x, y, s=50):
    c.rect(x, y, x + s - 1, y + s - 1, "W")
    c.rect(x, y, x + s - 2, y + s - 2, "B")
    c.rect(x + 1, y + 1, x + s - 2, y + s - 2, "S")


def view(kind, items, capacity, status, tip=None, held=None, scroll=0.0):
    rows_total = -(-capacity // COLS)
    rows = min(rows_total, MAXROWS)
    H = GY + rows * CH + 14 + 10 + 3 * 18 + 4 + 18 + 8
    c = g.Canvas(PANEL_X + W + 96, PANEL_Y + H + 4)
    px, py = PANEL_X, PANEL_Y
    for i, name in enumerate(["Library", "Armory", "Tools"]):
        tx = px + 4 + i * 26
        on = name == kind
        c.bevel(tx, py - 16 + (0 if on else 2), tx + 24, py + (1 if on else 0), "s" if on else "S")
        c.icon(v.tab_icon(name), tx + 5, py - 13 + (0 if on else 2))
    c.bevel(px, py, px + W - 1, py + H - 1, "s")
    c.text(px + 10, py + 7, {"Library": "ME Library", "Armory": "ME Armory", "Tools": "ME Tool Rack"}[kind], "dark", g.FONTB)
    c.rect(px + W - 92, py + 6, px + W - 12, py + 16, "O"); c.rect(px + W - 91, py + 7, px + W - 13, py + 15, (236, 236, 240))
    c.text(px + W - 88, py + 7, "Search…", (150, 150, 165), g.SMALL)
    hover = tip[0] if tip else None
    for i in range(min(capacity, rows * COLS)):
        r, k = divmod(i, COLS)
        x, y = px + GX + k * CW, py + GY + r * CH
        ix, iy = x + (CW - 50) // 2, y + 2
        it = items[i] if i < len(items) else None
        if held and held[1] == i:
            box(c, ix, iy)
            c.rect(ix + 1, iy + 1, ix + 48, iy + 48, (200, 220, 255))
            c.icon(held[0], ix + 1, iy + 1, scale=ITEM)
            continue
        if it is None:
            box(c, ix, iy)
            continue
        icon, nm, dur, ench = it
        if i == hover:
            c.rect(x + 1, y, x + CW - 2, y + CH - 3, (225, 226, 234))
        c.icon(v.glint(icon) if ench else icon, ix + 1, iy + 1, scale=ITEM)
        if dur is not None and dur < 1:
            col = (int(255 * (1 - dur)), int(255 * dur), 0)
            c.rect(ix + 6, iy + 46, ix + 43, iy + 48, (0, 0, 0))
            c.rect(ix + 6, iy + 46, ix + 6 + int(37 * dur), iy + 47, col)
        for j, line in enumerate(wrap(c, nm, CW - 4)):
            c.text(x + (CW - c.textw(line, NAME)) / 2, y + 53 + j * 7, line, "dark", NAME)
    # scrollbar
    sx = px + GX + COLS * CW + 4
    track_h = rows * CH - 4
    c.rect(sx, py + GY, sx + 11, py + GY + track_h, "B")
    c.rect(sx + 1, py + GY + 1, sx + 10, py + GY + track_h - 1, "b")
    if rows_total > rows:
        th = max(16, int(track_h * rows / rows_total))
        ty = py + GY + 1 + int((track_h - th) * scroll)
        c.bevel(sx + 1, ty, sx + 10, ty + th, "w")
    c.text(px + 10, py + GY + rows * CH + 2, status, "b", g.SMALL)
    iy = py + GY + rows * CH + 14
    ix = px + (W - 162) // 2
    c.text(ix, iy, "Inventory", "dark", g.SMALL)
    for r in range(3):
        for k in range(9):
            c.slot(ix + k * 18, iy + 10 + r * 18)
    for k in range(9):
        c.slot(ix + k * 18, iy + 10 + 3 * 18 + 4)
    for i, glyph in enumerate(["A-Z", "▼", "☰"]):
        bx, by = 2, py + 4 + i * 20
        c.bevel(bx, by, bx + 18, by + 18, "s")
        c.text(bx + (19 - c.textw(glyph, g.SMALL)) / 2, by + 5, glyph, "dark", g.SMALL)
    if tip:
        i, lines = tip
        r, k = divmod(i, COLS)
        v.tooltip(c, px + GX + k * CW + CW - 6, py + GY + r * CH + 20, lines)
    return c.img


def main():
    ae = v.ae
    B = lv.book
    names = ["AE2 Guide", "Dictionary of Spirits", "Theurgy", "Productive Bees", "Integrated Dynamics",
             "Mystical Agriculture", "EvilCraft", "My base notes", "Forbidden & Arcanus", "Occultism Rituals",
             "Pipez", "Silent Gear", "Apotheosis", "Ars Arcana", "Farm plans", "Powah", "Ender IO",
             "Croptopia", "Oritech"]
    icons = [lv.AE2_GUIDE, B("F", "I", "C"), B("R", "r"), B("Gr", "gr"), B("T", "t2", "C"), B("Bl", "bl"),
             B("R", "r", "C"), B("Yd", "dark", "P"), B("F", "I"), B("Gr", "gr", "C"), B("Bl", "bl", "C"),
             B("T", "t2"), B("R", "r"), B("F", "I", "G"), B("Yd", "dark"), B("Bl", "bl"), B("Gr", "gr"),
             B("T", "t2", "Y"), B("F", "I", "C")]
    library = [(ic, nm, None, False) for ic, nm in zip(icons, names)]
    armory = [(mr.SWORD, "Diamond Sword", 0.92, True), (mr.PICK, "Iron Pickaxe", 0.55, True),
              (mr.AXE, "Diamond Axe", 0.8, True), (mr.BOW, "Bow", 0.7, True),
              (mr.SHOVEL, "Netherite Shovel", 0.97, True), (mr.PICK, "Iron Pickaxe", 0.25, False),
              (mr.SWORD, "Iron Sword", 1.0, False)]
    tools = [(ae("certus_quartz_wrench"), "Certus Quartz Wrench", None, False),
             (ae("network_tool"), "Network Tool", None, False),
             (ae("memory_card_base"), "Memory Card", None, False),
             (ae("memory_card_base"), "Memory Card", None, False),
             (mr.SCREWDRIVER, "Configurator", None, False),
             (ae("nether_quartz_wrench"), "Nether Quartz Wrench", None, False),
             (ae("memory_card_base"), "Memory Card", None, False),
             (mr.SCREWDRIVER, "Configurator", None, False),
             (ae("network_tool"), "Network Tool", None, False),
             (ae("certus_quartz_wrench"), "Certus Quartz Wrench", None, False)]

    lib = view("Library", library, 24, "3 libraries · 19 of 24 books · scroll for more", scroll=0.0,
               tip=(4, [("Integrated Dynamics", "txt"), ("Universal · Library A1", "dim"),
                        ("Click: read", "C"), ("Shift-click: take", "C")]))
    arm = view("Armory", armory, 8, "1 armory · 7 of 8",
               tip=(4, [("Netherite Shovel", "txt"), ("Silk Touch", (180, 180, 255)), ("Durability 1980 / 2031", "dim"),
                        ("Armory B1", "dim"), ("Click: swap to hand", "C"), ("Shift-click: take", "C")]))
    tls = view("Tools", tools, 16, "2 tool racks · 10 of 16 · storing a wrench", held=(ae("certus_quartz_wrench"), 10),
               tip=(4, [("Configurator", "txt"), ("Tool Rack C1", "dim"),
                        ("Click: swap to hand", "C"), ("Shift-click: take", "C")]))

    font = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf", 16)
    small = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf", 14)
    pad = 24
    imgs = [(lib, "Library: 3 libraries (24 spots), 19 books"),
            (arm, "Armory: 1 armory with 7 tools = 1 empty spot"),
            (tls, "Tools: 2 tool racks, dropping a wrench into a free spot")]
    Wt = pad * 4 + sum(i.width for i, _ in imgs)
    Ht = 40 + max(i.height for i, _ in imgs) + 110
    sheet = Image.new("RGBA", (Wt, Ht), (52, 54, 66, 255))
    d = ImageDraw.Draw(sheet)
    x = pad
    for img, title in imgs:
        d.text((x, 14), title, fill=(240, 240, 245), font=font)
        sheet.alpha_composite(img, (x, 40))
        x += img.width + pad
    notes = ["5 items per row at triple size, each with its name underneath. Free spots are slot boxes; stored items have no box.",
             "Every block still holds 8, so spots run on across rows (1 armory = 5 + 3). The grid shows 3 rows, then scrolls.",
             "Click: read a book, or swap a tool or weapon to your hand. Shift-click: take it. Drop or shift-click from your inventory to store."]
    for i, n in enumerate(notes):
        d.text((pad, Ht - 95 + i * 22), n, fill=(210, 210, 222), font=small)
    sheet.save(os.path.join(OUT, "views_v4.png"))
    print(sheet.size)


if __name__ == "__main__":
    main()
