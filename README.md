# InvSwitcher

World inventory switcher add-on for BentoBox. This add-on will work for any game modes.

The following are switched per-world:

* Inventory & armor
* Advancements
* Food level
* Experience
* Health
* Enderchest contents

## How to use

1. Place the addon jar in the addons folder of the BentoBox plugin
2. Restart the server
3. Done!
4. (Optional) If you would prefer to have achievements not broadcasted in your server chat when players change worlds, run the command `/gamerule announceAdvancements false` in-game, or in your server console by removing the "/" symbol. 

## Do not run two inventory managers

InvSwitcher must be the only plugin managing per-world inventories. Running it alongside
Multiverse-Inventories, PerWorldInventory, MultiInv or similar makes both plugins save and restore
the player on every world change, and they overwrite each other's data.

The symptom is disappearing items: pick up an item on your island, go to the lobby, come back, and
the island inventory is empty. Nothing appears in the console, because neither plugin is failing —
each is faithfully saving a player state the other has already rewritten. The InvSwitcher version
makes no difference, so if you are seeing this, look for a second inventory plugin first.

### Multiverse-Inventories

Two things commonly mislead admins:

* **Leaving the BentoBox worlds out of every inventory group does not help.** Groups control which
  worlds *share* an inventory, not which worlds Multiverse-Inventories handles. It still writes a
  per-world profile for a world that is in no group.
* **`/mv remove <world>` does not help either.** Multiverse-Core re-registers BentoBox worlds as
  they are created, so the removal is undone on the next restart. `auto-import-3rd-party-worlds:
  false` does not prevent it — that only suppresses the import sweep run when Multiverse-Core
  starts, which is before BentoBox has created its worlds.

Multiverse-Inventories has no config option to ignore a world, but it does have a bypass
permission. Enable it in the Multiverse-Inventories `config.yml` (it ships as `false`):

```yml
share-handling:
  enable-bypass-permissions: true
```

Then grant `mvinv.bypass.world.<world>` for each BentoBox world — including its nether and end — to
every player. With LuckPerms:

```
lp group default permission set mvinv.bypass.world.bskyblock_world true
lp group default permission set mvinv.bypass.world.bskyblock_world_nether true
lp group default permission set mvinv.bypass.world.bskyblock_world_the_end true
```

Setting the nodes on a group every player inherits (such as `default`) covers new players
automatically. Repeat for each game mode world you run.

**Operators do not get this permission automatically** — it has to be granted explicitly. Testing
as an op without it looks exactly like the fix not working.

Use one node per world. Avoid `mvinv.bypass.world.*`, which switches Multiverse-Inventories off for
every world, including the ones you still want it to manage.

## Config.yml

The config allows to define which worlds that InvSwitcher should operate, and what aspects should be kept separate.

```
# Worlds to operate. Nether and End worlds are automatically included.
worlds:
- acidisland_world
- oneblock_world
- boxed_world
- bskyblock_world
options:
  # 
  # Per-world settings. Gamemode means Survivial, Creative, etc.
  inventory: true
  health: true
  food: true
  advancements: true
  gamemode: true
  experience: true
  location: true
  ender-chest: true
```

## Commands

There are no commands.

## What it does
This addon will give players a separate inventory, enderchest, health, food level, advancements and experience for each gamemode installed and their corresponding worlds. It enables players to play each gamemode independently of each other.
