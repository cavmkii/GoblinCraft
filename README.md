# GoblinCraft

A Fabric mod for Minecraft Java 1.21.1 that adds two unique, persistent NPCs to a world. It's a port of [ValheimGoblinPack](https://github.com/cavmkii/ValheimGoblinPack); where Valheim and Minecraft differ, the closest Minecraft equivalent is used. Those substitutions are listed [below](#what-changed-from-the-valheim-version).

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

Not exercised yet: pickpocketing and the stash drop, the Restraining Order, rage from chat, the Glock, Soda and Diddy Oil, and the stranded-NPC relocation. These need a real player wandering around for a while. Treat a first real session as a test of those.

On start-up the log shows `No data fixer registered for goblincraft:...` once per entity. That's Minecraft noting that modded entities have no data-fixer schema. It's harmless and common to Fabric mods.
