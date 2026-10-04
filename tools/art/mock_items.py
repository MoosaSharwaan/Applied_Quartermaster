"""Original 16x16 item sprites used only to illustrate custom farm/plate icons in mockups."""
from PIL import Image

PAL = {
    ".": (0, 0, 0, 0), "k": (40, 36, 40, 255), "w": (240, 240, 240, 255), "l": (200, 200, 205, 255),
    "g": (150, 152, 160, 255), "d": (100, 100, 108, 255),
    "y": (240, 200, 70, 255), "Y": (200, 150, 40, 255), "o": (150, 110, 40, 255),
    "G": (110, 190, 70, 255), "h": (60, 130, 40, 255),
    "r": (200, 60, 50, 255), "R": (140, 30, 30, 255), "p": (180, 120, 90, 255), "P": (130, 80, 60, 255),
    "s": (120, 200, 90, 255), "S": (70, 150, 60, 255), "c": (255, 230, 120, 255),
    "L": (255, 140, 30, 255), "M": (230, 80, 20, 255), "b": (110, 80, 50, 255), "B": (80, 56, 34, 255),
    "n": (170, 40, 60, 255), "N": (110, 20, 40, 255), "q": (90, 60, 40, 255), "e": (180, 180, 190, 255),
}


def sprite(rows):
    im = Image.new("RGBA", (16, 16))
    for y, r in enumerate(rows):
        for x, ch in enumerate(r):
            im.putpixel((x, y), PAL[ch])
    return im


IRON_INGOT = sprite([
    "................", "................", "................", "................",
    "......kkkkkk....", "....kkwwwwlkk...", "..kkwwllllllgk..", ".kwllllllllgdk..",
    ".kggllllllgddk..", ".kdggggggdddk...", "..kkdddddddk....", "....kkkkkkk.....",
    "................", "................", "................", "................",
])
ROTTEN_FLESH = sprite([
    "................", "................", "....kkk.........", "...kpPpk........",
    "..kpPpppkk......", "..kpppPppPk.....", "...kpPppppPk....", "...kSppPppppk...",
    "....kpppSpPpk...", ".....kPpppppPk..", "......kppPSpk...", ".......kPppk....",
    "........kkk.....", "................", "................", "................",
])
WHEAT = sprite([
    "..........k.....", ".........kyk....", "........kyYk.k..", ".......kyYk.kyk.",
    "......kyYk.kyYk.", ".....kyYk.kyYk..", "....kyYk.kyYk...", "...kyYkkyYk.....",
    "..kyYkyYYk......", "..kYYYYk........", ".kYoYk..........", "kYok............",
    "kok.............", "kk..............", "................", "................",
])
HONEYCOMB = sprite([
    "................", "....kkkkkkk.....", "...kyyyyyyyk....", "..kyccYycccYk...",
    "..kycYyycYyYk...", "..kyYyyyYyyyk...", "..kyccYycccYk...", "..kycYyycYyYk...",
    "..kyYyyyYyyyk...", "..kyccYycccYk...", "..kycYyycYyYk...", "...kYYYYYYYk....",
    "....kkkkkkk.....", "................", "................", "................",
])
NETHER_WART = sprite([
    "................", "................", "......kkk.......", ".....knnnk......",
    "....knnNnnk.....", "...knNnnNnk.....", "...knnnnnnk.kk..", "....kNnnNk.knnk.",
    ".....kkqkkknNnk.", "......kqk..knnk.", "......kqk...kk..", ".....kqqqk......",
    "................", "................", "................", "................",
])
SLIME_BALL = sprite([
    "................", "................", "................", ".....kkkkk......",
    "....ksssssk.....", "...kswsssSSk....", "...kswssssSk....", "...ksssssSSk....",
    "...kssssSSSk....", "....kSSSSSk.....", ".....kkkkk......", "................",
    "................", "................", "................", "................",
])
LAMP = sprite([
    "................", ".kkkkkkkkkkkkkk.", ".kccLccLccLccLk.", ".kcLccLccLccLck.",
    ".kLccLccLccLcck.", ".kccLccLccLccLk.", ".kcLccLccLccLck.", ".kLccLccLccLcck.",
    ".kccLccLccLccLk.", ".kcLccLccLccLck.", ".kLccLccLccLcck.", ".kccLccLccLccLk.",
    ".kcLccLccLccLck.", ".kLccLccLccLcck.", ".kkkkkkkkkkkkkk.", "................",
])
IRON_SWORD = sprite([
    "..............kk", ".............kwk", "............kwlk", "...........kwlk.",
    "..........kwlk..", ".........kwlk...", "........kwlk....", "...k...kwlk.....",
    "...kk.kwlk......", "....kkwlk.......", "....kddk........", "...kbdkk........",
    "..kbbk.kk.......", ".kbbk...........", "kBbk............", "kkk.............",
])
HOPPER = sprite([
    "................", ".kkkkkkkkkkkkkk.", ".kggggggggggggk.", ".kgkkkkkkkkkkgk.",
    ".kgk........kgk.", ".kgk........kgk.", ".kggggggggggggk.", "..kggggggggggk..",
    "...kggggggggk...", "....kdddddk.....", ".....kdddk......", ".....kdddk......",
    "......kdk.......", "......kkk.......", "................", "................",
])
LAVA = sprite([
    "................", "...kkkkkkkkkk...", "..kgllllllllgk..", "..kgLMLLMLLLgk..",
    "..kgLLLMLLMLgk..", "..kgMLLLLMLLgk..", "..kgLLMLLLLMgk..", "..kgLMLLMLLLgk..",
    "...kgLLLMLLgk...", "...kgMLLLLMgk...", "....kgggggk.....", ".....kkkkk......",
    "................", "................", "................", "................",
])
PISTON = sprite([
    "kkkkkkkkkkkkkkkk", "kbbbbbbbbbbbbbbk", "kbBbBbBbBbBbBbBk", "kkkkkkkkkkkkkkkk",
    "kddddddeedddddk.", "kdgggggeegggggdk", "kdgggggeegggggdk", "kdggggkeekggggdk",
    "kdggggkeekggggdk", "kdgggggeegggggdk", "kdgggggeegggggdk", "kdgggggeegggggdk",
    "kdgggggggggggggk", "kdddddddddddddk.", "kkkkkkkkkkkkkkkk", "................",
])
DOOR = sprite([
    "...kkkkkkkkkk...", "...kbbbbbbbbk...", "...kbkkkkkkbk...", "...kbkbbbbkbk...",
    "...kbkbbbbkbk...", "...kbkkkkkkbk...", "...kbbbbbbbbk...", "...kbbbbbbbbk...",
    "...kbkkkkkkbk...", "...kbkbbbbkbkk..", "...kbkbbbbkbyk..", "...kbkbbbbkbk...",
    "...kbkkkkkkbk...", "...kbbbbbbbbk...", "...kkkkkkkkkk...", "................",
])
