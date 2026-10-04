"""Mockup of the ME Tablet's opened screens, drawn at Minecraft GUI-pixel scale:
  1. Home screen (apps unlocked by inserted modules, default app pinned)
  2. Modules page (AE2-style panel with module slots, default selector, upgrades, battery, inventory)
AE2 item icons in the module slots are only for the mockup (in game they are the player's real items)."""
from PIL import Image, ImageDraw, ImageFont
import os

OUT = os.path.dirname(os.path.abspath(__file__))
AE2 = "/home/claude/refs/ae2/src/main/resources/assets/ae2/textures"
S = 2  # screen pixels per GUI pixel

C = {
    "O": (65, 63, 84), "o": (77, 77, 103), "W": (242, 242, 242), "w": (222, 223, 227),
    "s": (203, 204, 212), "S": (173, 176, 196), "B": (154, 159, 180), "b": (135, 143, 165),
    "F": (145, 93, 205), "A": (176, 111, 221), "G": (226, 163, 227), "I": (52, 40, 96),
    "i": (40, 30, 76), "t": (70, 56, 128), "C": (172, 233, 255), "Y": (240, 196, 72),
    "g": (110, 214, 140), "txt": (235, 232, 245), "dim": (150, 140, 185), "dark": (64, 64, 84),
}
FONT = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf", 6 * S + 1)
FONTB = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf", 6 * S + 1)
SMALL = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf", 5 * S + 1)


class Canvas:
    def __init__(self, w, h, bg=(0, 0, 0, 0)):
        self.img = Image.new("RGBA", (w * S, h * S), bg)
        self.d = ImageDraw.Draw(self.img)

    def rect(self, x0, y0, x1, y1, col):
        self.d.rectangle([x0 * S, y0 * S, x1 * S + S - 1, y1 * S + S - 1], fill=C[col] if isinstance(col, str) else col)

    def bevel(self, x0, y0, x1, y1, fill, hi="W", lo="b", outline="O"):
        self.rect(x0, y0, x1, y1, outline)
        self.rect(x0 + 1, y0 + 1, x1 - 1, y1 - 1, lo)
        self.rect(x0 + 1, y0 + 1, x1 - 2, y1 - 2, hi)
        self.rect(x0 + 2, y0 + 2, x1 - 2, y1 - 2, fill)

    def slot(self, x, y, item=None, ghost=None):
        """18x18 Minecraft-style slot at GUI coords (x, y)."""
        self.rect(x, y, x + 17, y + 17, "W")
        self.rect(x, y, x + 16, y + 16, "B")
        self.rect(x + 1, y + 1, x + 16, y + 16, "S")
        if ghost is not None:
            g = ghost.convert("LA").convert("RGBA")
            g.putalpha(g.getchannel("A").point(lambda a: a * 70 // 255))
            self.icon(g, x + 1, y + 1)
        if item is not None:
            self.icon(item, x + 1, y + 1)

    def icon(self, tex, x, y, scale=1):
        im = tex.crop((0, 0, 16, 16)).resize((16 * S * scale, 16 * S * scale), Image.NEAREST)
        self.img.alpha_composite(im, (x * S, y * S))

    def text(self, x, y, s, col="txt", font=None):
        self.d.text((x * S, y * S), s, fill=C[col] if isinstance(col, str) else col, font=font or FONT)

    def textw(self, s, font=None):
        return self.d.textlength(s, font=font or FONT) / S


def tex(path):
    return Image.open(path).convert("RGBA")


ICON = {k: tex(os.path.join(OUT, f"app_{k}.png")) for k in
        ["storage", "crafting", "network", "devices", "alerts", "library", "tools"]}
AE = {
    "wireless": tex(f"{AE2}/item/wireless_terminal.png"),
    "crafting": tex(f"{AE2}/item/wireless_crafting_terminal.png"),
    "tool": tex(f"{AE2}/item/network_tool.png"),
    "redstone": tex(f"{AE2}/item/card_redstone.png"),
    "emitter": tex(f"{AE2}/part/level_emitter_off.png"),
    "guide": tex(f"{AE2}/item/guide.png"),
    "booster": tex(f"{AE2}/item/wireless_booster.png"),
    "wrench": tex(f"{AE2}/item/certus_quartz_wrench.png"),
    "memory": tex(f"{AE2}/item/memory_card_base.png"),
}


def pin(c, x, y, on=True):
    """Small 7x7 pin badge (default app marker)."""
    col = "Y" if on else "b"
    c.rect(x + 2, y, x + 4, y + 3, col)
    c.rect(x + 1, y + 3, x + 5, y + 3, col)
    c.rect(x + 3, y + 4, x + 3, y + 6, "O" if on else "b")


def padlock(c, x, y):
    c.rect(x + 1, y, x + 4, y, "dim"); c.rect(x + 1, y, x + 1, y + 2, "dim"); c.rect(x + 4, y, x + 4, y + 2, "dim")
    c.rect(x, y + 3, x + 5, y + 7, "dim"); c.rect(x + 2, y + 4, x + 3, y + 5, "i")


# ---------------------------------------------------------------- screen 1: home
def home_screen():
    W, H = 252, 150
    c = Canvas(W, H + 10)
    oy = 10
    # antenna
    c.rect(226, 0, 228, 2, "A"); c.rect(227, 3, 227, oy, "O")
    # tablet body
    c.rect(1, oy, W - 2, oy + H - 1, "O")
    c.rect(0, oy + 1, W - 1, oy + H - 2, "O")
    c.rect(2, oy + 1, W - 3, oy + H - 2, "s")
    c.rect(2, oy + 1, W - 4, oy + 2, "W"); c.rect(2, oy + 1, 3, oy + H - 4, "W")
    c.rect(W - 4, oy + 3, W - 3, oy + H - 2, "S"); c.rect(3, oy + H - 4, W - 3, oy + H - 2, "B")
    # screen
    sx0, sy0, sx1, sy1 = 9, oy + 8, W - 10, oy + H - 17
    c.rect(sx0 - 1, sy0 - 1, sx1 + 1, sy1 + 1, "F")
    c.rect(sx0, sy0, sx1, sy1, "I")
    for y in range(sy0 + 14, sy1, 3):
        c.rect(sx0, y, sx1, y, "i")
    # status bar
    c.rect(sx0, sy0, sx1, sy0 + 11, "i")
    c.text(sx0 + 4, sy0 + 2, "Main base", font=FONTB)
    c.text(sx0 + 46, sy0 + 2, "Network online · 1.2M AE/t", "dim")
    for k in range(4):  # signal bars
        c.rect(sx1 - 46 + k * 3, sy0 + 8 - k * 2, sx1 - 45 + k * 3, sy0 + 8, "C")
    c.rect(sx1 - 30, sy0 + 3, sx1 - 14, sy0 + 8, "dim"); c.rect(sx1 - 29, sy0 + 4, sx1 - 18, sy0 + 7, "g")
    c.rect(sx1 - 13, sy0 + 5, sx1 - 12, sy0 + 6, "dim")
    c.text(sx1 - 10, sy0 + 2, "", "dim")
    # app tiles: 4 x 2
    apps = [("storage", "Storage", True, True), ("crafting", "Crafting", True, False),
            ("network", "Network", True, False), ("library", "Library", True, False),
            ("devices", "Devices", False, False), ("alerts", "Alerts", False, False),
            ("tools", "Tools", True, False), ("modules", "Modules", True, False)]
    tw, th, gx, gy = 52, 50, 4, 5
    gx0 = sx0 + (sx1 - sx0 + 1 - (4 * tw + 3 * gx)) // 2
    gy0 = sy0 + 16
    for i, (key, label, installed, default) in enumerate(apps):
        r, col = divmod(i, 4)
        x, y = gx0 + col * (tw + gx), gy0 + r * (th + gy)
        c.rect(x, y, x + tw - 1, y + th - 1, "t" if installed else "i")
        c.rect(x, y, x + tw - 1, y, "F" if installed else "t")
        if key == "modules":
            gear = Image.new("RGBA", (16, 16))
            gd = ImageDraw.Draw(gear)
            gd.ellipse([2, 2, 13, 13], fill=C["s"] + (255,), outline=C["O"] + (255,))
            gd.ellipse([5, 5, 10, 10], fill=C["F"] + (255,), outline=C["O"] + (255,))
            for (a, b2) in [(7, 0), (7, 14), (0, 7), (14, 7)]:
                gd.rectangle([a, b2, a + 1, b2 + 1], fill=C["s"] + (255,))
            c.icon(gear, x + 10, y + 6, scale=2)
        elif installed:
            c.icon(ICON[key], x + 10, y + 6, scale=2)
        else:
            g = ICON[key].convert("LA").convert("RGBA")
            g.putalpha(g.getchannel("A").point(lambda a: a * 90 // 255))
            c.icon(g, x + 10, y + 6, scale=2)
            padlock(c, x + tw - 9, y + 3)
        lab_col = "txt" if installed else "dim"
        c.text(x + (tw - c.textw(label, SMALL)) / 2, y + th - 11, label, lab_col, SMALL)
        if default:
            pin(c, x + 3, y + 3)
    # bottom bezel: home button and hint
    c.rect(W // 2 - 8, oy + H - 12, W // 2 + 7, oy + H - 9, "F")
    c.rect(W // 2 - 8, oy + H - 12, W // 2 + 7, oy + H - 12, "A")
    return c.img


# ---------------------------------------------------------------- screen 2: modules
def modules_screen():
    W, H = 194, 300
    c = Canvas(W, H)
    c.bevel(0, 0, W - 1, H - 1, "s")
    c.text(8, 5, "ME Tablet — Modules", "dark", FONTB)
    rows = [("Storage", "Wireless Terminal", AE["wireless"], True, True),
            ("Crafting", "Wireless Crafting Terminal", AE["crafting"], True, False),
            ("Network", "Network Tool", AE["tool"], True, False),
            ("Devices", "Redstone Card", AE["redstone"], False, False),
            ("Alerts", "ME Level Emitter", AE["emitter"], False, False),
            ("Library", "AE2 Guide", AE["guide"], True, False)]
    c.text(8, 18, "Slot", "b", SMALL); c.text(30, 18, "App it unlocks", "b", SMALL); c.text(W - 30, 18, "Default", "b", SMALL)
    y = 26
    for app, item_name, tx, filled, default in rows:
        c.slot(8, y, item=tx if filled else None, ghost=None if filled else tx)
        c.text(30, y + 1, app, "dark", FONTB)
        c.text(30, y + 9, ("" if filled else "Insert: ") + item_name, "b" if not filled else "dark", SMALL)
        # default selector: pin button
        c.bevel(W - 26, y + 2, W - 13, y + 15, "s" if not default else "w")
        pin(c, W - 22, y + 5, on=default)
        y += 20
    # home option
    c.text(30, y + 2, "Home screen", "dark", FONTB)
    c.text(30, y + 10, "Open to the app grid instead", "b", SMALL)
    c.bevel(W - 26, y + 3, W - 13, y + 16, "s"); pin(c, W - 22, y + 6, on=False)
    y += 22
    # upgrades + battery + tool slots
    c.rect(6, y, W - 7, y, "B"); y += 4
    c.text(8, y, "Upgrades", "dark", SMALL)
    c.slot(8, y + 8, item=AE["booster"]); c.slot(26, y + 8)
    c.text(72, y, "Tool slots", "dark", SMALL)
    c.slot(72, y + 8, item=AE["wrench"]); c.slot(90, y + 8, item=AE["memory"]); c.slot(108, y + 8); c.slot(126, y + 8)
    c.text(152, y, "Battery", "dark", SMALL)
    c.rect(152, y + 8, 186, y + 25, "O"); c.rect(153, y + 9, 185, y + 24, "S")
    c.rect(153, y + 9 + 4, 185, y + 24, "g")
    y += 32
    # player inventory
    c.text(8, y, "Inventory", "dark", SMALL)
    y += 9
    for r in range(3):
        for k in range(9):
            c.slot(8 + k * 18, y + r * 18)
    y += 3 * 18 + 4
    for k in range(9):
        c.slot(8 + k * 18, y, item=tex(os.path.join(OUT, "me_tablet.png")) if k == 0 else None)
    return c.img


def main():
    h, m = home_screen(), modules_screen()
    font = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf", 16)
    small = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf", 14)
    pad = 28
    Wt = pad * 3 + h.width + m.width
    Ht = max(h.height, m.height) + 90
    sheet = Image.new("RGBA", (Wt, Ht), (52, 54, 66, 255))  # dimmed game background behind the GUI
    d = ImageDraw.Draw(sheet)
    d.text((pad, 18), "1. When you open the tablet", fill=(240, 240, 245), font=font)
    d.text((pad * 2 + h.width, 18), "2. Modules page (tap Modules)", fill=(240, 240, 245), font=font)
    sheet.alpha_composite(h, (pad, 50))
    sheet.alpha_composite(m, (pad * 2 + h.width, 50))
    notes = ["Pin = default app: the tablet opens straight into it.",
             "Padlock = module missing; insert the item to unlock.",
             "Home button (purple) always returns to the app grid."]
    for i, n in enumerate(notes):
        d.text((pad, 50 + h.height + 16 + i * 22), n, fill=(210, 210, 222), font=small)
    sheet.save(os.path.join(OUT, "tablet_open_screens.png"))
    print(sheet.size)


if __name__ == "__main__":
    main()
