"""Final automation sheet: ME Farm Controller (switchboard) + ME Redstone Plate, wired together."""
from PIL import Image, ImageDraw, ImageFont
import os
import make_redstone as rs
import make_redstone_scene as sc
import make_plate as pl
import make_controller_variants as cv
import make_library_v2 as v2

OUT = os.path.dirname(os.path.abspath(__file__))
P = v2.P
SWITCH = cv.SWITCHBOARD


def offline(img):
    m = {P[c]: P["o"] for c in "CjgU"}
    out = img.copy(); out.putdata([m.get(p, p) for p in out.get_flattened_data()]); return out


def scene():
    boxes = [((0, 0, 0), (16, 16, 16), {k: rs.HOST for k in sc.ALL})]
    edge = Image.new("RGBA", (12, 1), P["S"])
    boxes.append(((2, 2, 16), (14, 14, 17), rs.faces_for(12, 12, 1, pl.plate_face("on"), edge, edge)))
    boxes.append(sc.cable(6, 6, 17, 10, 10, 30))
    boxes.append(sc.cable(10, 6, 26, 48, 10, 30))
    boxes.append(((48, 0, 20), (64, 16, 36), {"south": SWITCH, "east": rs.CTRL_NET, "west": rs.CTRL_FARM,
                                               "north": rs.CTRL_NET, "up": rs.CTRL_TOP, "down": v2.BOTTOM}))
    boxes.append(sc.cable(64, 6, 26, 80, 10, 30))
    return v2.render(boxes, k=6, size=(620, 470), origin=(230, 20))


def main():
    SWITCH.save(os.path.join(OUT, "me_farm_controller_front.png"))
    offline(SWITCH).save(os.path.join(OUT, "me_farm_controller_front_off.png"))
    font = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf", 13)
    bold = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf", 15)
    sheet = Image.new("RGBA", (1240, 480), (226, 226, 222, 255))
    d = ImageDraw.Draw(sheet)
    t = 72

    def tile(img, x, y, label):
        sheet.alpha_composite(img.resize((t, t), Image.NEAREST), (x, y))
        tw = d.textlength(label, font=font)
        d.text((x + (t - tw) / 2, y + t + 5), label, fill=(50, 50, 50), font=font)

    d.text((20, 12), "ME Farm Controller (switchboard)", fill=(30, 30, 30), font=bold)
    tile(SWITCH, 20, 44, "Front"); tile(offline(SWITCH), 104, 44, "Offline")
    tile(rs.CTRL_NET, 188, 44, "Network side"); tile(rs.CTRL_FARM, 272, 44, "Farm side")
    tile(rs.CTRL_TOP, 356, 44, "Top")
    d.text((20, 150), "ME Redstone Plate", fill=(30, 30, 30), font=bold)
    for i, st in enumerate(["offline", "off", "on"]):
        tile(pl.plate_face(st), 20 + i * 84, 182, {"offline": "Offline", "off": "Off (red)", "on": "On (green)"}[st])
    notes = ["Controller front: one light per plate in the farm,",
             "green = on, red = off, dark = unused spot.",
             "Plate light ring: green on, red off, no light offline.",
             "Only the controller uses a channel; plates use none."]
    for i, n in enumerate(notes):
        d.text((20, 290 + i * 20), n, fill=(60, 60, 60), font=font)
    d.text((470, 12), "Wired: machine ← plate ← farm cable ← controller ← main network", fill=(30, 30, 30), font=bold)
    sheet.alpha_composite(scene(), (450, 40))
    sheet.save(os.path.join(OUT, "automation_final.png"))
    print("ok")


if __name__ == "__main__":
    main()
