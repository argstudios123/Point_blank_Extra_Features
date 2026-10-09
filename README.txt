Point Blank Extra Features
==========================

An addon for Vic's Point Blank (Forge 1.20.1) that adds extra weapon features
without modifying or replacing Point Blank itself.

Requirements: Minecraft 1.20.1, Forge 47.4.23, Point Blank 2.2.0, GeckoLib 4.8.4.
This JAR does NOT bundle Point Blank or GeckoLib; install all three mods together.

Features
--------
1. Per-weapon Toggle Aiming
2. Per-weapon Melee Attack (configurable damage/cooldown/reach/animations)
3. Global Water/Lava Impact Particles (config toggle)

Toggle Aiming
-------------
Normally Point Blank aims while RMB is held. A weapon opts into toggle aiming:

    "toggle_aiming": "enabled"

RMB click #1 enters aim, release stays aimed, RMB click #2 exits aim. Missing or
"disabled" keeps normal hold-to-aim. Switching/dropping the weapon resets the
toggle so the player is never stuck aiming.

The toggle latches the right-mouse input value that Point Blank's own
ClientEventHandler.onClientTick reads. Point Blank still performs the actual
aiming transition, state, animations, GunClientState and AimingChangeRequestPacket.
Only weapons with "toggle_aiming": "enabled" are affected.

Melee Attack
------------
    "melee": {
        "enabled": true,
        "damage": 10.0,
        "cooldown": 20,
        "reach": 3.0,
        "animations": [
            "animation.model.melee_1",
            "animation.model.melee_2",
            "animation.model.melee_3"
        ]
    }

enabled (bool) - master switch; missing -> disabled.
damage  (num)  - damage dealt (server-authoritative).
cooldown(int)  - ticks (20 ticks = 1 second).
reach   (num)  - max target distance in blocks.
animations (array) - GeckoLib animation names, chosen randomly. Any length works
(0, 1, 3, 20, ...). Empty -> no animation but the attack still applies.

Melee key: "Melee Attack" under "Point Blank Extra Features" in Controls.
Default V, fully rebindable; gameplay logic never hard-codes V. One global key;
the held weapon's JSON decides whether it responds. Non-melee weapons ignore it.

Networking (anti-cheat):
  CLIENT press key -> check held weapon + local cooldown -> MeleeRequestPacket
  SERVER re-check gun/melee/cooldown -> server-side ray cast within reach ->
         apply damage from weapon config -> random animation -> MeleeResponsePacket
  CLIENT play the chosen GeckoLib animation on the held weapon
Damage is authoritative on the server; the client never supplies damage/target.

Water/Lava Impact Particles
---------------------------
    enableWaterLavaImpactParticles = true   (config/pointblankextra-client.toml)

Global toggle, enabled by default and independent of any weapon JSON. When a
Point Blank weapon hits a block that is in water or in lava, extra vanilla
particles are spawned at the hit position:
    water -> splash (+ a few bubble) particles
    lava  -> lava (+ smoke) particles
Works for hitscan weapons and for both Point Blank projectile types. When false
no extra Water or Lava particles are spawned at all.

Point Blank's standard block-hit behavior is never changed by this feature: the
addon listens to Point Blank's own BlockHitEvent (posted by
HurtingItem.handleBlockHit for every hitscan and projectile block hit), never
cancels it and adds only particles. Damage, explosions, Point Blank's own smoke
and block destruction are untouched, and hits on dry blocks are unaffected.

Point Blank casts shots with fluids ignored, so water and lava are not colliders:
a shot entering water/lava passes through it and hits the solid block behind or
below. The impact position and the fluid block on the hit face are both checked,
so shots into a pond, a waterfall or lava are recognized.

Weapon JSON placement
---------------------
Point Blank reads weapons from:
  1. Built-in: data/pointblank/items/<name>.json inside the Point Blank JAR.
  2. Extensions: <gamedir>/pointblank/<pack>/items/<name>.json (folder or zip).

This addon reads those same JSON files and extracts only "toggle_aiming" and
"melee". It never writes to them and never modifies Point Blank's parsing. No
separate per-weapon config file is needed.

See examples/example_weapon.json and examples/example_weapon_disabled.json.
Copy them into an extension's items/ folder and rename the "name" field.

IMPORTANT: animation names must exactly match names in the weapon model's
GeckoLib .animation.json file. Point Blank conventionally prefixes with
"animation.model." (e.g. "animation.model.fire").

Building
--------
Java 17 required. Run:  ./gradlew build
Output: build/libs/pointblankextra-<version>.jar

Integration (without replacing Point Blank)
-------------------------------------------
- JSON:       indexes original weapon JSON; Point Blank's parser is untouched.
- Toggle aim: Mixin into ClientEventHandler.onClientTick latches RMB input;
              Point Blank's own aiming system does the real work.
- Melee anim: Mixin into GunItem.registerControllers adds one dedicated,
              non-interfering GeckoLib controller ("pbextra_melee") that stays
              stopped unless a melee animation is triggered.
- Networking: separate Forge SimpleChannel; Point Blank's channel is untouched.
- Particles:  Forge listener on Point Blank's BlockHitEvent, never cancelled;
              Point Blank's block-hit logic still runs unchanged.

Config: config/pointblankextra-client.toml  ->  enableMod (master switch),
        enableWaterLavaImpactParticles (Water/Lava impact particle toggle).

