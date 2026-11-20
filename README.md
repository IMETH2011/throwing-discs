# Throwing Discs – Fabric mod for Minecraft Java 1.21.11

Copper, Iron, Golden, Diamond and Netherite throwing discs.

## Build the .jar (3 steps)
1. Install **JDK 21**.
2. Open a terminal in this folder and run:
   - Windows: `gradlew.bat build`   |   Mac/Linux: `./gradlew build`
   (If there is no `gradlew` file, run `gradle wrapper --gradle-version 9.2.1` once, or use the
   template route below.)
3. Your mod is `build/libs/throwing-discs-1.0.0.jar` (not the `-sources` one).
   Put it in `.minecraft/mods` together with **Fabric API for 1.21.11**.

### Template route (most reliable)
Generate a blank 1.21.11 project at https://fabricmc.net/develop/template (Yarn mappings),
then copy this project's `src/` folder into it (replace its `src/`) and run `./gradlew build`.
If the template uses another package/mod id, keep `fabric.mod.json` from this project.

## Gameplay
- Right-click to throw. Flies flat & spinning, no gravity, pierces mobs/players.
- Ricochets off blocks (faster + flies longer). 3+ ricochets = +2.5 damage.
- Comes back like a boomerang (hurts on the way back). Offhand throws return to the offhand.
- Smashes decorated pots.
- Damage = same as the sword of that tier: copper 5, iron 6, gold 4, diamond 7, netherite 8.
- Enchants: Sharpness I-V, Knockback I-II, Fire Aspect, Unbreaking I-III, Mending.
- Crafting: 4 ingots in a plus shape (empty centre). Netherite: smithing table
  (netherite upgrade template + diamond disc + netherite ingot).
- Sounds: copper/iron/gold/diamond use their metal sounds, netherite uses nether-brick sounds.
- All tuning numbers are constants at the top of `ThrownDiscEntity.java`.
