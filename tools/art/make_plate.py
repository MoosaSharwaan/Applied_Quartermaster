"""ME Redstone Plate (final choice): P2P-style faceplate with a light ring around the cable socket.
States: offline = no light, off = red, on = green. Rendered with the farm cable plugged in."""
from PIL import Image, ImageDraw, ImageFont
import os
import make_redstone as rs
import make_redstone_scene as sc
import make_library_v2 as v2

OUT = os.path.dirname(os.path.abspath(__file__))
P = v2.P
P.update({"L0": (52, 50, 70, 255)})
LIGHT = {"offline": ((52, 50, 70, 255), (40, 38, 56, 255)),
         "off": ((235, 60, 50, 255), (170, 30, 30, 255)),
         "on": ((90, 230, 110, 255), (40, 160, 70, 255))}

FRONT = [  # 12x12: wide light ring (L bright / M shaded) around a 4x4 cable socket (D)
    "oooooooooooo",
    "oWWWWWWWWWso",
    "oWLLLLLLLLSo",
    "oWLLLLLLLMSo",
    "oWLLDDDDMMSo",
    "oWLLDDDDMMSo",
    "oWLLDDDDMMSo",
    "oWLLDDDDMMSo",
    "oWLMMMMMMMSo",
    "oWMMMMMMMMSo",
    "oSSSSSSSSSBo",
    "oooooooooooo",
]


def plate_face(state):
    bright, dark = LIGHT[state]
    im = Image.new("RGBA", (12, 12))
    for y, r in enumerate(FRONT):
        for x, ch in enumerate(r):
            im.putpixel((x, y), bright if ch == "L" else dark if ch == "M" else P[ch])
    return im


def plate_boxes(state, with_cable=True):
    host = ((0, 0, 0), (16, 16, 16), {k: rs.HOST for k in sc.ALL})
    edge = Image.new("RGBA", (12, 1), P["S"])
    boxes = [host, ((2, 2, 16), (14, 14, 17), rs.faces_for(12, 12, 1, plate_face(state), edge, edge))]
    if with_cable:
        boxes.append(sc.cable(7, 7, 17, 9, 9, 30))   # cable plugged into the 2x2 socket... drawn 2px for the close-up
    return boxes


def cable_thin(x0, y0, z0, x1, y1, z1):
    return sc.cable(x0, y0, z0, x1, y1, z1)


def main():
    for st in LIGHT:
        plate_face(st).save(os.path.join(OUT, f"me_redstone_plate_{st}.png"))
    font = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf", 13)
    bold = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf", 15)
    sheet = Image.new("RGBA", (1220, 400), (226, 226, 222, 255))
    d = ImageDraw.Draw(sheet)
    d.text((20, 12), "ME Redstone Plate", fill=(30, 30, 30), font=bold)
    d.text((20, 34), "The light ring sits around the cable socket, so it stays visible with the farm cable plugged in.",
           fill=(80, 80, 80), font=font)
    for i, (st, label) in enumerate([("offline", "Offline: no power or no channel, no light"),
                                      ("off", "Switched off: red, no redstone"),
                                      ("on", "Switched on: green, redstone out")]):
        x = 20 + i * 400
        big = plate_face(st).resize((108, 108), Image.NEAREST)
        sheet.alpha_composite(big, (x, 66))
        boxes = [((0, 0, 0), (16, 16, 16), {k: rs.HOST for k in sc.ALL}),
                 ((2, 2, 16), (14, 14, 17), rs.faces_for(12, 12, 1, plate_face(st),
                                                          Image.new("RGBA", (12, 1), P["S"]), Image.new("RGBA", (12, 1), P["S"]))),
                 sc.cable(6, 6, 17, 10, 10, 30)]
        view = v2.render(boxes, k=7, size=(290, 285), origin=(175, 0))
        sheet.alpha_composite(view, (x + 105, 60))
        d.text((x, 330), label, fill=(50, 50, 50), font=font)
    d.text((20, 365), "Farm Controller lights match: green = switch on, red = switch off, dark = offline.",
           fill=(60, 60, 60), font=font)
    sheet.save(os.path.join(OUT, "redstone_plate_states.png"))
    print("ok")


if __name__ == "__main__":
    main()
