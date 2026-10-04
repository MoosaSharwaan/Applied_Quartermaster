"""Redstone Plates as cable-bus parts: several plates in one block space around a cable, like P2P tunnels.
Scene: a farm cable runs up through one block space; plates on its 4 sides each face a different machine.
The two machines nearest the viewer are left out so the plates facing them can be seen."""
from PIL import Image, ImageDraw, ImageFont
import os
import make_redstone as rs
import make_redstone_scene as sc
import make_plate as pl
import make_library_v2 as v2

OUT = os.path.dirname(os.path.abspath(__file__))
P = v2.P
HOSTF = {k: rs.HOST for k in sc.ALL}


def plate(face_dir, state, bx, bz):
    """Plate part inside the cable block at (bx..bx+16, 0..16, bz..bz+16), on the given side."""
    f = pl.plate_face(state)
    edge_h = Image.new("RGBA", (12, 2), P["S"])
    edge_v = Image.new("RGBA", (2, 12), P["S"])
    if face_dir == "south":
        box = ((bx + 2, 2, bz + 14), (bx + 14, 14, bz + 16))
        faces = {"south": f, "north": f, "east": edge_v, "west": edge_v, "up": edge_h, "down": edge_h}
    elif face_dir == "north":
        box = ((bx + 2, 2, bz), (bx + 14, 14, bz + 2))
        faces = {"south": f, "north": f, "east": edge_v, "west": edge_v, "up": edge_h, "down": edge_h}
    elif face_dir == "east":
        box = ((bx + 14, 2, bz + 2), (bx + 16, 14, bz + 14))
        faces = {"east": f, "west": f, "south": edge_v, "north": edge_v,
                 "up": Image.new("RGBA", (2, 12), P["S"]), "down": Image.new("RGBA", (2, 12), P["S"])}
    else:  # west
        box = ((bx, 2, bz + 2), (bx + 2, 14, bz + 14))
        faces = {"east": f, "west": f, "south": edge_v, "north": edge_v,
                 "up": Image.new("RGBA", (2, 12), P["S"]), "down": Image.new("RGBA", (2, 12), P["S"])}
    return (box[0], box[1], faces)


def scene():
    bx, bz = 16, 16                      # the cable block space
    boxes = [((0, 0, bz), (16, 16, bz + 16), HOSTF),          # machine to the west
             ((bx, 0, 0), (bx + 16, 16, 16), HOSTF)]          # machine to the north
    boxes.append(sc.cable(bx + 6, 0, bz + 6, bx + 10, 28, bz + 10))   # farm cable running up through
    for d, st in [("north", "on"), ("west", "off"), ("south", "on"), ("east", "off")]:
        boxes.append(plate(d, st, bx, bz))
    return v2.render(boxes, k=9, size=(600, 480), origin=(300, 20))


def main():
    font = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf", 14)
    bold = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf", 16)
    img = scene()
    sheet = Image.new("RGBA", (1160, 520), (226, 226, 222, 255))
    d = ImageDraw.Draw(sheet)
    sheet.alpha_composite(img, (0, 20))
    d.text((620, 30), "4 Redstone Plates in one block space", fill=(30, 30, 30), font=bold)
    lines = [
        "The farm cable runs through the block; each plate clips onto",
        "one side of it, exactly like P2P tunnels on a cable.",
        "",
        "Each plate faces a different machine and switches it on its",
        "own: here the south and north plates are on (green) and the",
        "east and west plates are off (red).",
        "",
        "Machines sit on all four sides in game; the two nearest you",
        "are left out of this picture so you can see the plates.",
        "",
        "AE2 cables take a part on every side, so the top and bottom",
        "can hold plates too: up to 6 in one block space.",
        "",
        "Each plate still has its own name, icon and strength,",
        "and shows up separately in the Automation screen.",
    ]
    for i, l in enumerate(lines):
        d.text((620, 66 + i * 24), l, fill=(60, 60, 60), font=font)
    sheet.save(os.path.join(OUT, "plates_on_cable_bus.png"))
    print("ok")


if __name__ == "__main__":
    main()
