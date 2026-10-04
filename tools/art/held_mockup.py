"""Mockup of the ME Tablet held in first person: plain item vs. custom 3D model."""
from PIL import Image, ImageDraw, ImageFont
import os, random

OUT = os.path.dirname(os.path.abspath(__file__))
W, H = 640, 400
random.seed(4)
font = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf", 16)


def scene():
    img = Image.new("RGBA", (W, H), (126, 168, 255, 255))
    d = ImageDraw.Draw(img)
    for y in range(H):  # sky
        t = y / H
        d.line([(0, y), (W, y)], fill=(int(120 + 60 * t), int(165 + 45 * t), 255, 255))
    b = 40  # blocky ground
    for by in range(220, H, b):
        for bx in range(0, W, b):
            top = by == 220
            base = (95, 159, 53) if top else (134, 96, 67)
            for py in range(by, by + b, 8):
                for px in range(bx, bx + b, 8):
                    j = random.randint(-14, 14)
                    c = tuple(max(0, min(255, v + j)) for v in base)
                    if top and py >= by + 16:
                        c = tuple(max(0, min(255, v + j)) for v in (134, 96, 67))
                    d.rectangle([px, py, px + 7, py + 7], fill=c + (255,))
    # AE2 drive block in the distance
    d.rectangle([380, 140, 460, 220], fill=(70, 70, 82, 255), outline=(30, 30, 36, 255), width=3)
    for i in range(5):
        d.rectangle([392, 152 + i * 13, 448, 160 + i * 13], fill=(40, 40, 50, 255))
        d.rectangle([440, 154 + i * 13, 444, 158 + i * 13], fill=(86, 214, 232, 255) if i < 4 else (138, 92, 214, 255))
    # crosshair
    d.line([(W // 2 - 8, H // 2), (W // 2 + 8, H // 2)], fill=(255, 255, 255, 230), width=2)
    d.line([(W // 2, H // 2 - 8), (W // 2, H // 2 + 8)], fill=(255, 255, 255, 230), width=2)
    return img


def skin_arm(d, pts):
    d.polygon(pts, fill=(201, 150, 112, 255), outline=(150, 105, 78, 255))


def option_a():
    img = scene()
    d = ImageDraw.Draw(img)
    skin_arm(d, [(560, 400), (640, 330), (640, 400)])
    skin_arm(d, [(470, 400), (560, 300), (640, 340), (590, 400)])
    tex = Image.open(os.path.join(OUT, "me_tablet.png")).resize((176, 176), Image.NEAREST)
    tex = tex.rotate(-28, resample=Image.NEAREST, expand=True)
    edge = tex.copy()
    px = edge.load()
    for y in range(edge.height):
        for x in range(edge.width):
            if px[x, y][3]:
                px[x, y] = (22, 22, 28, 255)
    for k in range(10, 0, -2):  # fake extruded thickness
        img.alpha_composite(edge, (420 + k, 165 + k))
    img.alpha_composite(tex, (420, 165))
    return img


def option_b():
    img = scene()
    d = ImageDraw.Draw(img)
    skin_arm(d, [(60, 400), (150, 330), (215, 352), (150, 400)])
    skin_arm(d, [(580, 400), (490, 330), (425, 352), (490, 400)])
    x0, y0, x1, y1 = 170, 220, 470, 380  # tablet body seen from above, tilted toward you
    d.polygon([(x0 + 20, y0), (x1 - 20, y0), (x1, y1), (x0, y1)], fill=(27, 27, 34, 255))
    d.polygon([(x0 + 28, y0 + 6), (x1 - 28, y0 + 6), (x1 - 8, y1 - 18), (x0 + 8, y1 - 18)], fill=(92, 92, 108, 255))
    sx0, sy0, sx1, sy1 = x0 + 40, y0 + 14, x1 - 40, y1 - 30
    d.polygon([(sx0, sy0), (sx1, sy0), (sx1 + 14, sy1), (sx0 - 14, sy1)], fill=(20, 30, 44, 255))
    icons = ["app_storage", "app_network", "app_crafting", "app_devices", "app_tools", "app_alerts"]
    for i, n in enumerate(icons):
        r, c = divmod(i, 3)
        ic = Image.open(os.path.join(OUT, f"{n}.png")).resize((40 + r * 4, 40 + r * 4), Image.NEAREST)
        img.alpha_composite(ic, (int(sx0 + 18 + c * (62 + r * 4) - r * 8), sy0 + 10 + r * 52))
    d.rectangle([x0 + 140, y1 - 12, x0 + 160, y1 - 6], fill=(138, 92, 214, 255))
    d.rectangle([sx0 + 4, sy0 + 2, sx0 + 60, sy0 + 6], fill=(86, 214, 232, 255))
    return img


a, b = option_a(), option_b()
sheet = Image.new("RGBA", (W * 2 + 30, H + 50), (240, 240, 236, 255))
sheet.paste(a, (10, 10))
sheet.paste(b, (W + 20, 10))
d = ImageDraw.Draw(sheet)
d.text((20, H + 18), "A: plain item (flat sprite, like most tools)", fill=(40, 40, 40), font=font)
d.text((W + 30, H + 18), "B: custom 3D model (held in both hands, screen faces you)", fill=(40, 40, 40), font=font)
sheet.save(os.path.join(OUT, "held_in_hand_mockup.png"))
print("ok")
