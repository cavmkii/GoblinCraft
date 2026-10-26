# SeanCraft

A Fabric mod for Minecraft Java 1.21.1. It turns survival into a raid game: you head out from a safe spot, loot what you can, and get back before things get bad. If you die out there, you lose your kit. It also adds a character system with a big passive tree, use-based skills, gear built from parts, body-part health, a Tetris-style inventory, and two NPCs who hate each other.

Everyone needs the mod, the server and every player.

## Install

1. Install [Fabric Loader](https://fabricmc.net/use/installer/) for **1.21.1**.
2. Put [Fabric API](https://modrinth.com/mod/fabric-api) (the 1.21.1 build) and `seancraft-<version>.jar` in your `mods` folder.
3. Start a **new world**. Spawn points are only built in new worlds.

To build the jar yourself you need JDK 21. Run `./gradlew build`, and the jar shows up in `build/libs/`.

## Keys

| Key | What it does |
|---|---|
| E or J | Inventory screen (GEAR tab) |
| H | Inventory screen, STATUS tab |
| K | Inventory screen, WEB tab (passive tree) |
| Z, R, G, V, B | Active skills in your 5 ability slots |

The inventory screen has five tabs: GEAR, CRAFTING, SKILLS, STATUS and WEB. Creative mode keeps the vanilla inventory.

## What's different from vanilla

### Raids, spawn and hideout

- **Spawn points.** A new world gets 3 fixed spawn points, one at world spawn and two about 400 blocks out. Each has a small shelter with 3 chests and a barrel of starting gear, and the empty ones restock every 30 minutes. New players start at a random one, and so does anyone who dies without a bed.
- **Safe zones.** The area around a spawn point is safe, and so is your hideout. Anywhere else, you're **in raid**. The HUD in the bottom left says which one you're in.
- **Threat.** The longer a raid goes, the worse it gets. Threat 1 hits at 5 minutes, 2 at 20, 3 at 40 and 4 at 60. Mobs start spawning as elites with more health and damage, and hunting parties come after you.
- **Dying in raid.** By default you lose everything except what's in your secure pouch, and nothing drops. Dying in a safe zone is a normal death.
- **Hideout.** Craft a Hideout Bench (3 cobblestone on top, then plank / crafting table / plank, then 3 planks) and place it. Everything within 30 blocks is your hideout. Chests and barrels inside it are your stash.
- **Hideout modules.** The bench builds modules, and each one takes real time. You don't need everything at once: hit CONTRIBUTE to put in whatever you have from your inventory and stash. Once it's all in, it starts building.

  | Module | What it does |
  |---|---|
  | Workshop | Faster furnaces in the hideout; unlocks bigger pouches, rigs and backpacks |
  | Medstation | Unlocks meds; bleeds stop and fractures heal in the hideout |
  | Perimeter | Bigger hideout radius |
  | Intel center | More calm time per raid; warns you about hunting parties |

### Inventory

- **Grids.** Your inventory is a set of grids: pockets (5x2), a rig, a backpack, and a secure pouch. Items take up real space, so a sword is 1x3 and a chestplate is 3x3. The hotbar, armor and off-hand still work like vanilla.
- **Controls:**
  - click to pick up and click to put down;
  - **R** rotates;
  - **Shift**+click takes half;
  - **Ctrl**+click sends an item across;
  - **1-9** puts the item you're hovering on that hotbar slot;
  - right-click or Esc lets go.
- **Pickups** fill pockets, then rig, then backpack. Weapons and tools go to the hotbar first. If nothing has room, the item stays on the ground.
- **Rigs and backpacks** have to be empty before you can take them off. You start with a Scav vest (4x2), a Sling bag (3x3) and a 2x2 pouch. Bigger ones are made at the Hideout Bench.
- **Chests and barrels** open on the GEAR tab as grids too. Where you put things stays put, while hoppers still work like normal. Each one has a SORT button (merges stacks and packs it tight) and a TAKE ALL button. In your hideout, every stash chest shows up there, and STASH ALL dumps your pockets, rig and backpack into them. Sneak + use with an empty hand opens the old chest screen.
- **Vanilla screens** like the crafting table, furnace and anvil show your grids beside them. Click a grid item to grab it, then drop it in a slot, or the other way round.
- **Crafting.** The CRAFTING tab has a recipe list on the left and a crafting grid on the right. Right-clicking a crafting table opens the same tab.
  - Click a recipe and it gets laid out in the grid with what you have. Your hotbar, grids and stash all count.
  - Or lay it out yourself: click something in MATERIALS, then click a grid cell. Right-click puts down just one.
  - Click the result to craft, or Shift-click to craft as many as you can.
  - Anything left in the grid goes back when you close the screen.
  - 3x3 recipes need a crafting table within 6 blocks, or you have to be in the hideout. Otherwise you get 2x2.
  - Outside a hideout, the tab tells you how to make a Hideout Bench and pins it to the top of the list.
- **Ammo.** Bows and crossbows pull arrows from your rig, pockets or backpack.

### Health

- **Body parts.** Your health is split across 7 parts: head, thorax, stomach, two arms and two legs. Hits land on a part depending on where they came from, and falls hit your legs.
- **Injuries.** Parts can bleed, fracture, or get destroyed. A destroyed head or thorax kills you.
- **Locked hearts.** A destroyed limb locks away its share of your hearts. Food, regen and potions can't fill them until a surgical kit fixes the limb. Locked hearts show up dark and crossed out.
- **Meds:** bandage, tourniquet, splint, painkillers, field kit and surgical kit.
  - Use one from the hotbar and it treats whatever needs it most.
  - Or pick the body part yourself on the STATUS tab.
  - Meds show up in chests too.
- **Status effects** like poison and wither are listed on the STATUS tab, with a button for each cure you're carrying:
  - milk clears everything;
  - honey cures poison;
  - an antidote (made at the Medstation) cures every bad effect.

### Weapons and armor from parts

- Swords, axes, crossbows, shields and metal armor are made from parts: blades, handles, guards, wraps, plates and padding. The vanilla recipes for them are gone. Bows keep their vanilla recipe.
- Craft the parts on the CRAFTING tab. Blades are 2 ingots in a column, plates are 2 in a row, handles are 2 logs, planks or bones diagonally, and so on.
- Build the item at the **Weapon Bench** (a smithing table and a crafting table, shapeless):
  1. With the left slot empty, pick what to build.
  2. Each part gets its own labelled slot: Blade, Grip, Guard, Wrap, or Plates ×4, Padding, Straps for a chestplate.
  3. Fill them and hit ASSEMBLE.
- The standard parts give you exactly the vanilla item. Other materials change damage, swing speed, durability, armor and weight.
- Put an existing item in the left slot to swap its parts, or level a part up to 4 with its own material.

### Character

- **Levels.** You get class XP from killing mobs and mining ores, up to level 60. Each level gives a point for the passive web.
- **The web (K)** has about 120 nodes and no fixed classes. Go straight down one branch and you're a pure warrior, ranger or caster, or mix them however you want. Some nodes unlock active skills, which go in your 5 ability slots. Each archetype has its own resource: rage, stamina or mana.
- **Attributes.** Strength, Agility and Intelligence come from your skills and the web. Your highest one is your primary attribute, and it adds damage.
- **Skills** level up as you use them: swords, axes, blocking, mining, bows & crossbows, running, sneaking, swimming, enchanting, brewing, casting and farming. You lose 5% of your skill progress when you die. Firing a grappling hook (from the Grappling Hook Mod) also trains bows & crossbows.
- **Respec.** Use the Altar of Regret (2 gold ingots and a book over 3 crying obsidian). It wipes your web and kills you on the spot. Below level 21 you get every point back. From 21 you get 75% back, and from 41 you get 50% and it also needs an Orb of Regret.

### World

- Villages are about twice as common.
- Village chests almost always have meds and some supplies. Other vanilla chests have meds about half the time.
- Small supply caches (a barrel and some mossy slabs) are scattered around the overworld.
- Portals work like normal.

### Joe and Sean

- **Joe** is a small green goblin who wanders around looking for Sean, says stuff in chat, throws up, and steals from your pockets, rig or backpack (never your hotbar, gear or pouch). He leaves you alone unless you hit him, or unless someone says "baby wars" in chat. When he dies he drops everything he stole. He gets stronger as players progress.
- **Sean** is a travelling merchant. He sells Soda, Merch, a Glock, a Restraining Order (keeps Joe away), Diddy Oil and the 67 sword, all for emeralds.
- **Fights.** When Joe and Sean get close, they fight. There are spells everywhere, and anyone nearby gets hit too.
- **The Baby Wars.** Say "baby wars" 3 times in an hour and they start:
  - Joe sets up a camp and sends waves of babies at everyone.
  - Sean shows up with his security guards.
  - A Baby Bonnet puts you on Joe's side, and Merch puts you on Sean's.
  - Throw stuff in Joe's cauldron to curse the next wave.
- **Snow.** When it starts snowing on you, you say "awe man the sky is cumming!" in chat.

There's only ever one Joe and one Sean, and they're not always around. Each visit lasts 20 to 40 minutes, then they wander off for 30 to 60 minutes (`General.StayMinutes*` and `General.AwayMinutes*`). They won't leave in the middle of a fight or the Baby Wars, or while you're watching. If they die, they come back later.

## Config and commands

- **Config file:** `config/seancraft/seancraft.properties`. It's created on first run, and every setting has a comment. Run `/seancraft reload` after changing it.
- **Common changes:**
  - `Raid.DeathRule`: `LOSE_EVERYTHING`, `KEEP_SECURE` (normal drops, but you keep the pouch), or `VANILLA`.
  - `Raid.ThreatMinutes`: when each threat level starts (default `5,20,40,60`).
  - `Spawn.*`: number of spawn points, distance, safe radius, restock time.
- **Commands:**
  - `/seancraft where` shows where Joe and Sean are.
  - `/seancraft summon joe|sean`, `/seancraft babywars start|stop` and `/seancraft reload` need op.
  - `/character xp|skill|reset` is for testing the character system (op).

## Works with

- **FallingTree:** trees you chop go into your grids, and axes made from parts work with it.
- **Grappling Hook Mod:** needs Forge Config API Port too. Hooks go to your hotbar, and using them trains bows & crossbows.

Both were checked by reading their code. Neither has been played alongside this mod yet.
