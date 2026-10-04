"""Four Farm Controller variants, each shown as its front tile and a 3D view (front + farm side)."""
from PIL import Image, ImageDraw, ImageFont
import os
import make_racks as mr
import make_redstone as rs
import make_library_v2 as v2

OUT = os.path.dirname(os.path.abspath(__file__))
P = v2.P
framed = mr.framed

# 1) Switchboard: header strip + 2 rows of 4 toggle lights (one per plate)
SWITCHBOARD = framed([
    "DDDDDDDDDDDD",
    "DCCCCCCCCCCD",
    "DDDDDDDDDDDD",
    "DoooDoooDoDD",
    "DogoDouoDoDD",
    "DoooDoooDoDD",
    "DDDDDDDDDDDD",
    "DoooDoooDoDD",
    "DogoDogoDoDD",
    "DoooDoooDoDD",
    "DDDDDDDDDDDD",
    "BBBBBBBBBBBB",
])
SWITCHBOARD = framed([
    "DDDDDDDDDDDD",
    "DCCCCCCCCCCD",
    "DDDDDDDDDDDD",
    "DggDUUDggDgD",
    "DggDUUDggDgD",
    "DDDDDDDDDDDD",
    "DggDggDUUDoD",
    "DggDggDUUDoD",
    "DDDDDDDDDDDD",
    "DooDooDooDoD",
    "DooDooDooDoD",
    "BBBBBBBBBBBB",
])

# 2) Light ring: matches the Redstone Plate; big status ring around a farm socket
def ring(color_bright, color_dark):
    rows = [
        "ssssssssssss",
        "sOOOOOOOOOOS",
        "sOLLLLLLLLMS",
        "sOLLLLLLLMMS",
        "sOLLDDDDMMMS",
        "sOLLDYqDMMMS",
        "sOLLDqYDMMMS",
        "sOLLDDDDMMMS",
        "sOLMMMMMMMMS",
        "sOMMMMMMMMMS",
        "sSSSSSSSSSSS",
        "BBBBBBBBBBBB",
    ]
    im = framed([r.replace("L", "s").replace("M", "s") for r in rows])
    for y, r in enumerate(rows):
        for x, ch in enumerate(r):
            if ch in "LM":
                im.putpixel((x + 2, y + 2), color_bright if ch == "L" else color_dark)
    return im


RING = ring((90, 230, 110, 255), (40, 160, 70, 255))

# 3) Farm emblem: big crop on a dark screen with certus corner lights and a status bar
EMBLEM = framed([
    "CDDDDDDDDDDC",
    "DDDDDYDDDDDD",
    "DDDDYYYDDDDD",
    "DDDYDYDYDDDD",
    "DDDDDYDDDDDD",
    "DDYDDYDDYDDD",
    "DDDYDYDYDDDD",
    "DDDDYYYDDDDD",
    "DDDDDqDDDDDD",
    "DDDqqqqqDDDD",
    "CggggggggggC",
    "BBBBBBBBBBBB",
])

# 4) Wall panel: a thick panel that mounts on a wall, screen + 3 rocker switches
PANEL_FRONT = framed([
    "DDDDDDDDDDDD",
    "DCjCCCCCCCCD",
    "DDDDDDDDDDDD",
    "DYDDYYDDDDDD",
    "DYYDDYDDDDDD",
    "DDDDDDDDDDDD",
    "ssssssssssss",
    "sOOsOOsOOsSs",
    "sOgsOUsOgsSs",
    "sOgsOUsOgsSs",
    "sOOsOOsOOsSs",
    "BBBBBBBBBBBB",
])


def solid(w, h, ch):
    return Image.new("RGBA", (w, h), P[ch])


def block_view(front):
    f = {"south": front, "east": rs.CTRL_FARM, "west": rs.CTRL_NET, "north": rs.CTRL_NET,
         "up": rs.CTRL_TOP, "down": v2.BOTTOM}
    return v2.render([((0, 0, 0), (16, 16, 16), f)], k=8, size=(270, 280), origin=(135, 10))


def panel_view(front):
    host = ((0, 0, 0), (16, 16, 16), {k: rs.HOST for k in ["south", "north", "east", "west", "up", "down"]})
    side = rs.CTRL_FARM.resize((5, 16), Image.NEAREST)
    top = solid(16, 5, "s")
    panel = ((0, 0, 16), (16, 16, 21), {"south": front, "north": front, "east": side, "west": side, "up": top, "down": top})
    return v2.render([host, panel], k=7, size=(270, 280), origin=(150, 5))


def main():
    variants = [
        ("1. Switchboard", SWITCHBOARD, block_view, "A light per Redstone Plate:\ngreen on, red off, dark = empty."),
        ("2. Light ring", RING, block_view, "Same ring as the plate: green when\nthe farm runs, red when all off."),
        ("3. Farm emblem", EMBLEM, block_view, "Big crop emblem on a screen;\nstatus bar along the bottom."),
        ("4. Wall panel", PANEL_FRONT, panel_view, "Thick panel that mounts on a wall:\nscreen plus rocker switches."),
    ]
    font = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf", 13)
    bold = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf", 15)
    colw = 300
    sheet = Image.new("RGBA", (colw * 4 + 20, 520), (226, 226, 222, 255))
    d = ImageDraw.Draw(sheet)
    for i, (title, front, fn, desc) in enumerate(variants):
        x = 15 + i * colw
        d.text((x, 12), title, fill=(30, 30, 30), font=bold)
        sheet.alpha_composite(front.resize((96, 96), Image.NEAREST), (x, 40))
        d.text((x + 106, 50), "Front", fill=(90, 90, 90), font=font)
        sheet.alpha_composite(fn(front), (x, 150))
        for j, line in enumerate(desc.split("\n")):
            d.text((x, 445 + j * 18), line, fill=(60, 60, 60), font=font)
        front.save(os.path.join(OUT, f"farm_controller_variant_{i + 1}.png"))
    d.text((15, 495), "All four keep the cyan network port and red farm port on the sides; colours shown with the farm running.",
           fill=(80, 80, 80), font=font)
    sheet.save(os.path.join(OUT, "farm_controller_variants.png"))
    print("ok")


if __name__ == "__main__":
    main()
