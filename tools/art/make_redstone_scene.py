"""Scene: Farm Controller -> farm cable -> redstone switch part mounted on a machine block.
Rendered for both switch options (A lever, B plate), switch on."""
from PIL import Image, ImageDraw, ImageFont
import os
import make_redstone as rs
import make_library_v2 as v2

OUT = os.path.dirname(os.path.abspath(__file__))
P = v2.P
ALL = ["south", "north", "east", "west", "up", "down"]


def cable_tex(w, h):
    """AE2-style cable skin: white sheath with a fluix core stripe along the long axis."""
    im = Image.new("RGBA", (w, h), P["W"])
    d = ImageDraw.Draw(im)
    if w >= h:
        d.line([(0, 0), (w - 1, 0)], fill=P["O"]); d.line([(0, h - 1), (w - 1, h - 1)], fill=P["O"])
        d.line([(0, h // 2), (w - 1, h // 2)], fill=P["F"])
    else:
        d.line([(0, 0), (0, h - 1)], fill=P["O"]); d.line([(w - 1, 0), (w - 1, h - 1)], fill=P["O"])
        d.line([(w // 2, 0), (w // 2, h - 1)], fill=P["F"])
    return im


def cable(x0, y0, z0, x1, y1, z1):
    w, h, d = x1 - x0, y1 - y0, z1 - z0
    f = {"south": cable_tex(w, h), "north": cable_tex(w, h), "east": cable_tex(d, h),
         "west": cable_tex(d, h), "up": cable_tex(w, d), "down": cable_tex(w, d)}
    return ((x0, y0, z0), (x1, y1, z1), f)


def controller(x0, z0):
    f = {"south": rs.CTRL_FRONT, "east": rs.CTRL_NET, "west": rs.CTRL_FARM, "north": rs.CTRL_NET,
         "up": rs.CTRL_TOP, "down": v2.BOTTOM}
    return ((x0, 0, z0), (x0 + 16, 16, z0 + 16), f)


def scene(option):
    boxes = [((0, 0, 0), (16, 16, 16), {k: rs.HOST for k in ALL})]       # machine / spawner
    if option == "lever":
        parts = rs.lever_boxes(True)[1:]
        cy0, cy1 = 4, 8
    else:
        parts = rs.plate_boxes(True)[1:]
        cy0, cy1 = 6, 10
    boxes += parts
    # farm cable: out of the switch toward the viewer, then right to the controller's farm side
    boxes.append(cable(6, cy0, 18, 10, cy1, 30))
    boxes.append(cable(10, cy0, 26, 48, cy1, 30))
    boxes.append(controller(48, 20))
    # main network cable leaving the controller's network side (east)
    boxes.append(cable(64, 6, 26, 80, 10, 30))
    return v2.render(boxes, k=6, size=(620, 470), origin=(230, 20))


def main():
    font = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf", 13)
    bold = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf", 15)
    sheet = Image.new("RGBA", (1280, 560), (226, 226, 222, 255))
    d = ImageDraw.Draw(sheet)
    for i, (opt, title) in enumerate([("lever", "A: ME Redstone Lever"), ("plate", "B: ME Redstone Plate")]):
        x = 10 + i * 635
        d.text((x + 10, 12), title, fill=(30, 30, 30), font=bold)
        sheet.alpha_composite(scene(opt), (x, 36))
        d.text((x + 10, 480), "Machine (left)  <-  switch  <-  farm cable  <-  Farm Controller  <-  main network", fill=(70, 70, 70), font=font)
    d.text((20, 515), "The switch sits on the machine's face and the farm cable plugs into it, like a P2P tunnel. Both shown switched on.",
           fill=(60, 60, 60), font=font)
    sheet.save(os.path.join(OUT, "redstone_switch_with_cable.png"))
    print("ok")


if __name__ == "__main__":
    main()
