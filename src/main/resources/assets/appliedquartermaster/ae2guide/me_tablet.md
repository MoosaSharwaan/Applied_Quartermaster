---
navigation:
  parent: applied_quartermaster.md
  title: ME Tablet
  icon: appliedquartermaster:me_tablet
  position: 10
item_ids:
- appliedquartermaster:me_tablet
---

# ME Tablet

<ItemImage id="appliedquartermaster:me_tablet" scale="4" />

The ME Tablet runs your network from anywhere in range. It works like a small tablet: right-click to open it on its
**home screen**, click an app to open it, and use the dock at the bottom or the home button on the right edge to
switch apps. The status bar on top shows whether the network is reached, the in-game day and time, and the battery.

## Apps

* **Wireless terminals**: one app for each terminal module. It opens that real AE2 terminal inside the tablet's frame;
  press **Esc** or the **Tablet** button in the status bar to come back.
* **Network**: every AE2 device on the network by kind, and a **Statistics** view (see below).
* **Farms**: every Farm Controller with an on/off switch; open one to switch each redstone plate and set its strength.
* **Library**, **Armory** and **Tools**: appear while their block is on the linked network.
* **Settings**: terminal modules, range upgrades and the battery.

Apps that are not there yet are shown dimmed on the home screen. Right-click an app (on the home screen or in the
dock) to pin it: the tablet then opens straight on that app. Sneak and right-click to open the home screen instead.

## Modules

Open **Settings** and put a wireless terminal in a module slot: an <ItemLink id="ae2:wireless_terminal" />, a
<ItemLink id="ae2:wireless_crafting_terminal" />, or a wireless terminal from another addon (AE2WTLib's, AdvancedAE's
and others).

The tablet reaches the network the terminal module is linked to, through a powered
<ItemLink id="ae2:wireless_access_point" /> in range. Link the terminal first, as you normally would. Out of range,
the tablet says so and offers a shortcut to Settings.

The two upgrade slots take range upgrades: each <ItemLink id="ae2:wireless_booster" /> adds the access point's range
again. With AEInfinityBooster installed, its Infinity Range Booster removes the range limit in the access point's
dimension and its Dimension Card works from any dimension.

## Network statistics

The Statistics view shows storage used in drives and ME Chests, item types, items and fluids stored, energy, and which
mods the stored items come from. **Falling stock** lists items whose amount went down over the last 10 minutes, hour
or day, by how much, and roughly when each runs out at that rate. The tablet starts noting stock the first time it
reaches a network, every 10 minutes, so this list fills in over time. Nothing is saved to disk.

## Storage apps

* Click an item to pick it up (it swaps with the item you hold); shift-click to move it to your inventory.
* Shift-click an item in your **Pockets** (your inventory, on the right) to store it on the network.
* The buttons in the app bar sort, show or hide free spots, and switch between Large, Medium and Small views.

## Opening it from anywhere

Set a key for **Open ME Tablet** under Options, Controls, Key Binds. It opens the tablet wherever you carry it: in your
hand, in your inventory, or worn in a Curios **curio** slot when Curios is installed. Sneak while pressing it to open
the home screen.

<RecipeFor id="appliedquartermaster:me_tablet" />
