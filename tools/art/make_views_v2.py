"""Library / Armory / Tools views, v2: AE2 wireless-terminal style.
Light AE2 panel, scrollable 8-per-row grid, search box, tabs to switch view, left toolbar
(sort / filter), player inventory below so items can be dropped in to store, vanilla tooltips."""
from PIL import Image, ImageDraw, ImageFont
import os
import make_open_gui as g
import make_racks as mr
import make_library_view as lv

OUT = os.path.dirname(os.path.abspath(__file__))
C = g.C
AE2 = "/home/claude/refs/ae2/src/main/resources/assets/ae2/textures/item"
ae = lambda n: Image.open(f"{AE2}/{n}.png").convert("RGBA").crop((0, 0, 16, 16))
TT_BG, TT_BORDER = (16, 0, 16), (80, 0, 160)

PANEL_X, PANEL_Y = 24, 18          # panel offset inside the canvas (room for toolbar + tabs)
W, H = 178, 218                     # panel size in GUI pixels
COLS, ROWS = 8, 5
GX, GY = 8, 24                      # grid origin inside the panel


def glint(img):
    """Approximate the enchantment glint with a soft purple diagonal sheen."""
    out = img.copy()
    px = out.load()
    for y in range(16):
        for x in range(16):
            r, gg, b, a = px[x, y]
            if a and (x + y) % 6 in (0, 1):
                px[x, y] = (min(255, r + 60), min(255, gg + 20), min(255, b + 90), a)
    return out


def durability(c, x, y, frac):
    """Vanilla-style durability bar along the bottom of a slot."""
    col = (int(255 * (1 - frac)), int(255 * frac), 0)
    c.rect(x + 2, y + 13, x + 14, y + 14, (0, 0, 0))
    c.rect(x + 2, y + 13, x + 2 + int(12 * frac), y + 13, col)


def tooltip(c, x, y, lines):
    w = max(c.textw(t, g.SMALL) for t, _ in lines) + 8
    h = 4 + 9 * len(lines)
    c.rect(x, y, x + w, y + h, TT_BG)
    c.rect(x, y, x + w, y, TT_BORDER); c.rect(x, y + h, x + w, y + h, TT_BORDER)
    c.rect(x, y, x, y + h, TT_BORDER); c.rect(x + w, y, x + w, y + h, TT_BORDER)
    for i, (t, col) in enumerate(lines):
        c.text(x + 4, y + 2 + i * 9, t, col, g.SMALL)


def tab_icon(kind):
    return {"Library": g.ICON["library"], "Armory": mr.SWORD, "Tools": ae("certus_quartz_wrench")}[kind]


def view(kind, items, capacity, status, tip=None, held=None):
    """capacity = total slots across all blocks of this kind (8 per block); only those slots are drawn."""
    total_rows = -(-capacity // COLS)
    ROWS = min(total_rows, 5)          # visible rows; more than 5 scrolls
    H = GY + ROWS * 18 + 15 + 9 + 3 * 18 + 4 + 18 + 8
    cw, ch = PANEL_X + W + 84, PANEL_Y + max(H, 120) + 4
    c = g.Canvas(cw, ch)
    px, py = PANEL_X, PANEL_Y
    # tabs above the panel (like creative-inventory tabs)
    for i, name in enumerate(["Library", "Armory", "Tools"]):
        tx = px + 4 + i * 26
        on = name == kind
        c.bevel(tx, py - 16 + (0 if on else 2), tx + 24, py + (1 if on else 0), "s" if on else "S")
        c.icon(tab_icon(name), tx + 5, py - 13 + (0 if on else 2))
    # panel
    c.bevel(px, py, px + W - 1, py + H - 1, "s")
    c.text(px + 8, py + 6, {"Library": "ME Library", "Armory": "ME Armory", "Tools": "ME Tool Rack"}[kind], "dark", g.FONTB)
    # search field
    c.rect(px + 98, py + 5, px + 169, py + 15, "O"); c.rect(px + 99, py + 6, px + 168, py + 14, (236, 236, 240))
    c.text(px + 102, py + 6, "Search…", (150, 150, 165), g.SMALL)
    # grid
    for r in range(ROWS):
        for k in range(COLS):
            i = r * COLS + k
            if i >= capacity:
                continue
            x, y = px + GX + k * 18, py + GY + r * 18
            it = items[i] if i < len(items) else None
            c.slot(x, y, item=None)
            if it:
                icon, dur, ench = it[0], it[1], it[2]
                c.icon(glint(icon) if ench else icon, x + 1, y + 1)
                if dur is not None and dur < 1:
                    durability(c, x, y, dur)
    # scrollbar
    sx = px + GX + COLS * 18 + 4
    c.rect(sx, py + GY, sx + 11, py + GY + ROWS * 18 - 1, "B")
    c.rect(sx + 1, py + GY + 1, sx + 10, py + GY + ROWS * 18 - 2, "b")
    if total_rows > ROWS:
        c.bevel(sx + 1, py + GY + 1, sx + 10, py + GY + 16, "w")
    # status line
    c.text(px + 8, py + GY + ROWS * 18 + 3, status, "b", g.SMALL)
    # inventory
    iy = py + GY + ROWS * 18 + 15
    c.text(px + 8, iy, "Inventory", "dark", g.SMALL)
    for r in range(3):
        for k in range(9):
            c.slot(px + 8 + k * 18, iy + 9 + r * 18)
    for k in range(9):
        c.slot(px + 8 + k * 18, iy + 9 + 3 * 18 + 4)
    # left toolbar (AE2-style buttons): sort, filter, view
    for i, glyph in enumerate(["A-Z", "▼", "☰"]):
        bx, by = 2, py + 4 + i * 20
        c.bevel(bx, by, bx + 18, by + 18, "s")
        c.text(bx + (19 - c.textw(glyph, g.SMALL)) / 2, by + 5, glyph, "dark", g.SMALL)
    # a held item being dropped in (store)
    if held:
        icon, slot_i = held
        k, r = slot_i % COLS, slot_i // COLS
        x, y = px + GX + k * 18, py + GY + r * 18
        c.rect(x + 1, y + 1, x + 16, y + 16, (200, 220, 255))
        c.icon(icon, x + 5, y + 5)
    if tip:
        slot_i, lines = tip
        k, r = slot_i % COLS, slot_i // COLS
        tooltip(c, px + GX + k * 18 + 14, py + GY + r * 18 + 10, lines)
    return c.img


def main():
    B = lv.book
    lib_books = [lv.AE2_GUIDE, B("F", "I", "C"), B("R", "r"), B("Gr", "gr"), B("T", "t2", "C"), B("Bl", "bl"),
                 B("R", "r", "C"), B("Yd", "dark", "P"), B("F", "I"), B("Gr", "gr", "C"), B("Bl", "bl", "C"),
                 B("T", "t2"), B("R", "r"), B("F", "I", "G"), B("Yd", "dark"), B("Bl", "bl"), B("Gr", "gr"),
                 B("T", "t2", "Y"), B("F", "I", "C")]
    library = [(b, None, False) for b in lib_books]
    armory = [(mr.SWORD, 0.92, True), (mr.PICK, 0.55, True), (mr.AXE, 0.8, True), (mr.BOW, 0.7, True),
              (mr.SHOVEL, 0.97, True), (mr.PICK, 0.25, True), (mr.SWORD, 0.4, False), (mr.AXE, 1.0, False),
              (mr.PICK, 0.9, False), (mr.SHOVEL, 0.6, False), (mr.BOW, 1.0, False), (mr.SWORD, 1.0, True),
              (mr.PICK, 0.15, False)]
    tools = [(ae("certus_quartz_wrench"), None, False), (ae("network_tool"), None, False),
             (ae("memory_card_base"), None, False), (ae("memory_card_base"), None, False),
             (mr.SCREWDRIVER, None, False), (ae("nether_quartz_wrench"), None, False),
             (ae("memory_card_base"), None, False), (ae("certus_quartz_wrench"), None, False),
             (mr.SCREWDRIVER, None, False), (ae("network_tool"), None, False)]

    v1 = view("Library", library, 24, "3 libraries · 19 of 24 books", tip=(7, [
        ("My base notes", "txt"), ("Written book · Library A1", "dim"),
        ("Click: read", "C"), ("Shift-click: take", "C")]))
    v2_ = view("Armory", armory, 16, "2 armories · 13 of 16", tip=(7, [
        ("Diamond Axe", "txt"), ("Durability 1561 / 1561", "dim"),
        ("Armory B1", "dim"), ("Click: swap to hand", "C"), ("Shift-click: take", "C")]))
    v3 = view("Tools", tools, 16, "2 tool racks · 10 of 16", tip=(7, [
        ("Certus Quartz Wrench", "txt"), ("Tool Rack C1", "dim"),
        ("Click: swap to hand", "C"), ("Shift-click: take", "C")]),
        held=(ae("certus_quartz_wrench"), 12))

    font = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf", 16)
    small = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf", 14)
    pad = 24
    hmax = max(v1.height, v2_.height, v3.height)
    sheet = Image.new("RGBA", (pad * 4 + v1.width * 3, hmax + 170), (52, 54, 66, 255))
    d = ImageDraw.Draw(sheet)
    for i, (img, title) in enumerate([(v1, "Library"), (v2_, "Armory"), (v3, "Tools (storing a wrench)")]):
        x = pad + i * (v1.width + pad)
        d.text((x, 14), title, fill=(240, 240, 245), font=font)
        sheet.alpha_composite(img, (x, 40))
    notes = ["Only real slots are shown: 8 per row, one row per block (3 libraries = 3 rows). Over 5 rows, the grid scrolls.",
             "Store: drop any item from your inventory into the grid (or shift-click it) and it goes into the first free spot.",
             "Click: read a book, or swap a tool/weapon to your hand. Shift-click: take it to your inventory.",
             "Hover for details: enchantments, durability, saved memory-card settings, and which block holds it.",
             "Left buttons: sort (A-Z, by mod, by durability), filter, and view options, like AE2's terminal."]
    for i, n in enumerate(notes):
        d.text((pad, 40 + hmax + 10 + i * 21), n, fill=(210, 210, 222), font=small)
    sheet.save(os.path.join(OUT, "views_v2.png"))
    print(sheet.size)


if __name__ == "__main__":
    main()
