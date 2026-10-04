"""Draws the ME Tablet pixel-art textures (16x16) in AE2's wireless-terminal style,
plus a scaled-up preview sheet that includes AE2's own terminal for comparison."""
from PIL import Image, ImageDraw, ImageFont
import os

OUT = os.path.dirname(os.path.abspath(__file__))
AE2_REF = "/home/claude/refs/ae2/src/main/resources/assets/ae2/textures/item/wireless_terminal.png"

# Palette sampled from AE2's wireless terminal / network tool so the tablet sits next to them naturally
P = {
    ".": (0, 0, 0, 0),
    "O": (65, 63, 84, 255),     # darkest outline
    "o": (77, 77, 103, 255),    # outline
    "W": (242, 242, 242, 255),  # highlight
    "w": (222, 223, 227, 255),  # light silver
    "s": (203, 204, 212, 255),  # silver
    "S": (173, 176, 196, 255),  # shaded silver
    "B": (105, 109, 136, 255),  # bottom band / deep shade
    "F": (145, 93, 205, 255),   # purple bezel
    "A": (176, 111, 221, 255),  # antenna purple
    "G": (226, 163, 227, 255),  # pink screen light
    "I": (90, 71, 158, 255),    # dark purple screen
    "C": (172, 233, 255, 255),  # certus cyan (from network tool)
    "c": (138, 187, 239, 255),  # blue-cyan shade
    "y": (240, 196, 72, 255),   # warning amber
    "Y": (255, 232, 150, 255),  # light amber
    "g": (110, 214, 140, 255),  # on-green
}

SPRITES = {
    "me_tablet": [
        "................",
        "............A...",
        "............O...",
        ".oooooooooooOoo.",
        ".oWWWwWWwwwwwwo.",
        ".oWFFFFFFFFFFso.",
        ".owFIGIICIIGFSO.",
        ".oWFIIIIIIIIFsO.",
        ".owFICIIGIICFSO.",
        ".oWFIIIIIIIIFSO.",
        ".owFFFFFFFFFFSO.",
        ".OsssssSsSSSSSO.",
        ".OBBBBBAABBBBBO.",
        "..OOOOOOOOOOOO..",
        "................",
        "................",
    ],
    "app_storage": [
        "................",
        "....oooooooo....",
        "...oWWWwwwwwo...",
        "...oWFFFFFFso...",
        "...owFGGGGFSO...",
        "...oWFIIIIFsO...",
        "...owFGGGGFSO...",
        "...oWFIIIIFSO...",
        "...owFFFFFFSO...",
        "...oWssssssSO...",
        "...owsCsAsgSO...",
        "...OsssssSSSO...",
        "...OBBBBBBBBO...",
        "....OOOOOOOO....",
        "................",
        "................",
    ],
    "app_network": [
        "................",
        "......oooo......",
        "......oCWo......",
        "......ocCO......",
        "......OOOO......",
        ".......AF.......",
        ".......FA.......",
        "...AAAAFFAAAA...",
        "...F....F...F...",
        "...F....F...F...",
        ".oooo.oooo.oooo.",
        ".oCWo.oGWo.oCWo.",
        ".ocCO.oFGO.ocCO.",
        ".OOOO.OOOO.OOOO.",
        "................",
        "................",
    ],
    "app_crafting": [
        "................",
        ".ooooooooooooooo",
        ".oWWWWwwwwwwwwwo",
        ".oWIIIwIIIwIIIsO",
        ".oWIGIwICIwIGIsO",
        ".owIIIwIIIwIIISO",
        ".owwwwwwwssssssO",
        ".oWIIIwIIIsIIISO",
        ".oWICIsIGIsICISO",
        ".owIIIsIIIsIIISO",
        ".owsssssssSSSSSO",
        ".owIIIsIIISIIISO",
        ".osIGISICISIGISO",
        ".osIIISIIISIIISO",
        ".OBBBBBBBBBBBBBO",
        ".OOOOOOOOOOOOOOO",
    ],
    "app_devices": [
        "................",
        "................",
        "..oooooooooooo..",
        ".oIIIIIIIIoWWwo.",
        ".oIIIIIIIIowsSO.",
        ".oIIIIIIIIoSSBO.",
        "..OOOOOOOOOOOO..",
        "................",
        "................",
        "..oooooooooooo..",
        ".oWWwoCCCCCCCCo.",
        ".owsSoCCCCCCCcO.",
        ".oSSBoccccccccO.",
        "..OOOOOOOOOOOO..",
        "................",
        "................",
    ],
    "app_tools": [
        "................",
        "..........ooo...",
        ".........oWwO...",
        "........oWso.o..",
        "........oso.oWo.",
        "........oWsoWsO.",
        ".......oWssssSO.",
        "......oWsSOOOO..",
        ".....oWsSO......",
        "....oWsSO.......",
        "...oAFSO........",
        "..oAFO..........",
        ".oGAFO..........",
        ".oAFFO..........",
        "..OOO...........",
        "................",
    ],
    "app_alerts": [
        "................",
        ".......oo.......",
        "......oYYo......",
        "......oYyO......",
        ".....oYyyyO.....",
        ".....oYyOyO.....",
        "....oYyyOyyO....",
        "....oYyyOyyO....",
        "...oYyyyOyyyO...",
        "...oYyyyyyyyO...",
        "..oYyyyyOyyyyO..",
        "..oyyyyyyyyyyO..",
        ".oOOOOOOOOOOOOO.",
        "................",
        "................",
        "................",
    ],
    "app_library": [
        "................",
        "................",
        "..oo.oo.oo......",
        "..oFoGWoCWo.oo..",
        "..oFoGwoCwooAWo.",
        "..oAoGsocsoAAsO.",
        "..oFoGWoCWoAFSO.",
        "..oFoGwoCwoFASO.",
        "..oAoGsocsoAFSO.",
        "..oFoGsoCsoFASO.",
        "..oFoGSocSoAFSO.",
        "..OOOOOOOOOOOOO.",
        ".oWWWWwwwwwssssO",
        ".OBBBBBBBBBBBBBO",
        "................",
        "................",
    ],
}


def draw(rows):
    assert len(rows) == 16 and all(len(r) == 16 for r in rows), [len(r) for r in rows]
    img = Image.new("RGBA", (16, 16))
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            img.putpixel((x, y), P[ch])
    return img


def main():
    images = {}
    for name, rows in SPRITES.items():
        img = draw(rows)
        img.save(os.path.join(OUT, f"{name}.png"))
        images[name] = img

    tiles = []
    if os.path.exists(AE2_REF):
        tiles.append(("AE2 terminal (ref)", Image.open(AE2_REF).convert("RGBA").crop((0, 0, 16, 16))))
    for name, img in images.items():
        label = {"me_tablet": "ME Tablet", "me_library_front": "Library block"}.get(name, name.replace("app_", "").capitalize())
        tiles.append((label, img))

    scale, pad, label_h = 7, 20, 26
    cell = 16 * scale
    cols = 5
    rows_n = (len(tiles) + cols - 1) // cols
    sheet = Image.new("RGBA", (cols * (cell + pad) + pad, rows_n * (cell + pad + label_h) + pad), (198, 198, 198, 255))
    d = ImageDraw.Draw(sheet)
    font = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf", 13)
    for i, (label, img) in enumerate(tiles):
        r, c = divmod(i, cols)
        x, y = pad + c * (cell + pad), pad + r * (cell + pad + label_h)
        d.rectangle([x - 4, y - 4, x + cell + 3, y + cell + 3], fill=(139, 139, 139, 255))
        sheet.alpha_composite(img.resize((cell, cell), Image.NEAREST), (x, y))
        tw = d.textlength(label, font=font)
        d.text((x + (cell - tw) / 2, y + cell + 8), label, fill=(40, 40, 40, 255), font=font)
    sheet.save(os.path.join(OUT, "preview_sheet.png"))
    print("saved", len(images), "textures + preview_sheet.png")


if __name__ == "__main__":
    main()
