"""Opened tablet, reworked from the user's sketch: the terminal-style screen IS the tablet.
Tabs along the top = installed modules (empty tab slots for modules not yet inserted),
a Modules button at the top right opens the Modules page (also terminal style)."""
from PIL import Image, ImageDraw, ImageFont
import os
import make_open_gui as g
import make_views_v2 as v
import make_views_v4 as v4
import make_automation_view2 as av
import mock_items as mi

OUT = os.path.dirname(os.path.abspath(__file__))
COLS, CW, CH = av.COLS, av.CW, av.CH
GX, GY = av.GX, av.GY
PANEL_X, PANEL_Y = 24, 20
W = av.W
TAB_SLOTS = 7


def gear():
    im = Image.new("RGBA", (16, 16))
    d = ImageDraw.Draw(im)
    s, o, f = g.C["s"] + (255,), g.C["O"] + (255,), (172, 233, 255, 255)
    for (a, b) in [(7, 0), (7, 13), (0, 7), (13, 7), (2, 2), (12, 2), (2, 12), (12, 12)]:
        d.rectangle([a, b, a + 2, b + 2], fill=s, outline=o)
    d.ellipse([2, 2, 13, 13], fill=s, outline=o)
    d.ellipse([5, 5, 10, 10], fill=f, outline=o)
    return im


GEAR = gear()
INSTALLED = [("Library", g.ICON["library"]), ("Armory", v.tab_icon("Armory")),
             ("Tools", v.tab_icon("Tools")), ("Automation", av.FARM_ICON)]


def chrome(c, px, py, active, pinned="Automation"):
    """Tab row (installed modules + empty slots) and the Modules button."""
    for i in range(TAB_SLOTS):
        tx = px + 4 + i * 26
        if i < len(INSTALLED):
            name, icon = INSTALLED[i]
            on = name == active
            c.bevel(tx, py - 16 + (0 if on else 2), tx + 24, py + (1 if on else 0), "s" if on else "S")
            c.icon(icon, tx + 5, py - 13 + (0 if on else 2))
            if name == pinned:
                g.pin(c, tx + 17, py - 15 + (0 if on else 2))
    # Modules button, top right
    mx = px + W - 26
    on = active == "Modules"
    c.bevel(mx, py - 16 + (0 if on else 2), mx + 24, py + (1 if on else 0), "s" if on else "S")
    c.icon(GEAR, mx + 5, py - 13 + (0 if on else 2))


def automation_home():
    img = av.panel("Farms", [], 2)  # placeholder to reuse sizes
    rows = 2
    H = GY + rows * CH + 18
    c = g.Canvas(PANEL_X + W + 100, PANEL_Y + H + 4)
    px, py = PANEL_X, PANEL_Y
    chrome(c, px, py, "Automation")
    c.bevel(px, py, px + W - 1, py + H - 1, "s")
    c.text(px + 10, py + 7, "Farms", "dark", g.FONTB)
    c.rect(px + W - 92, py + 6, px + W - 12, py + 16, "O"); c.rect(px + W - 91, py + 7, px + W - 13, py + 15, (236, 236, 240))
    c.text(px + W - 88, py + 7, "Search…", (150, 150, 165), g.SMALL)
    farms = [(mi.IRON_INGOT, "Iron Farm", "4 of 5 on", av.GREEN, "on"),
             (mi.ROTTEN_FLESH, "Mob Grinder", "2 of 6 on", av.GREEN, "on"),
             (mi.WHEAT, "Wheat Farm", "All off", av.RED, "off"),
             (mi.HONEYCOMB, "Bee Apiary", "3 of 3 on", av.GREEN, "on"),
             (mi.NETHER_WART, "Nether Farm", "Offline", av.GREY, "offline"),
             (mi.SLIME_BALL, "Slime Farm", "1 of 2 on", av.GREEN, "on")]
    draw_cards(c, px, py, farms)
    scroll(c, px, py, rows)
    c.text(px + 10, py + GY + rows * CH + 2, "6 farms · 22 plates · 1 offline", "b", g.SMALL)
    toolbar(c, py)
    v.tooltip(c, px + W + 2, py - 4, [("Modules", "txt"), ("Insert or remove modules,", "dim"), ("pick the default tab", "dim")])
    return c.img


def draw_cards(c, px, py, cards, boxes_after=0):
    for i, card in enumerate(cards):
        r, k = divmod(i, COLS)
        x0, y0 = px + GX + k * CW, py + GY + r * CH
        ix = x0 + (CW - 50) // 2
        if card is None:
            v4.box(c, ix, y0 + 1)
            c.text(x0 + (CW - c.textw("Empty", v4.NAME)) / 2, y0 + 53, "Empty", "b", v4.NAME)
            continue
        icon, name, sub, subcol, badge = card
        c.icon(icon, ix + 1, y0 + 2, scale=3)
        if badge in ("on", "off", "offline"):
            bx0, by0 = ix + 36, y0 + 36
            c.rect(bx0, by0, bx0 + 13, by0 + 13, "O"); c.rect(bx0 + 1, by0 + 1, bx0 + 12, by0 + 12, (33, 32, 52))
            col = {"on": (90, 230, 110), "off": (235, 60, 50), "offline": (70, 68, 90)}[badge]
            for gx in range(3):
                for gy in range(3):
                    c.rect(bx0 + 2 + gx * 4, by0 + 2 + gy * 4, bx0 + 3 + gx * 4, by0 + 3 + gy * 4, col)
        elif badge == "pin":
            g.pin(c, ix + 40, y0 + 2)
        line = v4.wrap(c, name, CW - 4)[0]
        c.text(x0 + (CW - c.textw(line, v4.NAME)) / 2, y0 + 53, line, "dark", v4.NAME)
        c.text(x0 + (CW - c.textw(sub, v4.NAME)) / 2, y0 + 61, sub, subcol, v4.NAME)


def scroll(c, px, py, rows):
    sx = px + GX + COLS * CW + 4
    c.rect(sx, py + GY, sx + 11, py + GY + rows * CH - 4, "B")
    c.rect(sx + 1, py + GY + 1, sx + 10, py + GY + rows * CH - 5, "b")


def toolbar(c, py):
    for i, glyph in enumerate(["A-Z", "▼"]):
        bx2, by = 2, py + 4 + i * 20
        c.bevel(bx2, by, bx2 + 18, by + 18, "s")
        c.text(bx2 + (19 - c.textw(glyph, g.SMALL)) / 2, by + 5, glyph, "dark", g.SMALL)


def modules_page():
    rows = 2
    H = GY + rows * CH + 14 + 10 + 3 * 18 + 4 + 18 + 8
    c = g.Canvas(PANEL_X + W + 100, PANEL_Y + H + 4)
    px, py = PANEL_X, PANEL_Y
    chrome(c, px, py, "Modules")
    c.bevel(px, py, px + W - 1, py + H - 1, "s")
    c.text(px + 10, py + 7, "Modules", "dark", g.FONTB)
    c.text(px + 70, py + 8, "· pin = opens first", "b", g.SMALL)
    # upgrades + battery in the header
    c.text(px + W - 120, py + 8, "Upgrades", "b", g.SMALL)
    c.slot(px + W - 82, py + 4, item=g.AE["booster"]); c.slot(px + W - 64, py + 4)
    c.rect(px + W - 42, py + 6, px + W - 12, py + 19, "O"); c.rect(px + W - 41, py + 7, px + W - 13, py + 18, "S")
    c.rect(px + W - 41, py + 7, px + W - 18, py + 18, (110, 214, 140))
    mods = [(g.ICON["library"], "Library", "Installed", av.GREY, None),
            (v.tab_icon("Armory"), "Armory", "Installed", av.GREY, None),
            (v.tab_icon("Tools"), "Tools", "Installed", av.GREY, None),
            (av.FARM_ICON, "Automation", "Default", (150, 110, 20), "pin"),
            None, None, None]
    draw_cards(c, px, py, mods)
    c.text(px + 10, py + GY + rows * CH + 2, "4 of 7 module slots used", "b", g.SMALL)
    iy = py + GY + rows * CH + 14
    ix = px + (W - 162) // 2
    c.text(ix, iy, "Inventory", "dark", g.SMALL)
    for r in range(3):
        for k in range(9):
            c.slot(ix + k * 18, iy + 10 + r * 18)
    for k in range(9):
        c.slot(ix + k * 18, iy + 10 + 3 * 18 + 4)
    return c.img


def main():
    a, b = automation_home(), modules_page()
    font = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf", 16)
    small = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf", 14)
    pad = 24
    sheet = Image.new("RGBA", (pad * 3 + a.width + b.width, 40 + max(a.height, b.height) + 110), (52, 54, 66, 255))
    d = ImageDraw.Draw(sheet)
    d.text((pad, 14), "1. Opening the tablet: the pinned tab opens first", fill=(240, 240, 245), font=font)
    d.text((pad * 2 + a.width, 14), "2. Modules button: insert modules, pin the default", fill=(240, 240, 245), font=font)
    sheet.alpha_composite(a, (pad, 40))
    sheet.alpha_composite(b, (pad * 2 + a.width, 40))
    notes = ["Tabs = installed modules only. A tab appears when its module is added and disappears when it is removed.",
             "The gold pin marks the default tab, which opens when you open the tablet. The gear button opens the Modules page.",
             "Modules page uses the same style: big icons with names, boxes for empty module slots, upgrades and battery in the header."]
    for i, n in enumerate(notes):
        d.text((pad, sheet.height - 90 + i * 22), n, fill=(210, 210, 222), font=small)
    sheet.save(os.path.join(OUT, "tablet_shell_v2.png"))
    print(sheet.size)


if __name__ == "__main__":
    main()


# ---------------------------------------------------------------- view-size button
def size_icon(c, x, y, size):
    """View-size glyph: large = 2x2 big squares, medium = 3x3 squares, small = 4x4 dots."""
    col = g.C["dark"]
    n, step, dot = {"large": (2, 5, 3), "medium": (3, 4, 2), "small": (4, 3, 1)}[size]
    for gx in range(n):
        for gy in range(n):
            c.rect(x + gx * step, y + gy * step, x + gx * step + dot, y + gy * step + dot, col)


def toolbar3(c, py, size, highlight=False):
    for i, glyph in enumerate(["A-Z", "▼"]):
        bx2, by = 2, py + 4 + i * 20
        c.bevel(bx2, by, bx2 + 18, by + 18, "s")
        c.text(bx2 + (19 - c.textw(glyph, g.SMALL)) / 2, by + 5, glyph, "dark", g.SMALL)
    bx2, by = 2, py + 44
    c.bevel(bx2, by, bx2 + 18, by + 18, "w" if highlight else "s")
    size_icon(c, bx2 + 4, by + 4, size)


def library_view(size):
    import make_library_view as lv
    B = lv.book
    names = ["AE2 Guide", "Dictionary of Spirits", "Theurgy", "Productive Bees", "Integrated Dynamics",
             "Mystical Agriculture", "EvilCraft", "My base notes", "Forbidden & Arcanus", "Occultism Rituals",
             "Pipez", "Silent Gear", "Apotheosis", "Ars Arcana", "Farm plans", "Powah", "Ender IO",
             "Croptopia", "Oritech"]
    icons = [lv.AE2_GUIDE, B("F", "I", "C"), B("R", "r"), B("Gr", "gr"), B("T", "t2", "C"), B("Bl", "bl"),
             B("R", "r", "C"), B("Yd", "dark", "P"), B("F", "I"), B("Gr", "gr", "C"), B("Bl", "bl", "C"),
             B("T", "t2"), B("R", "r"), B("F", "I", "G"), B("Yd", "dark"), B("Bl", "bl"), B("Gr", "gr"),
             B("T", "t2", "Y"), B("F", "I", "C")]
    capacity = 24
    rows = 3
    H = GY + rows * CH + 18
    c = g.Canvas(PANEL_X + W + 100, PANEL_Y + H + 4)
    px, py = PANEL_X, PANEL_Y
    chrome(c, px, py, "Library")
    c.bevel(px, py, px + W - 1, py + H - 1, "s")
    c.text(px + 10, py + 7, "Library", "dark", g.FONTB)
    c.rect(px + W - 92, py + 6, px + W - 12, py + 16, "O"); c.rect(px + W - 91, py + 7, px + W - 13, py + 15, (236, 236, 240))
    c.text(px + W - 88, py + 7, "Search…", (150, 150, 165), g.SMALL)
    if size == "large":
        cards = [(ic, nm, "", av.GREY, None) for ic, nm in zip(icons, names)]
        shown = cards[:COLS * rows]
        draw_cards(c, px, py, shown)
        status = "Large view · 5 per row with names · 19 of 24 books (scroll for more)"
    elif size == "medium":
        cols, cell = 8, 41
        for i in range(capacity):
            r, k = divmod(i, cols)
            x0, y0 = px + GX + 4 + k * cell, py + GY + r * cell
            if i < len(icons):
                c.icon(icons[i], x0 + 2, y0 + 2, scale=2)
            else:
                c.rect(x0, y0, x0 + 35, y0 + 35, "W"); c.rect(x0, y0, x0 + 34, y0 + 34, "B"); c.rect(x0 + 1, y0 + 1, x0 + 34, y0 + 34, "S")
        status = "Medium view \u00b7 8 per row, names on hover \u00b7 19 of 24 books, all on one screen"
        v.tooltip(c, px + GX + 4 + 7 * cell + 30, py + GY + 20, [("Theurgy", "txt"), ("Modonomicon \u00b7 Library A1", "dim"),
                                                                  ("Click: read", "C"), ("Shift-click: take", "C")])
    else:
        cols, cell = 17, 19
        for i in range(capacity):
            r, k = divmod(i, cols)
            x0, y0 = px + GX + k * cell, py + GY + r * cell
            if i < len(icons):
                c.icon(icons[i], x0 + 1, y0 + 1)
            else:
                c.slot(x0, y0)
        status = "Small view · 17 per row, names on hover · 19 of 24 books, all on one screen"
        v.tooltip(c, px + GX + 2 * cell + 16, py + GY + 44, [("Theurgy", "txt"), ("Modonomicon · Library A1", "dim"),
                                                              ("Click: read", "C"), ("Shift-click: take", "C")])
    scroll(c, px, py, rows)
    c.text(px + 10, py + GY + rows * CH + 2, status, "b", g.SMALL)
    toolbar3(c, py, size, highlight=True)
    return c.img


def size_sheet():
    views = [(library_view("large"), "Large (default): 5 per row, names"),
             (library_view("medium"), "Medium: 8 per row, double size"),
             (library_view("small"), "Small: 17 per row, AE2 size")]
    font = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf", 16)
    small = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf", 14)
    pad = 20
    wsum = sum(im.width for im, _ in views)
    sheet = Image.new("RGBA", (pad * 4 + wsum, 40 + views[0][0].height + 80), (52, 54, 66, 255))
    d = ImageDraw.Draw(sheet)
    x = pad
    for im, title in views:
        d.text((x, 14), title, fill=(240, 240, 245), font=font)
        sheet.alpha_composite(im, (x, 40))
        x += im.width + pad
    notes = ["The view-size button (third on the left) cycles Large \u2192 Medium \u2192 Small; its icon shows the current size.",
             "Same in every tab, remembered per tab. Medium and Small show names on hover."]
    for i, n in enumerate(notes):
        d.text((pad, sheet.height - 62 + i * 22), n, fill=(210, 210, 222), font=small)
    sheet.save(os.path.join(OUT, "view_size_toggle.png"))
    print(sheet.size)


if __name__ == "__main__":
    size_sheet()
