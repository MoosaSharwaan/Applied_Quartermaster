"""Automation screen (terminal style, matching the final views): farm list, and inside one farm."""
from PIL import Image, ImageDraw, ImageFont
import os
import make_open_gui as g
import make_views_v2 as v
import make_views_v4 as v4
import make_plate as pl
import make_controller_variants as cv
import mock_items as mi

OUT = os.path.dirname(os.path.abspath(__file__))
COLS, CW, CH = 5, 66, 80
GX, GY = 10, 40
PANEL_X, PANEL_Y = 24, 18
W = GX + COLS * CW + 20
GREEN, RED, GREY = (40, 150, 60), (190, 40, 40), (120, 120, 135)


def pad16(img):
    out = Image.new("RGBA", (16, 16))
    out.alpha_composite(img, ((16 - img.width) // 2, (16 - img.height) // 2))
    return out


FARM_ICON = cv.SWITCHBOARD


def tab_icons():
    return [("Library", g.ICON["library"]), ("Armory", v.tab_icon("Armory")), ("Tools", v.tab_icon("Tools")),
            ("Automation", FARM_ICON)]


def button(c, x, y, label, fill="w"):
    w = c.textw(label, g.SMALL) + 10
    c.bevel(x, y, x + w, y + 13, fill)
    c.text(x + 5, y + 2, label, "dark", g.SMALL)
    return w


def panel(title, cards, rows, header_buttons=(), back=False, status="", tip=None):
    H = GY + rows * CH + 18
    c = g.Canvas(PANEL_X + W + 100, PANEL_Y + H + 4)
    px, py = PANEL_X, PANEL_Y
    for i, (name, icon) in enumerate(tab_icons()):
        tx = px + 4 + i * 26
        on = name == "Automation"
        c.bevel(tx, py - 16 + (0 if on else 2), tx + 24, py + (1 if on else 0), "s" if on else "S")
        c.icon(icon, tx + 5, py - 13 + (0 if on else 2))
    c.bevel(px, py, px + W - 1, py + H - 1, "s")
    x = px + 10
    if back:
        bw = button(c, x, py + 5, "← Farms")
        x += bw + 6
    c.text(x, py + 7, title, "dark", g.FONTB)
    c.rect(px + W - 92, py + 6, px + W - 12, py + 16, "O"); c.rect(px + W - 91, py + 7, px + W - 13, py + 15, (236, 236, 240))
    c.text(px + W - 88, py + 7, "Search…", (150, 150, 165), g.SMALL)
    bx = px + 10
    for lab in header_buttons:
        bx += button(c, bx, py + 21, lab) + 4
    hover = tip[0] if tip else None
    for i, card in enumerate(cards):
        icon, name, sub, subcol = card[:4]
        badge = card[4] if len(card) > 4 else None
        r, k = divmod(i, COLS)
        x0, y0 = px + GX + k * CW, py + GY + r * CH
        ix = x0 + (CW - 50) // 2
        if i == hover:
            c.rect(x0 + 1, y0, x0 + CW - 2, y0 + CH - 3, (225, 226, 234))
        c.icon(icon, ix + 1, y0 + 2, scale=3)
        if badge:  # small switchboard badge in the corner, coloured by state
            bx0, by0 = ix + 36, y0 + 36
            c.rect(bx0, by0, bx0 + 13, by0 + 13, "O")
            c.rect(bx0 + 1, by0 + 1, bx0 + 12, by0 + 12, (33, 32, 52))
            col = {"on": (90, 230, 110), "off": (235, 60, 50), "offline": (70, 68, 90)}[badge]
            for gx in range(3):
                for gy in range(3):
                    c.rect(bx0 + 2 + gx * 4, by0 + 2 + gy * 4, bx0 + 3 + gx * 4, by0 + 3 + gy * 4, col)
        for j, line in enumerate(v4.wrap(c, name, CW - 4)[:1]):
            c.text(x0 + (CW - c.textw(line, v4.NAME)) / 2, y0 + 53, line, "dark", v4.NAME)
        c.text(x0 + (CW - c.textw(sub, v4.NAME)) / 2, y0 + 61, sub, subcol, v4.NAME)
    sx = px + GX + COLS * CW + 4
    c.rect(sx, py + GY, sx + 11, py + GY + rows * CH - 4, "B")
    c.rect(sx + 1, py + GY + 1, sx + 10, py + GY + rows * CH - 5, "b")
    c.text(px + 10, py + GY + rows * CH + 2, status, "b", g.SMALL)
    for i, glyph in enumerate(["A-Z", "▼"]):
        bx2, by = 2, py + 4 + i * 20
        c.bevel(bx2, by, bx2 + 18, by + 18, "s")
        c.text(bx2 + (19 - c.textw(glyph, g.SMALL)) / 2, by + 5, glyph, "dark", g.SMALL)
    if tip:
        i, lines = tip
        r, k = divmod(i, COLS)
        v.tooltip(c, px + GX + k * CW + CW - 6, py + GY + r * CH + 20, lines)
    return c.img


def main():
    offline_icon = cv.SWITCHBOARD.copy()
    off = {pl.P[c]: pl.P["o"] for c in "CjgU"}
    offline_icon.putdata([off.get(p, p) for p in offline_icon.get_flattened_data()])
    farms = [(mi.IRON_INGOT, "Iron Farm", "4 of 5 on", GREEN, "on"),
             (mi.ROTTEN_FLESH, "Mob Grinder", "2 of 6 on", GREEN, "on"),
             (mi.WHEAT, "Wheat Farm", "All off", RED, "off"),
             (mi.HONEYCOMB, "Bee Apiary", "3 of 3 on", GREEN, "on"),
             (mi.NETHER_WART, "Nether Farm", "Offline", GREY, "offline"),
             (FARM_ICON, "Slime Farm", "1 of 2 on", GREEN, None)]
    left = panel("Farms", farms, 2, status="6 farms · 22 plates · 1 offline",
                 tip=(4, [("Nether Farm", "txt"), ("Offline: no power or channel", "dim"),
                          ("Click: open farm", "C"), ("Hold an item + click: set icon", "C")]))
    on, offp = pad16(pl.plate_face("on")), pad16(pl.plate_face("off"))
    plates = [(mi.LAMP, "Spawner lights", "On \u00b7 15", GREEN, "on"), (mi.IRON_SWORD, "Golem killer", "Off", RED, "off"),
              (mi.HOPPER, "Collector", "Off", RED, "off"), (mi.LAVA, "Lava blade", "Off", RED, "off"),
              (mi.PISTON, "Crusher", "Off", RED, "off"), (mi.DOOR, "Door", "On \u00b7 8", GREEN, "on")]
    right = panel("Mob Grinder", plates, 2, header_buttons=("All on", "All off", "Rename"), back=True,
                  status="6 plates · 2 on · controller online",
                  tip=(4, [("Crusher", "txt"), ("Off · strength 15", "dim"),
                           ("Click: turn on", "C"), ("Scroll: signal strength", "C"), ("Right-click: rename or set icon", "C")]))
    font = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf", 16)
    small = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf", 14)
    pad = 24
    Wt = pad * 3 + left.width + right.width
    Ht = 40 + max(left.height, right.height) + 100
    sheet = Image.new("RGBA", (Wt, Ht), (52, 54, 66, 255))
    d = ImageDraw.Draw(sheet)
    d.text((pad, 14), "Automation: your farms", fill=(240, 240, 245), font=font)
    d.text((pad * 2 + left.width, 14), "Inside a farm: its Redstone Plates", fill=(240, 240, 245), font=font)
    sheet.alpha_composite(left, (pad, 40))
    sheet.alpha_composite(right, (pad * 2 + left.width, 40))
    notes = ["Custom icons: any item you choose (set on the controller, from the tablet, or dragged from JEI); the item is not used up.",
             "The corner badge keeps the status: green running, red off, dark offline. No icon set (Slime Farm) = the switchboard icon.",
             "Plates: click to turn on or off (light turns green or red), scroll to set strength, All on / All off for the whole farm."]
    for i, n in enumerate(notes):
        d.text((pad, Ht - 85 + i * 22), n, fill=(210, 210, 222), font=small)
    sheet.save(os.path.join(OUT, "automation_view_icons.png"))
    print(sheet.size)


if __name__ == "__main__":
    main()
