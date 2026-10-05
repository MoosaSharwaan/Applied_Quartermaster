---
navigation:
  parent: applied_quartermaster.md
  title: Farm automation
  icon: appliedquartermaster:me_farm_controller
  position: 40
item_ids:
- appliedquartermaster:me_farm_controller
- appliedquartermaster:me_redstone_plate
---

# Farm automation

<Row gap="20">
<BlockImage id="appliedquartermaster:me_farm_controller" scale="5" />
<ItemImage id="appliedquartermaster:me_redstone_plate" scale="5" />
</Row>

## ME Farm Controller

One controller per farm. Seen from the front:

* the cyan **network ports** (back and left side) connect to your ME network and use 1 channel;
* the red **farm port** (right side) takes the farm's own cables. Keep these cables apart from your main network.

Name the controller in an anvil or from the tablet. Right-click it to see the farm's status.

## ME Redstone Plate

A cable part that goes on the farm's cables, like a P2P tunnel. Up to 6 fit around one cable. When switched on it
powers the block it faces (strength 1 to 15).

* Green light: on. Red: off. Dark: the farm's controller is offline.
* Right-click a plate to switch it; sneak and right-click to change its strength.

## Farms app

The tablet's **Farms** app lists every farm, each with a switch for all its plates. Click a farm to see its plates: click
a plate's switch to turn it on or off, click its strength bar (or scroll over it) to set the strength, or use **All on**
and **All off**. The pencil renames a plate; right-click a farm or plate to rename it too, and click it while holding any
item to use that item as its icon.

<RecipeFor id="appliedquartermaster:me_farm_controller" />
<RecipeFor id="appliedquartermaster:me_redstone_plate" />
