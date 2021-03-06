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
