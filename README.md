# Applied Quartermaster

An addon for [Applied Energistics 2](https://github.com/AppliedEnergistics/Applied-Energistics-2) that puts your whole ME
network in your hands. The **ME Tablet** is a handheld hub with a tab for each part of your base, and four blocks give your
guide books, weapons, tools and farms a home on the network.

> **Status: beta.** Built for **All the Mods 11** (Minecraft 26.1.2, NeoForge). Every screenshot below is real: the mod's
> automated self-test took them in game. Feedback and bug reports are welcome in [Issues](../../issues).

![A small base: Farm Controller, AE2 controller, two ME Libraries with two merged ME Armories on top, and an ME Tool Rack](docs/screenshots/overview.png)

| Block or item | What it does |
| --- | --- |
| **ME Tablet** | Handheld hub: opens your AE2 terminals and has a tab for each kind of storage block on your network |
| **ME Library** | Holds 8 guide books; read them from the tablet |
| **ME Armory** | Holds 8 weapons and mining tools |
| **ME Tool Rack** | Holds 8 utility tools (wrenches, memory cards, network tools…) |
| **ME Farm Controller** | One per farm; connects the farm's own cables to your network |
| **ME Redstone Plate** | Cable part that switches a machine on and off with redstone |

## Contents

1. [Getting started](#getting-started)
2. [The ME Tablet](#the-me-tablet)
3. [ME Library](#me-library)
4. [ME Armory and ME Tool Rack](#me-armory-and-me-tool-rack)
5. [Farm automation](#farm-automation)
6. [Recipes](#recipes)
7. [In-game guide](#in-game-guide)
8. [Troubleshooting](#troubleshooting)
9. [Other AE2 addons](#other-ae2-addons)
10. [Requirements, download and building](#requirements)

## Getting started

1. **Craft an ME Tablet** ([recipe](#recipes)).
2. **Link a wireless terminal** to your network the normal AE2 way: put an AE2 *Wireless Terminal* (or *Wireless
   Crafting Terminal*, or an AE2WTLib universal terminal) in a *Wireless Access Point*.
3. **Right-click the tablet.** It opens on the **Modules** page. Put the linked terminal in a module slot, and a tab with
   the terminal's icon appears at the top.
4. **Place your storage blocks** (ME Library, ME Armory, ME Tool Rack, ME Farm Controller) on that network. As soon as one
   is on the network, its tab appears on the tablet.

The tablet reaches the network through a powered Wireless Access Point in range, exactly like the terminal itself. Add
Wireless Boosters to the tablet for more range (see [Upgrades and battery](#upgrades-and-battery)).

## The ME Tablet

### Holding it

With your off hand empty, you hold the tablet in **both hands**, like a map, and it tilts up as you look down at it. With
something in your off hand, you hold it in one hand. Other players see you holding it up to read. In inventories it keeps
its flat icon.

<p>
<img src="docs/screenshots/tablet_two_hands.png" alt="The tablet held in both hands in first person" width="560">
<img src="docs/screenshots/tablet_third_person.png" alt="The tablet held in third person" width="300">
</p>

### Opening it

- **Right-click:** opens the *pinned* tab (see below). With nothing pinned, it opens the Modules page.
- **Sneak + right-click:** always opens the Modules page.
- **Open ME Tablet key:** opens the tablet from anywhere you carry it: your hand, your inventory or a Curios slot. It
  behaves like right-click (sneak for the Modules page). It has no key by default, like AE2's terminal keys: set one
  under *Options → Controls → Key Binds → Applied Quartermaster*.
- **Esc** closes the tablet. In an AE2 terminal opened from the tablet, **Esc goes back to the tablet** instead.

### Wearing it (Curios)

With [Curios API](https://modrinth.com/mod/curios) installed, the tablet fits in the **curio** slot, the same slot
AE2WTLib's terminals use. The mod adds one curio slot to players for it. Worn there, it does everything it does in your
hand: open it with the **Open ME Tablet** key, and its terminals, Devices tab and storage tabs all work, with module
changes saved back to the worn tablet.

![The ME Tablet worn in the curio slot](docs/screenshots/tablet_curios.png)

### Modules page

Open it with the **gear** at the top right.

![The Modules page with a Wireless Terminal module, two Wireless Boosters and the battery](docs/screenshots/tablet_modules.png)

- **Module slots (4):** each terminal you put here gets its own tab. Clicking that tab opens the **real AE2 terminal**, with
  everything it normally does (search, crafting, sorting). The terminal's settings, power and crafting grid are saved back
  into the module.
- **Pin:** click the small pin on a module (or right-click any tab) to make it the tab that opens first. The pinned tab
  has a gold pin. Click again to unpin.

#### Upgrades and battery

- **Upgrades (2 slots):** each AE2 **Wireless Booster** adds the access point's range again (up to 4 per slot), so two
  boosters triple it. With [AEInfinityBooster](https://modrinth.com/mod/aeinfinitybooster), its **Infinity Range
  Booster** removes the range limit in the access point's dimension, and its **Dimension Card** works from any dimension.
- **Battery:** the charge of your terminal module. Charge the terminal in an AE2 Charger as usual.

![The AE2 Wireless Terminal opened from the tablet](docs/screenshots/terminal_from_tablet.png)

### Storage tabs: Library, Armory and Tools

Each tab shows everything stored in that kind of block across your whole network, in AE2's terminal style.

![The Armory tab, Large view](docs/screenshots/tab_armory_large.png)

| Action | What happens |
| --- | --- |
| **Click** an item | Picks it up onto your cursor. If you are already holding an item, the two swap. |
| **Shift-click** an item | Moves it into your inventory. |
| **Click** a book (Library tab) | Opens the book to read. Right-click picks it up instead. |
| **Shift-click** an item in your inventory | Stores it in the first free spot on the network. |
| **Click an empty box** while holding an item | Stores the held item. |
| **Scroll** | Scrolls the list. |
| **Search** box | Filters by name. Right-click the box to clear it. |

Only real free spots are drawn as boxes: two Armories holding 7 items show 9 empty boxes. The counter in the header
(`7 / 16`) shows used and total spots.

**Left toolbar:**

- **Sort:** storage order, A–Z or Z–A.
- **Filter:** show or hide the free spots.
- **View size:** Large (5 per row, big icons with names), Medium (8 per row) or Small (17 per row, AE2 size). Each tab
  remembers its own size.

<p>
<img src="docs/screenshots/tab_armory_medium.png" alt="Medium view" width="49%">
<img src="docs/screenshots/tab_armory_small.png" alt="Small view" width="49%">
</p>

### Automation tab

See [Farm automation](#farm-automation).

### Devices tab

The **Devices** tab is always there while the tablet reaches a network. It lists every AE2 device on the network,
grouped by kind, much like AE2's Network Status screen, but you can look inside each group and find the blocks.

![The Devices tab](docs/screenshots/tab_devices.png)

* Each kind shows its icon, how many there are, and a status light: green when all are working, red when any are
  offline (the count of offline ones is shown).
* Click a kind to list each device of it: its position (and dimension when not the Overworld), a state light
  (**Active**, **No channel**, **No power** or **Starting**), the channels it carries and its power use in AE/t.
  Hover a row for full details.

![One kind's devices](docs/screenshots/tab_devices_list.png)

* Click a row to **locate** the device: the tablet tells you its distance and direction, and a beam of light rises
  from it for 10 seconds.
* The sort button orders the list by position, problems first or problems last (A–Z or Z–A for kinds).
  The search box filters by name or position. **<** goes back to the kinds.

![Locating a device](docs/screenshots/devices_locate.png)

## ME Library

Each ME Library holds **8 guide books** and uses **1 channel**. Libraries that touch pass the network along, so a row of
libraries needs only one cable (up to 8 on a normal cable). The shelves on the sides show how many books are inside, and
the lights glow only while the block has power and a channel.

![The Library tab](docs/screenshots/tab_library.png)

- **Accepted:** guide books from any mod (Patchouli, Modonomicon, GuideME and others), written books and books and quills.
  Items whose name says book, guide, manual, journal, codex, lexicon and so on are accepted too.
- **Not accepted:** blank books, enchanted books and Apothic Enchanting's tomes.
- **Reading:** click a book on the Library tab. Written books open right away; guide books open as if you had
  right-clicked them.
- **Right-click the block** to open its own 8 slots:

![The ME Library's own screen](docs/screenshots/library_block_screen.png)

Pack makers can add or block items with the item tags `appliedquartermaster:library_books` and
`appliedquartermaster:library_blocked`.

## ME Armory and ME Tool Rack

- **ME Armory:** 8 weapons and mining tools (swords, axes, pickaxes, shovels, hoes, bows, crossbows, tridents, maces,
  shields, and anything with a tool or weapon component).
- **ME Tool Rack:** 8 utility tools: wrenches, memory cards, network tools and any other single item that is not armour or
  food.
- Each uses **1 channel**. Place several **side by side or stacked, facing the same way**, and their frames merge into
  one cabinet.
- Blocks face you when you place them. Their lights glow only with power and a channel.

![The Tools tab](docs/screenshots/tab_tools.png)

Pack makers can extend them with the item tags `appliedquartermaster:armory_items` and
`appliedquartermaster:tool_rack_items`.

## Farm automation

Switch your farms on and off from anywhere.

![A Farm Controller with its farm cable: the top plate is on and lights the lamp; the west plate is off (red ring)](docs/screenshots/farm_in_world.png)

### 1. Place an ME Farm Controller

Seen from the front (the switchboard face):

- the cyan **network ports** on the **back and left** connect to your main ME network and use **1 channel**;
- the red **farm port** on the **right** takes the farm's own cables.

Keep the farm's cables apart from your main network cables. Name the controller in an anvil (or from the tablet) to name
the farm. Right-click it with an empty hand to see the farm's status; sneak-right-click clears its icon.

### 2. Put ME Redstone Plates on the farm cables

Run cables from the farm port to your machines and place **ME Redstone Plates** on them, facing the block you want to
power, the same way you place a P2P tunnel. Up to 6 plates fit around one cable.

- **Green ring:** on. **Red ring:** off. **Dark:** the farm's controller is offline (no power or channel).
- When on, a plate powers the block it faces with a signal of **1 to 15**.
- **Right-click** a plate to switch it. **Sneak-right-click** changes its strength.

### 3. Control farms from the tablet

The **Automation** tab appears when a Farm Controller is on your network.

<p>
<img src="docs/screenshots/tab_automation_farms.png" alt="The farm list" width="49%">
<img src="docs/screenshots/tab_automation_plates.png" alt="Inside a farm: its plates" width="49%">
</p>

| Where | Action | What happens |
| --- | --- | --- |
| Farm list | Click a farm | Opens it to show its plates |
| Inside a farm | Click a plate | Switches it on or off |
| Inside a farm | Scroll over a plate | Changes its signal strength |
| Inside a farm | **All on** / **All off** | Switches every plate of the farm |
| Inside a farm | **<** | Goes back to the farm list |
| Anywhere | Right-click a farm or plate | Renames it |
| Anywhere | Click while holding any item | Uses that item as the icon (the item is not used up) |
| Anywhere | Sneak-right-click | Clears the icon |
| With JEI | Drag an item from JEI onto a farm or plate | Uses it as the icon |

Each farm has a **status light**: green when running, red when everything is off or the farm is offline. Plates without
their own icon show their light ring in its current colour. New plates are named after the side they face ("Top plate",
"West plate").

## Recipes

Everything is crafted from AE2 parts.

![Recipes for all six items, from the in-game guide](docs/screenshots/recipes.png)

| Item | Main ingredients |
| --- | --- |
| ME Tablet | Wireless receiver, logic and engineering processors, energy cell, quartz glass, iron |
| ME Library | Bookshelves, engineering processor, fluix glass cable, iron |
| ME Armory | Iron sword, shield, engineering processor, fluix glass cable, iron |
| ME Tool Rack | Certus quartz wrench, iron bars, engineering processor, fluix glass cable, iron |
| ME Farm Controller | Repeater, comparator, logic processor, fluix glass cable, iron |
| ME Redstone Plate (×2) | Fluix crystal, redstone torch, iron |

## In-game guide

The mod adds an **Applied Quartermaster** section to AE2's guide, with pages for the tablet, each block, farm automation
and all recipes. Hold **G** (the guide key) over any of the mod's items to jump to its page.

![The Applied Quartermaster section in AE2's guide](docs/screenshots/guide_index.png)

## Troubleshooting

| What you see | Fix |
| --- | --- |
| *Install a Wireless Terminal module…* | Put a wireless terminal in a module slot on the Modules page. |
| *Link the Wireless Terminal module to your network…* | Link the terminal in a Wireless Access Point, then put it back in the tablet. |
| *Out of range…* | Get closer to a powered Wireless Access Point, or add Wireless Boosters to the tablet. |
| *No ME Library (Armory, Tool Rack, Farm Controller) on this network* | Place one on the network the terminal is linked to. |
| A block's lights are off | The block has no power or no channel. A normal cable carries 8 channels. |
| Plates are dark | The Farm Controller is offline: check that its network port has power and a channel. |

## Other AE2 addons

Applied Quartermaster doesn't need any other addon, and doesn't change how they work. Where they meet the tablet:

- **Terminals:** any wireless terminal built on AE2's goes in a module slot and opens its own screen from the tablet,
  including AE2WTLib's (pattern access, pattern encoding, universal) and AdvancedAE's Wireless Quantum Crafter Terminal.
- **Curios:** wear the tablet in the curio slot and open it with the Open ME Tablet key (see
  [Wearing it](#wearing-it-curios)).
- **Range upgrades:** AE2's Wireless Booster, and AEInfinityBooster's Infinity Range Booster and Dimension Card.
- **Devices tab:** every addon machine on the network shows up with its own icon, state, channels and power.
  Multiblock parts (crafting units, AdvancedAE's quantum computer) appear once the multiblock is built, as in AE2.
- **Storage blocks:** addon weapons and tools go in the ME Armory, and addon tools (for example the ME Placement Tools)
  in the ME Tool Rack.

Tested in the game with the NeoForge 26.1.2 builds of these addons, each on its own and all together:

| Addon | Tested version | What was checked |
| --- | --- | --- |
| AE2 Wireless Terminals (AE2WTLib) | 26.1.1-beta | Its terminals in module slots; the Pattern Access Terminal opens from the tablet and Esc returns to it |
| AdvancedAE | 26.1.7 | Its wireless terminal in a module slot; its machines in the Devices tab |
| AEInfinityBooster | 26.1.2-1.0.0.57 | Both cards fit the upgrade slots and set unlimited range (same dimension, or any dimension) |
| AE2 Lightning Tech | 1.0.1alpha | Its machines in the Devices tab; its tool in the Tool Rack |
| AE2 Crystal Science | 26.1.2-1.1.12 | Its machines in the Devices tab; its swords and tools in the Armory |
| ME Placement Tool | 2.1.3-beta1 | Its three tools in the Tool Rack |
| Bigger AE2 | 26.1.2.1 | Loads alongside; no overlap |
| AE2 OMNI Cells | 1.1.7 | Loads alongside; no overlap |
| AE2 Import Export Card | 26.1.2-2.3.1 | Loads alongside; no overlap |
| AE2 Toggleable View Cell | 26.1-1.0.1 | Loads alongside; no overlap |
| Myotus Lib | 26.1.2-26.0.0 | Loads alongside; no overlap |

AE2 Lightning Tech shows a "Warning while loading mods" screen at start-up about its own code; click
**Proceed to main menu** and it works normally. That warning comes from Lightning Tech, not this mod.

### For modpack makers

Which items fit where is set by item tags, so other addons can be added with a datapack or KubeJS:

| Tag | Used for |
| --- | --- |
| `appliedquartermaster:tablet_range_boosters` | Upgrade slot: each one adds the access point's range again |
| `appliedquartermaster:tablet_infinite_range` | Upgrade slot: no range limit in the access point's dimension |
| `appliedquartermaster:tablet_any_dimension` | Upgrade slot: no range limit, from any dimension |
| `appliedquartermaster:library_books` / `library_blocked` | Extra books for the ME Library, or books it must refuse |
| `appliedquartermaster:armory_items` | Extra items for the ME Armory |
| `appliedquartermaster:tool_rack_items` | Extra items for the ME Tool Rack |

## Requirements

| | Version |
| --- | --- |
| Minecraft | 26.1.2 |
| NeoForge | 26.1.2.107 or newer |
| Applied Energistics 2 | 26.1.8-alpha or newer |
| AE2 Wireless Terminals (AE2WTLib) | Optional: more wireless terminals for the module slots |
| AEInfinityBooster | Optional: unlimited range and cross-dimension cards for the upgrade slots |
| Curios API | Optional: wear the tablet in a curio slot (15.0.0+26.1.2 or newer) |
| JEI | Optional: drag items onto farms and plates to set their icons |

## Download

Every push builds the mod on GitHub: open the **Actions** tab, pick the latest run and download the
`appliedquartermaster-jar` file. Tagged versions (for example `v0.1.0`) appear under **Releases** with the jar attached.

## Building it yourself

You need **Java 25**.

```
git clone https://github.com/MoosaSharwaan/Applied_Quartermaster.git
cd Applied_Quartermaster
./gradlew build        # Windows: gradlew.bat build
```

The jar appears in `build/libs/`. Put it in your mods folder next to AE2.

### Development self-test

`./gradlew runSelftest` starts the game, builds a small test network in the singleplayer world `aqtest` (create it
first; a superflat world works best) and opens every tablet page and guide page. It checks storing, taking, recipes and
redstone output, saves screenshots to `run/screenshots/aq_*.png` and quits. It is switched off in normal play. The
screenshots in this README come from it.

## Credits and licence

Code and art by MoosaSharwaan. Early design mockups are kept in [`docs/design`](docs/design).
Licensed under the [GNU LGPL v3](LICENSE), the same licence as AE2.
