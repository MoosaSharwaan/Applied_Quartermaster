"""ME Farm Controller block + two options for the redstone switch part (AE2 style):
  A) ME Redstone Lever  - lever-like part mounted on a block face
  B) ME Redstone Plate  - P2P-tunnel-style faceplate mounted on a block face
Shows on/off states in 3D on a host block."""
from PIL import Image, ImageDraw, ImageFont
import os, random
import make_racks as mr          # palette additions + framed()
import make_library_v2 as v2

OUT = os.path.dirname(os.path.abspath(__file__))
P = v2.P
P.update({
    "u": (150, 20, 20, 255),     # redstone dark
    "U": (220, 40, 30, 255),     # redstone
    "a": (255, 120, 90, 255),    # redstone glow
    "g": (100, 200, 110, 255),   # status green
    "l": (64, 44, 44, 255),      # unlit redstone
})
framed = mr.framed

# ---------------- ME Farm Controller faces
CTRL_FRONT = framed([          # status screen: wheat + gear emblem, LED rows for switches
    "DDDDDDDDDDDD",
    "DYDYDDDoooDD",
    "DYYYDDoNNNoD",
    "DDYDDDoNDNoD",
    "DDYDDDoNNNoD",
    "DDYDDDDoooDD",
    "DDDDDDDDDDDD",
    "DgDgDgDUDgDD",
    "DDDDDDDDDDDD",
    "DgDUDgDgDgDD",
    "DDDDDDDDDDDD",
    "BBBBBBBBBBBB",
])
CTRL_NET = framed([            # network side: certus port
    "ssssssssssss",
    "sSSSSSSSSSSB",
    "sSccccccccSB",
    "sScDDDDDDcSB",
    "sScDCCCCDcSB",
    "sScDCjjCDcSB",
    "sScDCjjCDcSB",
    "sScDCCCCDcSB",
    "sScDDDDDDcSB",
    "sSccccccccSB",
    "sSSSSSSSSSSB",
    "BBBBBBBBBBBB",
])
CTRL_FARM = framed([           # farm side: redstone port
    "ssssssssssss",
    "sSSSSSSSSSSB",
    "sSuuuuuuuuSB",
    "sSuDDDDDDuSB",
    "sSuDUUUUDuSB",
    "sSuDUaaUDuSB",
    "sSuDUaaUDuSB",
    "sSuDUUUUDuSB",
    "sSuDDDDDDuSB",
    "sSuuuuuuuuSB",
    "sSSSSSSSSSSB",
    "BBBBBBBBBBBB",
])
CTRL_TOP = mr.RACK_TOP


def unlit(img):
    m = {P["C"]: P["o"], P["j"]: P["B"], P["g"]: P["o"], P["U"]: P["l"], P["a"]: P["u"]}
    out = img.copy(); out.putdata([m.get(p, p) for p in out.get_flattened_data()]); return out


def solid(w, h, ch):
    return Image.new("RGBA", (w, h), P[ch])


def host_face():
    random.seed(3)
    im = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            v = random.randint(-10, 10)
            im.putpixel((x, y), (118 + v, 118 + v, 120 + v, 255))
    return im


HOST = host_face()


def faces_for(w, h, d, front, side, top):
    return {"south": front if front.size == (w, h) else front.resize((w, h), Image.NEAREST),
            "north": side.resize((w, h), Image.NEAREST), "east": side.resize((d, h), Image.NEAREST),
            "west": side.resize((d, h), Image.NEAREST), "up": top.resize((w, d), Image.NEAREST),
            "down": top.resize((w, d), Image.NEAREST)}


def lever_boxes(on):
    """Lever part on the south face of a host block (z = 16 is the face)."""
    host = ((0, 0, 0), (16, 16, 16), {k: HOST for k in ["south", "north", "east", "west", "up", "down"]})
    base_front = v2.tex(["".join(r) for r in [
        "................"]] * 0 + [
        "WWWWWWWWWWWWWWWW"] * 16) if False else None
    base = Image.new("RGBA", (6, 9), P["s"])
    db = ImageDraw.Draw(base)
    db.rectangle([0, 0, 5, 8], outline=P["O"])
    db.line([(1, 1), (4, 1)], fill=P["W"])
    db.rectangle([2, 3, 3, 5], fill=P["D"])                      # slot for the handle
    db.point((2, 7), fill=P["C"] if on else P["o"]); db.point((3, 7), fill=P["C"] if on else P["o"])
    boxes = [host, ((5, 4, 16), (11, 13, 18), faces_for(6, 9, 2, base, solid(6, 9, "S"), solid(6, 2, "s")))]
    stem = solid(2, 2, "N")
    tip = solid(2, 2, "j" if on else "o")
    steps = [(8, 17), (9, 18), (10, 19)] if on else [(7, 17), (6, 18), (5, 19)]
    for i, (yy, zz) in enumerate(steps):
        t = tip if i == len(steps) - 1 else stem
        sd = solid(2, 2, "j" if (on and i == len(steps) - 1) else ("o" if i == len(steps) - 1 else "n"))
        boxes.append(((7, yy, zz), (9, yy + 2, zz + 2), faces_for(2, 2, 2, t, sd, t)))
    return boxes


def plate_boxes(on):
    host = ((0, 0, 0), (16, 16, 16), {k: HOST for k in ["south", "north", "east", "west", "up", "down"]})
    front = v2.tex([
        "................",
        "................",
        "..oooooooooooo..",
        "..oWWWWWWWWWso..",
        "..oWsssssssSso..",
        "..oWsOOOOOOSso..",
        "..oWsODDDDOSso..",
        "..oWsODaUDOSso..".replace("a", "a" if on else "l").replace("U", "U" if on else "u"),
        "..oWsODUaDOSso..".replace("a", "a" if on else "l").replace("U", "U" if on else "u"),
        "..oWsODDDDOSso..",
        "..oWsOOOOOOSso..",
        "..oWsSSSSSSSSo..",
        "..oSSSSSSSSSBo..",
        "..oooooooooooo..",
        "................",
        "................",
    ]).crop((2, 2, 14, 14))
    edge = solid(12, 1, "S")
    return [host, ((2, 2, 16), (14, 14, 17), faces_for(12, 12, 1, front, edge, edge))]


def main():
    for name, im in {"me_farm_controller_front": CTRL_FRONT, "me_farm_controller_network": CTRL_NET,
                     "me_farm_controller_farm": CTRL_FARM}.items():
        im.save(os.path.join(OUT, f"{name}.png")); unlit(im).save(os.path.join(OUT, f"{name}_off.png"))

    font = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf", 13)
    bold = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf", 15)
    sheet = Image.new("RGBA", (1300, 620), (226, 226, 222, 255))
    d = ImageDraw.Draw(sheet)
    t = 64

    def tile(img, x, y, label):
        sheet.alpha_composite(img.resize((t, t), Image.NEAREST), (x, y))
        tw = d.textlength(label, font=font)
        d.text((x + (t - tw) / 2, y + t + 5), label, fill=(50, 50, 50), font=font)

    # Farm Controller
    d.text((20, 14), "ME Farm Controller", fill=(30, 30, 30), font=bold)
    d.text((20, 36), "One per farm. Network side joins your ME network; farm side runs the farm's own cables.", fill=(80, 80, 80), font=font)
    tile(CTRL_FRONT, 20, 66, "Front"); tile(CTRL_NET, 104, 66, "Network side")
    tile(CTRL_FARM, 188, 66, "Farm side"); tile(CTRL_TOP, 272, 66, "Top")
    tile(unlit(CTRL_FRONT), 356, 66, "Front, off")
    cube = v2.render([((0, 0, 0), (16, 16, 16), {"south": CTRL_FRONT, "east": CTRL_FARM, "west": CTRL_NET,
                                                  "north": CTRL_NET, "up": CTRL_TOP, "down": v2.BOTTOM})],
                     k=7, size=(240, 250), origin=(120, 20))
    sheet.alpha_composite(cube, (440, 50))
    d.text((452, 290), "Front + farm side (red port)", fill=(70, 70, 70), font=font)

    # Switch options
    d.text((720, 14), "Redstone switch: two options", fill=(30, 30, 30), font=bold)
    d.text((720, 36), "Both mount on any block face, like a P2P tunnel, and output redstone when on.", fill=(80, 80, 80), font=font)
    opts = [("A: ME Redstone Lever", lever_boxes), ("B: ME Redstone Plate", plate_boxes)]
    for row, (label, fn) in enumerate(opts):
        y = 64 + row * 275
        d.text((720, y), label, fill=(30, 30, 30), font=bold)
        for col, on in enumerate((False, True)):
            img = v2.render(fn(on), k=7, size=(270, 250), origin=(150, 5))
            sheet.alpha_composite(img, (700 + col * 290, y + 20))
            d.text((780 + col * 290, y + 248), "On (glows)" if on else "Off", fill=(70, 70, 70), font=font)

    d.text((20, 340), "How a farm is wired:", fill=(30, 30, 30), font=bold)
    lines = ["Main ME network -- cable --> network side of the Farm Controller",
             "Farm side --> farm cables --> Redstone Switches on the machines, spawners or doors",
             "Only the Farm Controller uses a channel; switches need none, so a farm can have any number.",
             "Lights: controller LEDs show each switch (green on, red off); everything goes dark without power."]
    for i, l in enumerate(lines):
        d.text((20, 366 + i * 22), l, fill=(60, 60, 60), font=font)
    sheet.save(os.path.join(OUT, "farm_controller_and_switches.png"))
    print("ok")


if __name__ == "__main__":
    main()
