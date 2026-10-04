# Art tools

These Python scripts drew every texture and preview image for Applied Quartermaster during the design phase.
All art is drawn as pixel maps in code, so colours and shapes can be changed by editing a script and running it again.

Requirements: Python 3 and Pillow (`pip install pillow`). The preview sheets use the DejaVu fonts.

| Script | Makes |
| --- | --- |
| `make_textures.py` | ME Tablet item and the app icons |
| `make_library_v2.py`, `make_library_v3.py` | ME Library faces: shelves with 0 to 8 books, lit and unlit |
| `make_racks.py`, `make_racks_v2.py` | ME Armory and ME Tool Rack faces, all 16 connected versions per face, lit and unlit |
| `make_redstone.py`, `make_controller_variants.py`, `make_automation_final.py` | ME Farm Controller (switchboard) |
| `make_plate.py`, `make_plate_bus.py`, `make_redstone_scene.py` | ME Redstone Plate states and the cable previews |
| `make_views_v4.py`, `make_automation_view2.py`, `make_tablet_shell.py` | Tablet screen mockups |

Scripts write their output next to themselves; copy finished textures into `src/main/resources/assets/appliedquartermaster/textures/`.
A few preview scripts show AE2's own item icons for illustration and expect a local copy of AE2's textures; the paths at the top of those scripts point to it and need adjusting on your machine. The mod's own textures never use AE2's art.
