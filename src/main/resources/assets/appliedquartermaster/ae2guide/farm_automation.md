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

## Automation tab

The tablet's **Automation** tab lists every farm. Click a farm to see its plates, click a plate to switch it, scroll over
it to change its strength, or use **All on** and **All off**. Right-click a farm or plate to rename it, and click it while
holding any item to use that item as its icon.

<RecipeFor id="appliedquartermaster:me_farm_controller" />
<RecipeFor id="appliedquartermaster:me_redstone_plate" />
