# GoblinCraft

A Fabric mod for Minecraft Java 1.21.1. It started as a port of [ValheimGoblinPack](https://github.com/cavmkii/ValheimGoblinPack) (two unique, persistent NPCs). It now also has a character system, and the mechanics of [TarkoValheim](https://github.com/cavmkii/TarkoValheim): raids and hideouts, body-part health, modular weapons and armor, and a Tetris inventory. Where Valheim and Minecraft differ, the closest Minecraft equivalent is used. Those substitutions are listed [below](#what-changed-from-the-valheim-version).

Everything runs on the server. Clients need the mod too (for the entity renderers and items), but no client config.

## Joe

A small goblin (a green-tinted piglin, 0.8x size) who roams around looking for Sean.

- **Wanders.** He walks toward far-off points, and about half the time (`Joe.SeanSeekChance`) toward Sean's last known position. He avoids oceans and rivers.
- **Only talks about Sean or the Baby Wars.** Every 18–40 s, if a player is within 40 blocks, he says a line in chat, often using the nearest player's name. His dialogue is in `config/goblincraft/joe_lines.txt`, created on first run. Edit it, then run `/goblincraft reload`.
- **Throws up.** Every 1–2.5 minutes, if a player is within 40 blocks, he stops and spends four seconds spraying slime (`Joe.PukeEnabled`, `Joe.PukeIntervalMin/Max`).
- **Steals.** He walks up to a nearby player and takes up to 10 items from one random stack. He never takes from your hand or armor slots. Then he gloats and runs off. Everything he steals is saved with the world, and killing him drops all of it with its enchantments and durability intact.
- **Passive until hit.** Joe doesn't target a player until that player damages him. He then stays hostile to that player for 90 s (`Joe.ProvokeSeconds`). Other players stay safe unless they hit him too.
- **"babywars".** If anyone types `babywars` or `baby wars` in chat (case and punctuation don't matter; `/say` and `/me` count), Joe turns hostile to all players for 120 s. If the speaker is within 400 blocks, he charges at them. The phrases are configurable (`Joe.RagePhrases`).
- **Scales with "player level".** Minecraft has no skills, so the mod uses one of two stand-ins:
  - **Skills:** the highest XP level among players within 80 blocks, out of 50 (`Scaling.SkillsMaxLevel`).
  - **Bosses:** how many of seven progression advancements any online player has: Acquire Hardware, the Nether, Into Fire (blaze rod), Eye Spy, Free the End, Bring Home the Beacon (needs the Wither) and Cover Me in Debris. The list is configurable (`Scaling.BossAdvancements`).

  By default it uses whichever is higher. Both map to 0–1, which sets his health (0.6x–10x of 40) and his damage (0.25x–2.5x of 6). His name tag shows `Joe (Lv N)`.
- **Unique, and respawns.** There is only ever one Joe (and one Sean). Any other copy, including one made with `/summon`, removes itself. When Joe dies, he comes back ten minutes later 60–140 blocks from a random player in the overworld.
- **Doesn't get lost.** If no player has been within 128 blocks of him for 5 minutes (`General.StrandedMinutes`), he turns up near a random player again. Minecraft only runs mobs in loaded chunks, so without this he'd freeze wherever the players last left him.

## Sean

A travelling merchant in a wandering trader's robes.

- He walks a few hundred blocks to a new spot, camps there for 3–6 minutes, then moves on. While any player is within 15 blocks, he stops and stays open for business.
- Right-click him to open the normal villager trade screen. Prices are in emeralds, and he never runs out of stock.
- He's a tank (1000 HP) and takes only 10% damage from ordinary mobs, so zombies don't keep killing the shop. Players can still kill him, and he respawns after 20 minutes.

**Sean's stock:**

| Item | Type | Effect | Price |
|---|---|---|---|
| Soda ×5 | drink (food) | 6 hunger and high saturation, plus Health Boost II for 20 min | 1 emerald |
| Merch | chestplate | black leather look, no armor | 3 emeralds |
| Glock | ranged | semi-automatic crossbow: no loading, a shot every 4 ticks, uses arrows (tipped arrows work) | 30 emeralds |
| Restraining Order | carried item | Joe must stay 15 blocks away from you: no stealing, no taunting, he backs off. Void if you hit him; doesn't cover the Baby Wars rage | 12 emeralds |
| Diddy Oil ×3 | drink | Speed I and Resistance I for 5 min | 5 emeralds |
| 67 | sword | end-game fists: 13 attack damage, fast swings, fire-resistant | 6 emerald blocks + 7 diamonds |

## The Baby Wars

Say "baby wars" in chat three times within an hour (anyone, in total; `BabyWars.TriggerCount` / `TriggerWindowMinutes`) and the war starts near whoever said it last:

- Everyone sees **THE BABY WARS HAVE BEGUN**, and a thunderstorm runs until it's over.
- **Joe** sets up a war camp 25–40 blocks away and commands from it. The camp is a cauldron on a campfire, a skull totem and two red banners.
- **Joe's baby army** pours out of the camp in waves (4 by default, 6 babies then +3 each wave). The next wave comes after 75 s, or sooner once most of the current one is dead. The babies have toddler proportions:
  - **Diaper Commando:** a brawler.
  - **Bottle Grenadier:** throws glass bottles.
  - **Binky Berserker:** bigger and tougher, from wave 2.

  They scale with progression like Joe (`BabyHealthScale` / `BabyDamageScale`) and drop only a few gold nuggets.
- **Sean** arrives on the far side with **Sean's Security** (3 guards in iron), who fight the babies.
- **Picking a side:**
  - **Baby Bonnet:** Joe drops one into the inventory of everyone who said the phrase. Wear it and the babies and Joe leave you alone, but Sean's Security attacks you.
  - **Merch:** Sean's shirt. Security leaves you alone, but the babies still come for you.
  - **Neither:** everyone attacks you.
- **Curse the next wave** at **Joe's War Cauldron** by right-clicking it holding one of these:
  - **Poisonous Potato:** the babies keep stopping to throw up.
  - **Cobweb:** they're half speed.
  - **Leather:** they're slower and hit 40% softer.

  Curses stack, and everyone near the camp is told who cursed the wave.
- **How it ends:**
  - The last wave dies: the players win.
  - Joe dies: the babies scatter.
  - 15 minutes pass: Joe "declares victory".

  Either way, the camp blocks, babies and guards disappear, the sky clears, and there's a 30-minute cooldown before the next war. Camp blocks can't be broken while the war is on.

## Snow

When it starts snowing where you are, your character says "awe man the sky is cumming!" in chat. It's formatted like a normal chat message from you, and everyone sees it. It happens once per snowfall, at most every 10 minutes. You can turn it off or change the line under `Fun`: `SnowLineEnabled`, `SnowLine`, `SnowLineCooldownMinutes`.

## When Joe meets Sean

When the two get within 35 blocks of each other, they fight for 45 s:

- **Joe** throws fireballs and calls down meteors.
- **Sean** throws ice shards, lightning strikes and hail volleys.

Every spell is an area-of-effect blast, and plenty of them miss and land around the arena, so anyone nearby gets caught in it. Bystanders take 50% damage by default (`Fight.BystanderDamageMultiplier`). Fire sets you alight, frost slows you, and the spells never break blocks.

Neither of them can drop the other below 15% health, so the fight always ends in a stalemate. Joe then storms off. After a 10-minute cooldown they can fight again. Players nearby get a warning on screen when a fight starts.

## Character system

There's no class to pick. Your class is wherever you spend your points on the **passive web**. Three things build a character:

- **Character level** (1–60) comes from its own class XP. Vanilla XP is left alone for enchanting. You earn class XP from:
  - kills (twice the mob's max health)
  - ores (6, or 40 for diamond, emerald and netherite)
  - hitting Joe (5 per hit) and killing him (500)
  - winning a Baby War (2000)

  Each level gives one web point. Ability slots open at levels 1, 10, 25, 40 and 60.
- **Use-skills** (Valheim-style) rank 0–100 as you use them. Each one improves its own action, and together they feed your attributes. When you die you lose 5% of your progress in every skill.

  | Strength | Agility | Intelligence |
  |---|---|---|
  | Swords, Axes: up to +50% damage with that weapon | Bows: up to +50% arrow damage | Enchanting: up to 40% chance to refund the levels |
  | Blocking: reflect up to 50% of blocked damage | Running: up to +15% sprint speed | Brewing: potions you drink last up to 50% longer |
  | Mining: up to +100% breaking speed | Sneaking: faster sneaking | Casting: up to +25% ability damage and faster cooldowns |
  | | Swimming: faster swimming, longer breath | Farming: up to 50% chance of extra crops |

- **Attributes** (Dota-style). Each one is the sum of its four skill ranks ÷ 10, plus 0.25 per level, plus web nodes. Your highest attribute is your primary and adds melee and arrow damage.
  - **Strength:** +0.4 max health and +0.01 health regen per point, and a bigger Rage pool.
  - **Agility:** +0.6% attack speed, +0.25% movement speed and +0.08 armor per point, and a bigger Stamina pool.
  - **Intelligence:** a bigger Mana pool and faster regen, and +1% ability damage per point.

### The passive web

Press **K** to open it. It has 121 nodes:
- the start node in the middle
- six regions around it: Warrior (Str), Duelist (Str/Agi), Ranger (Agi), Shadow (Agi/Int), Mage (Int) and Templar (Int/Str)
- bridge nodes linking each region to the next

Each region has attribute nodes, four notables, four active skills and a keystone with a trade-off. Going straight out to one region gives you the "true" class; spreading out gives hybrids.

To use it: click a connected node to take it. Drag to pan and scroll to zoom. To slot a skill, click a learned active skill and then a slot at the bottom. Right-click a slot to empty it. The left panel shows your level, points, path, attributes and skill ranks.

| Region | Active skills | Keystone |
|---|---|---|
| Warrior (Rage) | Leap Slam, War Cry, Ground Slam, Berserk | Juggernaut: +40% health, +50% knockback resistance, −15% speed |
| Duelist (Stamina/Rage) | Lunge, Riposte, Whirlwind, Blade Flurry | Bladedancer: +30% attack speed, +10% crit, −6 armor |
| Ranger (Stamina) | Dash, Volley, Rain of Arrows, Hunter's Mark | Wind Walker: no fall damage, +15% speed, −20% health |
| Shadow (Stamina/Mana) | Smoke Bomb, Shadow Step, Poison Cloud, Blade Fan | Ghost: invisible while sneaking, +30% backstab, −20% health |
| Mage (Mana) | Fireball, Frost Nova, Blink, Chain Lightning | Mind over Matter: 30% of damage taken comes from mana |
| Templar (Mana/Rage) | Consecrate, Holy Smite, Divine Shield, Judgment | Holy Aura: burn nearby monsters, +0.5 regen |

### Abilities and resources

Learned skills go on their own five-slot bar to the right of the hotbar, cast with **Z, R, G, V, B** (rebindable under Controls → GoblinCraft). Each skill costs its region's resource and has a cooldown. A bar appears above the ability bar for each resource your slotted skills use:
- **Rage:** builds when you hit or get hit, and drains after 5 s out of combat.
- **Mana:** regenerates, faster with Intelligence.
- **Stamina:** its own bar, separate from hunger. Vanilla sprinting doesn't use it.

You get a popup whenever you level up or a skill ranks up.

### Respec: the Altar of Regret

Craft it from 2 gold ingots and a book over 3 crying obsidian, then place it. Using it clears your whole web **and kills you on the spot**; it's a real death, so the skill penalty applies too. How much you get back depends on your level:
- **Levels 1–20:** every point.
- **Levels 21–40:** 75%.
- **Levels 41–60:** 50%, and it also uses up an **Orb of Regret**. Joe drops one 25% of the time; Sean sells them for 32 emerald blocks and 2 diamond blocks.

Points you don't get back are gone for good.

Admin commands for testing: `/character xp <player> <amount>`, `/character skill <player> <skill> <level>`, `/character reset <player>`.

## Raids and hideout

From [TarkoValheim](https://github.com/cavmkii/TarkoValheim). The systems stack with the character system: a raid death costs your kit **and** the 5% skill loss, body-part health is shared out of your attribute-boosted max health, and Brewing improves meds.

### Hideout and raids

Craft a **Hideout Bench** (3 cobblestone over planks, a crafting table and planks) and place it. Everything within 30 blocks of it is your hideout; anywhere else you're **in raid**. The HUD shows which, with a raid clock.

- **Threat** rises the longer a raid lasts: after the calm period (default 5 minutes), one level per step (default 5 minutes), up to 4. Monsters that appear near you can spawn elite (1 or 2 stars: +100% health and +50% damage per star), and hunting parties are sent after you.
- **Dying in raid** follows `Raid.DeathRule`. The default, `LOSE_EVERYTHING`, means no drops: everything outside your secure pouch is gone. `KEEP_SECURE` drops your kit normally and keeps the pouch. `VANILLA` drops everything, the pouch included.
- Dying inside the hideout is a normal Minecraft death.
- Portals are untouched.

**Modules**, built at the bench from what you carry plus the **stash** (every chest and barrel inside the hideout). Each takes real time to build, and only one builds at a time.

| Module | Levels |
|---|---|
| Workshop | furnaces in the hideout smelt 25/50/75% faster; unlocks Beta/Gamma/Kappa pouches and bigger rigs and packs; builds modules faster |
| Medstation | unlocks meds by tier; bleeding stops in the hideout, then fractures mend, then faster regeneration |
| Perimeter | hideout radius +10/+20/+30 |
| Intel center | +3/+6 calm minutes per raid; warns you when a hunting party is sent |

The bench's CRAFT tab makes things only a hideout can: secure pouch upgrades, meds, rigs and backpacks.

### Body-part health

Damage lands on one of seven parts (head, thorax, stomach, two arms, two legs), chosen by the height and side the hit came from (falls hit the legs; damage with no direction, like fire, is spread). Each part holds a fixed share of your max health. Parts can **bleed** (light or heavy, losing health over time), **fracture** (legs slow you, arms weaken your swing), or be **destroyed**. A destroyed head or thorax kills you.

Meds come from the Hideout Bench (Medstation 1–3): bandage, tourniquet, splint, painkillers, field kit and surgical kit. Use one from the hotbar to treat the worst part, or press **H** (or the STATUS tab) to pick the part yourself. The STATUS tab lists meds from everything you carry, grids included.

### Modular weapons and armor

Swords, axes, bows, crossbows, shields and all four armor pieces are built from parts:
- weapons: head (blade, axe head or limbs), handle, guard, wrap;
- armor: plates, padding, wrap.

The vanilla recipes for those items are replaced: craft the parts, then lay them out in a crafting grid to assemble the item. A standard build is exactly the vanilla item. Other parts shift damage, swing speed, durability, block, armor and weight, and the normal tooltip shows the result.

The **Weapon Bench** (smithing table + crafting table, shapeless) swaps parts on an existing item, and levels a part up (to level 4) with its own material.

### Tetris inventory

In survival, **E** (or **J**) opens the Tarkov screen instead of the vanilla inventory. Creative players keep the vanilla one.

- **GEAR:**
  - Armor, off-hand, and slots for a **rig** and a **backpack**.
  - The 9-slot hotbar works as in vanilla.
  - The grids: pockets (5x2), the rig's grid, the backpack's grid, and the **secure pouch** (2x2 Alpha up to 4x3 Kappa).
  - Items take real space: swords are 1x3, chestplates 3x3, shields 2x2, and so on.
  - Pickups fill pockets, then rig, then backpack. Weapons and tools go to an empty hotbar slot first. If nothing has room, the item stays on the ground.
  - Controls:
    - click to pick up, click to put down;
    - **R** rotates the held item;
    - **Shift**+click takes half a stack;
    - **Ctrl**+click transfers;
    - **1–9** sends the hovered item to that hotbar slot;
    - right-click or Esc lets go.
  - SORT repacks every grid, and the DISCARD box drops the item.
  - A rig or backpack has to be empty before you take it off.
- **Chests and barrels** open on the GEAR tab, on the right. Inside your hideout, the whole stash shows there too, and STASH ALL empties your pockets, rig and backpack into it. Sneak+use with an empty hand still opens the vanilla screen.
- **CRAFTING** lists every crafting recipe you can make right now, from your hotbar and grids (and the stash, when you're home). 2x2 recipes work anywhere. 3x3 recipes need a crafting table within 6 blocks, or the hideout. Vanilla crafting tables still work too.
- **SKILLS:** character level, attributes and the 12 use-skills.
- **STATUS:** body health and meds.

Bows, crossbows and the Glock take arrows from the rig, pockets or backpack when the hotbar has none.

New characters start with a Scav vest (4x2), a Sling bag (3x3) and an Alpha pouch. Better gear comes from the Hideout Bench:

| Item | Grid | Needs |
|---|---|---|
| Scav vest (rig) | 4x2 | 6 leather, 4 string |
| Sling bag | 3x3 | 4 leather, 4 string |
| Scav backpack | 4x5 | Workshop 1; 12 leather, 8 string, 2 iron |
| Carrier rig | 5x3 | Workshop 2; 10 leather, 6 string, 6 iron |
| Pilgrim backpack | 5x7 | Workshop 3; 24 leather, 12 string, 4 gold |

The old slot-based secure container was replaced by the pouch grid. What was in it moves into the pouch the first time you log in.

## Install

1. Install [Fabric Loader](https://fabricmc.net/use/) for Minecraft 1.21.1 and put [Fabric API](https://modrinth.com/mod/fabric-api) in `mods/`.
2. Build the jar (below) and put `build/libs/goblincraft-<version>.jar` in `mods/`.
3. The server and every player need the mod.

## Building

Requirements: JDK 21.

```sh
./gradlew build
```

The jar lands in `build/libs/`. `./gradlew runClient` starts a dev client with the mod loaded; `./gradlew runServer` starts a dev server.

## Config

`config/goblincraft/goblincraft.properties` is written with every setting and a comment for each on first run. Only the server's copy matters. Reload with `/goblincraft reload`, which also reloads Joe's lines. Some values (Joe's size, Sean's health) only apply to newly spawned NPCs.

## Commands

- `/goblincraft where`: Joe's and Sean's last known positions, whether they're loaded, whether Joe is enraged, and the war status.
- `/goblincraft summon joe|sean` (op): replaces Joe or Sean with a fresh one in front of you. Handy for testing a fight: summon both.
- `/goblincraft babywars start|stop` (op): start the Baby Wars at your position, or call them off.
- `/goblincraft reload` (op): reload the config and `joe_lines.txt`.

## What changed from the Valheim version

| Valheim | Here | Why |
|---|---|---|
| Fuling model for Joe and the babies | piglin model tinted green; babies use the baby piglin proportions | no goblin mob in Minecraft; no custom textures needed |
| Dvergr mage Sean | wandering trader look, villager trade screen | closest merchant Minecraft has |
| Dvergr guards | human guards in iron armor | no Dvergr |
| coins | emeralds, at about 25 coins per emerald | Minecraft's currency |
| skill levels | XP level | Minecraft has no skills |
| 7 boss global keys | 7 progression advancements, earned by any online player | bosses aren't tracked per world |
| Sean 3000 HP | 1000 HP | Minecraft caps max health at 1024 |
| Restraining Order is a belt | works while it's anywhere in your inventory | no belt slot |
| Soda +35 health / +150 stamina | hunger, saturation and Health Boost | no stamina bar |
| Diddy Oil +100% parry | Resistance I | no parry |
| Glock fires bolts | fires arrows | no bolts |
| Pukeberries / Thistle / Troll hide | Poisonous Potato / Cobweb / Leather | no such items |
| thunderstorm only for the war's players | real thunderstorm for the whole world | Minecraft weather is per world |
| speech shown above heads | speech in chat, within 40 blocks (shouts 100) | no speech bubbles |
| Joe roams the whole map | roams near players, and turns up again if left alone | Minecraft only runs loaded chunks |
| weather charms, bottled weather, weather weapons (off by default) | not ported | they were legacy stock, hidden by default |

Sean's lines about selling weather charms now pitch his actual stock. Joe's lines are unchanged.

## How it works (code map)

| File | Role |
|---|---|
| `GoblinCraft.java` | Entry point. Loads config and lines; hooks server ticks, chat (rage phrases), cauldron use, camp-block protection and commands. |
| `entity/Joe.java` | Joe's behaviour: provocation, rage, restraining order, pickpocketing, taunts, puking, level scaling, stash drop on death. |
| `entity/Sean.java` | Sean: travelling, camping, greeting customers, trades, damage rules. |
| `entity/Fight.java`, `entity/SpellProjectile.java` | The duel. Spells are item-rendered projectiles (lightning is instant) that burst into area damage where they land. |
| `entity/BabyGoblin.java`, `entity/SeanSecurity.java`, `entity/BabyBottle.java` | The war's mobs. They remove themselves when no war is running. |
| `entity/Wander.java` | Long-distance travel in ~24-block legs on vanilla pathfinding, plus loitering around an anchor. |
| `world/WorldDirector.java` | One Joe and one Sean per world, death and respawn timers, bringing stranded NPCs back, rage state. |
| `world/BabyWar.java` | Trigger counting, camp, waves, curses, bonnets, ending. |
| `world/GoblinData.java` | Saved with the world: which Joe and Sean are real, respawn timers, Joe's stash, camp blocks to clean up. |
| `client/*` | Renderers, item tint for the dyed leather items, tooltips. |
| `rpg/PassiveWeb.java`, `rpg/WebNode.java` | The web: a 19-node template repeated per region, plus bridges. Built identically on both sides, so only node ids go over the network. |
| `rpg/PlayerData.java`, `rpg/Stats.java` | The saved character (a Fabric attachment kept through death) and everything derived from it. |
| `rpg/Rpg.java` | Server side: xp, death penalty, attribute modifiers, resources, casting, slots, respec, `/character`. |
| `rpg/RpgDamage.java`, `mixin/*` | Damage rules (weapon skills, crits, lifesteal, dodge, parry, blocking) and the hooks for enchanting, brewing and potion duration. |
| `rpg/SkillEffects.java` | What the 24 active skills do. |
| `client/rpg/*` | Web screen, ability bar and resource bars, popups, keys, altar confirmation. |
| `tarkov/Tarkov.java`, `tarkov/Hideouts.java`, `tarkov/Module.java`, `tarkov/Stash.java` | Raid tracking, threat, elites and hunting parties, raid death rules; hideouts (saved per dimension), modules, the stash. |
| `tarkov/Health.java`, `tarkov/Body.java`, `tarkov/Med.java` | Body-part health: hit location, bleeds, fractures, limb effects, meds. |
| `parts/*` | Part catalogue, the installed-parts data component, stat rewriting, part assembly recipe, Weapon Bench. |
| `tetris/Tetris.java` | Tetris inventory, server side: the inbox sweep, pickups, moves, containers and stash, crafting, death. |
| `tetris/Grid.java`, `tetris/ItemSize.java`, `tetris/InvData.java` | Grid placement, item footprints, the saved inventory (a Fabric attachment). |
| `mixin/InventoryMixin.java`, `mixin/PlayerProjectileMixin.java` | Pickups into the grids; bows finding arrows in the grids. |
| `client/tarkov/*` | Raid HUD, Hideout screen, the Tarkov screen (GEAR, CRAFTING, SKILLS, STATUS), E-key handling. |

Item icons reuse vanilla textures (honey bottle, crossbow, paper and so on) through the item model files, so the mod ships no textures of its own.

## Verification status

It builds cleanly (no compiler warnings) against Minecraft 1.21.1, Loom 1.11.8, Fabric Loader 0.16.9 and Fabric API 0.116.17+1.21.1. It's been run on a dev server and a dev client under a software-rendered virtual display. What was seen working:

- **Uniqueness:** a second summoned Joe removes itself. A baby summoned with no war on removes itself.
- **Death and respawn:** killing Joe starts the 10-minute respawn timer, and a Joe summoned during the timer removes itself. The state is saved to `data/goblincraft.dat`.
- **The duel:** Joe and Sean start fighting when close. Fireballs and lightning render, damage lands, and it ends in a stalemate with both end-of-fight lines.
- **Rendering:** Joe as a green piglin, the toddler babies, Sean in trader robes, the guards, and the war camp (cauldron on campfire, totem, banner).
- **Speech:** Joe's taunts and puke lines, and Sean's greeting and pitch, show in chat.
- **The Baby Wars:** the title, the thunderstorm, the camp, the bonnet handed out, waves 1 to 4, and cursing a wave at the cauldron ("cursed Joe's next wave: Slow!"). A full war ran to "The last baby has fallen" and the camp was removed.
- **The snow line** fired when the war's storm started snowing on the player.
- **Sean's trade screen** opens with all six offers.

- **Raids and hideout:** raid HUD and threat, elite strays and hunting parties, a raid death wiping the kit and keeping the pouch, the Workshop building from stash chests.
- **Body health:** the STATUS screen, bleeds, field kit healing.
- **Parts:** assembling a sword in a crafting grid, swapping and levelling parts at the Weapon Bench, the armor bench (diamond chestplate with netherite plates: +6% armor), tooltips showing the new stats.
- **Tetris inventory:**
  - E opens the screen; pickups fill pockets, then the backpack.
  - The old secure container's contents moved into the pouch.
  - Equipping a chestplate from the backpack (the vanilla "Suit Up" advancement fires).
  - Rotating a sword into the rig as 3x1.
  - STASH ALL into a hideout chest, and Ctrl+click back out.
  - A barrel outside the hideout opening on GEAR and looting from it.
  - Crafting planks from logs in the stash, with search.
  - A bow firing arrows from the rig (39 → 37).
  - A raid death leaving only the pouch, with the stash intact.

Not exercised yet: pickpocketing and the stash drop, the Restraining Order, rage from chat, the Glock, Soda and Diddy Oil, and the stranded-NPC relocation. These need a real player wandering around for a while. Treat a first real session as a test of those.

On start-up the log shows `No data fixer registered for goblincraft:...` once per entity. That's Minecraft noting that modded entities have no data-fixer schema. It's harmless and common to Fabric mods.
