Point Blank Extra Features
==========================

An addon for Vic's Point Blank (NeoForge 26.1.2) that adds extra weapon features
without modifying or replacing Point Blank itself.

Requirements: Minecraft 26.1.2, NeoForge 26.1.2.7-beta, Vic's Point Blank 2.2.0
(CurseForge file 8938498), GeckoLib 5.5.2 (CurseForge file 8327201).
This JAR does NOT bundle Point Blank or GeckoLib; install all three mods together.

Features
--------
1. Per-weapon Toggle Aiming
2. Per-weapon Melee Attack (configurable damage/cooldown/reach/animations)
3. Per-weapon Custom Crosshair (native 39x39 atlas or static complete-PNG fallback)
4. Global Water/Lava Impact Particles (config toggle)

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
        "knockback": 4.0,
        "angleRange": 40,
        "meleeAnimations": [
            {
                "name": "animation.model.melee_1",
                "duration": 700,
                "preMelee": 200
            }
        ]
    }

enabled (bool)    - master switch; missing -> disabled.
damage  (num)     - damage dealt (server-authoritative).
cooldown(int)     - ticks (20 ticks = 1 second).
reach   (num)     - max target distance in blocks, measured from the eyes to the
                    nearest point of the target's bounding box.
knockback (num)   - knockback strength in tenths of the vanilla melee impulse:
                    4.0 = 0.4 blocks/tick (the historical fixed force),
                    3.0 = 0.3. Missing -> 4.0, so old JSONs keep their old force.
angleRange (num)  - FULL horizontal width of the frontal attack cone in degrees.
                    40 means 20 degrees to either side of the player's facing
                    direction. Missing -> 40. 360 hits every direction.
meleeAnimations (array) - animation objects; each has a GeckoLib name, a
                    duration in milliseconds and a preMelee time in milliseconds.
                    Missing/empty -> the attack still applies, with no animation.
The legacy flat "animations": ["name", ...] array is still accepted.

Every swing can damage EVERY eligible target inside the cone, at most once per
target. Targets behind the player or outside angleRange are ignored. Melee is
server-authoritative: the client reports the held key and the server validates
the weapon, player state, cooldown, reach, angle, targets and timing. The damage
is scheduled from the animation's preMelee time in server ticks (rounded up), so
a broken or unrendered client animation cannot lose the hit.

Melee key: "Melee Attack" under "Point Blank Extra Features" in Controls.
Default V, fully rebindable; gameplay logic never hard-codes V. Holding V keeps
attacking whenever the player is eligible and the cooldown has expired; releasing
V stops new attacks (an accepted swing still finishes at its preMelee time).
Melee is blocked while sprinting, aiming, firing, reloading, inspecting,
drawing/equipping, holstering/hiding, changing fire mode or overheating.

Networking (anti-cheat, NeoForge payloads):
  CLIENT press/hold key -> check held weapon + local cooldown -> MeleeRequestPacket
  SERVER re-check gun/melee/cooldown/player state -> pick animation ->
         schedule impact at preMelee (server ticks) -> MeleeResponsePacket
  CLIENT play the chosen GeckoLib animation on the held weapon
  SERVER at the impact tick -> re-check held weapon/config -> select every
         eligible target in the reach + angle cone -> damage + knockback
Damage, targets, reach, angle and timing are authoritative on the server; the
client never supplies a target list or damage.

Reusable First-Person Animation Libraries
------------------------------------------
Point Blank's native "firstPersonFallbackAnimations" string keeps working exactly
as before:

    "firstPersonFallbackAnimations": "walking_animations"

This addon additionally accepts an ARRAY of reusable libraries under the same
key. Each entry names a GeckoLib animation resource without the
".animation.json" extension. Two resource layouts are supported:

  1. Traditional:
       assets/pointblank/animations/item/<entry>.animation.json
       (legacy built-in equivalent:
        assets/pointblank/geckolib/animations/item/<entry>.animation.json)

  2. Reusable library directory:
       assets/pointblank/default_animations/<entry-without-the-optional-default_animations-prefix>.animation.json

The custom directory may be nested (for example
default_animations/pistol_anims/aw_pistol_dualwield). It is loaded into
GeckoLib's baked animation cache at resource reload, so the definitions are
genuinely available to Point Blank's animation controllers; it is not just a
path rewrite.

Example:

    assets/pointblank/
    +-- animations/item/
    |   +-- existing_weapon_animations.animation.json
    +-- default_animations/
        +-- walking_animations.animation.json
        +-- pistol_anims/
            +-- aw_pistol_dualwield.animation.json
            +-- aw_pistol_dualwield_running.animation.json

    "firstPersonFallbackAnimations": [
        "walking_animations",
        "default_animations/pistol_anims/aw_pistol_dualwield_running",
        "default_animations/pistol_anims/aw_pistol_dualwield"
    ]

For each entry the resolver checks the traditional location first, then the
default_animations location:

    walking_animations
      -> assets/pointblank/animations/item/walking_animations.animation.json
      -> assets/pointblank/default_animations/walking_animations.animation.json

    default_animations/pistol_anims/aw_pistol_dualwield
      -> assets/pointblank/animations/item/default_animations/pistol_anims/aw_pistol_dualwield.animation.json
      -> assets/pointblank/default_animations/pistol_anims/aw_pistol_dualwield.animation.json

An entry that already starts with "default_animations/" is not prefixed twice.
Traditional resources keep priority when both locations contain the same
library, which preserves existing content packs. Do NOT include
"animations/item/" or ".animation.json" in the JSON value.

Each library file is a normal GeckoLib animation JSON (format_version +
animations), so no new file format is introduced. One file may contain any
number of animation names, including walking, idle and dual-wield variants.
The custom directory is read only during GeckoLib's resource reload; missing or
invalid files are reported by GeckoLib, and valid definitions replace the
previous cache on F3+T. Nothing is scanned or parsed per tick.

Resolution order for every animation name:
  1. the weapon's own animation file (always wins),
  2. the array libraries in the order listed, trying the traditional root then
     default_animations for each entry,
  3. Point Blank's AnimationType defaults (pointblank:pistol, pointblank:common
     for PISTOL; pointblank:common for RIFLE).

Relative array paths use the "pointblank" namespace, matching Point Blank's
native string form. An array entry may also be written as "namespace:path" to
read animations from another namespace, provided that namespace's assets are
available through the normal resource manager. Point Blank extension folders
and ZIPs are injected under the pointblank namespace, so content packs should
normally use relative paths. The native string form cannot use a custom
namespace because Point Blank hardcodes "pointblank" for it.

Dual-wield is supported without changing Point Blank's naming rules: Point Blank
resolves _dwleft for the offhand and _dwright for the main hand when
DualWieldFeature is enabled, and checks the same weapon + library fallback
chain. Put _dwleft/_dwright variants in the reusable library files and both
hands resolve correctly.

Important precedence detail: Point Blank's internal animation-cache fallback
tries the exact suffixed name and then immediately tries the unsuffixed base
name in the same resource. Without the addon's exact-variant pass, a primary
file containing "animation.model.idle" would shadow a later library containing
"animation.model.idle_dwright". The addon now searches the complete primary +
fallback chain for the exact _dwleft/_dwright definition before Point Blank's
base-name fallback runs. If no exact suffixed variant exists anywhere, Point
Blank's existing fallback to the unsuffixed name is preserved (for example a
missing "walking_dwright" uses "walking"). Provide suffixed variants for every
state where the custom pose matters.

Reload-time diagnostics:
  - Missing array libraries are reported once per resource reload, naming the
    weapon and the GeckoLib cache keys that were checked:
        pointblank:item/<entry>
        pointblank:default_animations/<entry-without-the-optional-default_animations-prefix>
  - Native string fallbacks are not validated by the addon; Point Blank owns
    them.
  - Invalid identifiers and non-string array entries are reported as warnings.
  - At debug level, "[dual-wield exact]" reports the resource and animation
    definition chosen for an exact _dwleft/_dwright name. "no exact definition"
    means the custom libraries do not contain that name and Point Blank's
    base-name fallback is used.
  - Nothing is scanned or parsed per tick. Parsed animations are cached by
    GeckoLib; the addon's library index is rebuilt only on resource reload.


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

Custom Crosshair
----------------
Every Point Blank gun can replace Vic's Point Blank's native crosshair artwork
with a content-pack PNG. A 39x39 native-layout atlas is passed through VPB's
own renderer, so VPB keeps controlling expansion, smoothing, positioning and
visibility. Arbitrary complete PNGs remain supported through a simple static
fallback.

    {
      "type": "custom_crosshair",
      "enabled": true,
      "texture": "textures/item/custom_crosshairs/halo_reticle.png",
      "size": 16
    }

Place this object in the weapon's "features" array.

enabled (bool) - defaults to true when the feature is present. false leaves the
                 normal Point Blank/vanilla crosshair in place.
texture (str)  - texture path. A path without a namespace is resolved in Point
                 Blank's "pointblank" namespace, exactly like Point Blank's
                 Reticle feature. Point Blank extension packs expose that
                 namespace, so use "textures/..." or "pointblank:textures/...".
                 The "textures/" prefix and ".png" suffix are optional. Other
                 namespaces are only resolvable when an ordinary Minecraft
                 resource pack provides them; a Point Blank extension pack does
                 not expose them.
size (num)     - default 16, clamped to 1-512. Used only by the static
                 complete-image fallback; native Point Blank atlases keep VPB's
                 native geometry and ignore it.
layout (str)   - optional. "complete" (default) keeps the existing arbitrary-PNG
                 static fallback. "pointblank" (aliases "native", "segmented",
                 "atlas") supplies a 39x39 native atlas to VPB's own renderer.
                 This single setting is required because a complete PNG cannot
                 be a drop-in replacement for VPB's segmented native atlas.

Native VPB behavior (verified in the Point Blank 2.2.0 source):
- The native crosshair is drawn only in first person, only when Point Blank's
  global crosshair is DEFAULT, only while not aiming, and only while firing or
  idle. Aiming, reloading, drawing/holstering, inspecting, changing fire mode
  and similar states use VPB's own behavior (often a scope overlay or no
  crosshair).
- `getCrosshairExpansionRatio` starts at 1.0, adds 8.0 per block/tick of
  position delta, multiplies by 3.0 while sprinting, 2.4 while airborne, 0.8
  while visually crawling and 0.7 while crouching, adds 0.1 per degree of
  look-rotation delta, multiplies by 4.0 while
  `getTotalUninterruptedShots() > 0`, smooths over 100 ms and clamps to
  1.0-7.0. It does not derive spread from a per-weapon accuracy stat.
- `CrosshairRenderer.renderCrosshairOverlay3` uses a single 39x39 atlas: a fixed
  central patch plus left, right and bottom segments. Those are the only
  segments the native renderer draws; PBE adds the missing top segment using
  the same native expansion value and mirrors the native bottom geometry.
- For DEFAULT and DISABLED, VPB cancels the vanilla crosshair; for VANILLA it
  leaves the vanilla crosshair in place. Scopes and the attachment screen are
  handled by the same native code.

Custom artwork is injected by redirecting only VPB's native crosshair draw call.
For native-layout atlases, VPB's native expansion value is passed straight back
into its own renderer. Complete PNGs use a static draw inside the same native
branch. Native visibility checks, the global crosshair setting, scope handling,
firing state and native smoothing therefore remain in charge. The feature is
purely client-side: it adds no packets and no server-side changes. Different
weapons may use different textures.

Layouts
-------
"layout": "complete" (default; backwards compatible)
    The texture is an arbitrary complete PNG. PBE draws it as a simple static
    image centred inside VPB's native crosshair branch. Visibility, first-person
    state, aiming/scopes and firing/idle state still come from VPB, but the
    image does not expand or contract. A single undivided image cannot reproduce
    VPB's segmented atlas movement. `size` is the largest rendered dimension.

"layout": "pointblank" (aliases: "native", "segmented", "atlas")
    The texture is a 39x39 native crosshair atlas and is passed directly to
    VPB's own `CrosshairRenderer.renderCrosshairOverlay3`, giving exact native
    geometry and movement. The atlas regions are:
      left 41% x full height   -> left segment
      right 41% x full height  -> right segment
      full width x top 41%     -> top segment (PBE extension; VPB omits it)
      full width x bottom 41%  -> bottom segment
      central 18% x 18%        -> fixed center patch
    The texture must be exactly 39x39; otherwise PBE logs one warning and leaves
    VPB's ordinary native crosshair in place. `size` is ignored for this layout:
    VPB's native geometry and scale are kept exactly.
    An editable template is bundled at:
      assets/pointblankextra/textures/custom_crosshair/custom_crosshair_native.png
    Copy it to your pack, edit it, and set "layout": "pointblank".

Resource layout (directory pack):

    <gamedir>/pointblank/<pack>/
    +-- assets/pointblank/items/<weapon>.json        (contains the feature)
    +-- assets/pointblank/textures/item/custom_crosshairs/<texture>.png

All content-pack crosshair textures belong under assets/pointblank/...;
different weapons and packs can use different paths inside that namespace.
Because every Point Blank extension pack shares that namespace, prefer a unique
subdirectory per pack if several packs may define a file with the same name.
No global replacement of Point Blank's texture is needed.

Behaviour and fallbacks:
- Custom artwork is only substituted inside VPB's existing native crosshair
  branch. It is not an always-visible HUD stamp: aiming, reloading, holstering,
  inspecting and other native states keep VPB's normal behavior.
- With "layout": "pointblank", the custom 39x39 atlas is passed directly to
  `CrosshairRenderer.renderCrosshairOverlay3`, so VPB's expansion, smoothing,
  positioning and left/right/bottom/center segment movement are used unchanged.
  PBE then draws the missing top segment with the same expansion value.
- With the default "layout": "complete", the image is static. It still uses
  VPB's native visibility/state checks but deliberately does not simulate spread.
- VPB's own crosshair cancellation still suppresses the vanilla crosshair for
  DEFAULT/DISABLED; VANILLA remains vanilla. Ammo, hotbar and other HUD layers
  are untouched.
- Scoped aiming is left to Point Blank's scope overlay.
- A disabled feature, missing/invalid texture, malformed PNG, wrong native
  atlas dimensions or a texture that fails to load leaves VPB's ordinary native
  crosshair in place. Missing textures are logged once, never once per frame.
- Resolved ids and decoded PNG dimensions are cached; no texture lookup or
  JSON parsing happens per frame.

Placeholder PNG and content-pack workflow:
- The mod bundles an editable 64x64 transparent complete-layout placeholder at
  assets/pointblankextra/textures/custom_crosshair/custom_crosshair.png.
- During early client startup (before the initial resource reload), and before
  an in-game content reload, Point Blank Extra scans directory packs for
  custom_crosshair features whose texture path ends in custom_crosshair.png.
  If that file does not exist, the bundled placeholder is copied to the exact
  referenced resource path. For the example above and a pack named
  "Argos Weaponry", the generated file is:
    <gamedir>/pointblank/Argos Weaponry/assets/pointblank/textures/item/custom_crosshairs/custom_crosshair.png
  Placeholder generation only targets the pointblank namespace, because that is
  the namespace Point Blank extension packs expose.
- The exact-native-layout template is bundled at
  assets/pointblankextra/textures/custom_crosshair/custom_crosshair_native.png
  (39x39). Copy it into your pack manually when using
  "layout": "pointblank"; it is not auto-installed because it is an optional
  advanced format. Automatic complete-layout placeholder generation skips
  "layout": "pointblank" features so a 64x64 image is never written where a
  39x39 native atlas is required.
- Existing files are never overwritten; a creator's edited placeholder is left
  exactly as it is. Unrelated packs and assets are not touched, and ZIP/JAR
  packs are not modified. If the game cannot write to a pack directory, the
  failure is logged and the feature simply falls back to the normal crosshair.
  If a pack is added while the game is running, run the addon's content reload
  (or restart) before the generated file is visible.
- To use your own design, edit or replace the generated PNG (or ship a texture
  at the same path). Complete-layout PNGs may have any dimensions; larger
  images are scaled to the configured size while keeping their aspect ratio.

PSD templates (editable sources, shipped in the JAR):
- assets/pointblankextra/templates/custom_crosshair/custom_crosshair_native_39.psd
  Exact 39x39 native atlas with separate Top/Bottom/Left/Right/Center layers
  and a hidden Guides layer. Export a 39x39 PNG for "layout": "pointblank".
- assets/pointblankextra/templates/custom_crosshair/custom_crosshair_native_workbook_312.psd
  8x native workbook. Edit, hide Guides, resize/export to exactly 39x39.
- assets/pointblankextra/templates/custom_crosshair/custom_crosshair_complete_512.psd
  Workbook for the static complete-image layout. Export any PNG size.
- PSD files are for editing only; Minecraft loads the exported PNG.
- Keep the Guides layer hidden when exporting. Keep all non-segment areas
  transparent, especially the corners.

Limitations:
- Point Blank extension packs expose only the pointblank namespace, so content
  pack textures must live under assets/pointblank.
- An arbitrary complete PNG cannot reproduce VPB's exact native atlas geometry.
  Complete-layout textures are therefore drawn statically. Use the 39x39
  "pointblank" layout for exact native geometry and native dynamic movement.
- Point Blank 2.2.0's native renderer omits the top atlas region. PBE draws a
  mirrored top segment using VPB's native expansion value and native bottom-arm
  geometry. If a future Point Blank update draws the top segment itself, the
  PBE top extension must be removed or adjusted to avoid duplication.
- `dynamic` keys from older experiments are ignored. PBE does not calculate
  movement, firing, jumping or recovery spread; VPB owns those calculations,
  and only native-layout textures go through them.
- ZIP/JAR packs are never modified automatically; add the PNG to them manually.
- Scoped aiming uses Point Blank's scope overlay; the custom crosshair is not
  drawn over it.
- Point Blank's global "crosshair: disabled" setting takes precedence.
- Exact native behavior is tied to the verified Point Blank 2.2.0 / NeoForge
  26.1.2 crosshair call. A Point Blank update requires re-checking the mixin
  target descriptor.


Bullet Holes
------------
    enableBulletHoles = true   (config/pointblankextra-client.toml)

Master switch, enabled by default. When false, no new bullet holes are created,
existing ones are removed, and nothing is rendered. Weapon JSON and content-pack
settings can never override this switch. Re-enabling it makes new bullet holes
appear normally.

Every Point Blank gun gets bullet holes without editing its JSON. A gun without a
"bulletHoles" entry inherits its content pack's defaults, then the mod's built-in
defaults (enabled, 0.12 size, 1200-tick lifetime, 256 per chunk, block debris
particles, impact sounds with the mod's own default impact sound and a
16-block audible limit). Every built-in default is defined in exactly one place:
src/main/java/com/arg/pbextra/bullethole/BulletHoleDefaults.java. Configuration
layers are:
  1. built-in defaults,
  2. <pack>/bullet_holes.json (also accepted at assets/pointblank/bullet_holes.json),
  3. the gun's optional feature entry.
Texture maps are merged entry by entry; a gun overrides only the entries it
lists. If a pack disables bullet holes, guns inherit that disabled state unless
they explicitly set "enabled": true.

Pack-wide bullet_holes.json:
    {
      "enabled": true,
      "default_texture": "mypack:textures/bullet_holes/stone.png",
      "size": 0.12,
      "lifetime": 1200,
      "max_per_chunk": 256,
      "max_block_particles": 22,
      "surface_textures": { "minecraft:oak_planks": "mypack:textures/bullet_holes/wood.png" },
      "tag_textures": { "minecraft:logs": "mypack:textures/bullet_holes/wood.png" }
    }

Per-weapon entry inside the normal "features" array:
    { "type": "bulletHoles",
      "bullet_holes": { "enabled": true, "size": 0.18,
                        "surface_textures": { "minecraft:stone": "mypack:textures/bullet_holes/stone.png" } } }
A root-level "bulletHoles": { ... } object is also accepted. A missing entry does
not disable bullet holes.

Texture selection: exact block override, then the first matching tag (tags are
checked in lexicographic identifier order, so the result is deterministic), then
the effective default texture. Textures may be written as
"namespace:textures/bullet_holes/x.png", "namespace:bullet_holes/x" or
"bullet_holes/x"; invalid or missing textures fall back to the built-in default
and are only warned about once. Built-in textures: default, wood, stone, metal,
glass, dirt and sand under
assets/pointblankextra/textures/bullet_holes/.

Built-in material mappings are part of the effective defaults, so every gun gets
them without editing weapon JSON:
  minecraft:oak_planks                -> wood.png
  c:glass_blocks, c:glass_panes       -> glass.png
  minecraft:dirt                      -> dirt.png
  minecraft:sand                      -> sand.png
  minecraft:logs, minecraft:planks,
  minecraft:bamboo_blocks             -> wood.png
  stone tags and the built-in stone
  surfaces                            -> stone.png
  metal block/bar surfaces            -> metal.png
  unmatched blocks                    -> default.png
The glass, dirt and sand mappings are tag-based so stained glass, glass panes,
rooted/coarse dirt and red/suspicious sand follow their parent tag. A pack or gun
can override any of them with an exact "surface_textures" entry or a
"tag_textures" entry; exact wins over tag, and explicit entries win over the
built-in mapping. The deterministic tag rule still applies when several tags
match. Glass and other transparent blocks are never rejected just for being
transparent; only air and fluids are excluded. Note that Point Blank's
"bulletsBreakGlassEnabled" setting (default true) makes hitscan shots destroy
glass instead of stopping on it; glass that survives a hit (for example with
that setting disabled, or from a projectile that does not break it) receives
normal bullet holes.

The server reuses Point Blank's own BlockHitEvent for hitscan and projectile
block hits and broadcasts the exact hit result to players within 96 blocks. Each
client stores lightweight decal records in chunk buckets: no entity, block
entity, particle or per-decal tick. Decals are frustum/range culled, expire after
their lifetime (fading near the end), and are bounded by max_per_chunk plus a
4096 global cap. A lightweight block-position index removes decals immediately
when their supporting block changes; a client ClientLevel hook covers server
block updates, section batches, local prediction and Point Blank/vanilla
explosion block changes, while a neighbour-notify listener remains as a
secondary hook. Only the changed position is inspected, and neighbouring decals
are preserved. Each decal stores the exact local shape box it was placed on,
so a slab/stairs state change that moves the supporting face invalidates it even
when the block id is unchanged. Chunk load/unload events drop only that chunk's
decals. The once-per-second sweep remains a fallback for lifetime expiry and
block-validity checks.
Rendering uses the level submit pipeline with a translucent, lightmapped decal
render type. The decal pipeline keeps the depth test but does not write depth:
overlapping translucent decals are coplanar, so writing their depth made them
fight for the same depth value and flicker. The small outward surface offset is
unchanged, so decals do not float farther above the block. This applies to all
six faces and to partial shapes through the stored support box.

Face placement is edge-aware. An impact that has enough room keeps the
configured size exactly; very large configured sizes still obey the existing
face-size cap. Near an edge or corner the decal shrinks just enough for its
random rotation to fit the hit face, with a 50% minimum so edge hits remain
visible. If even that minimum cannot be centred, the centre is nudged inward by
the smallest amount that fits. The quad never crosses the supporting face, is
never placed on a neighbouring block and keeps the same outward offset and face
normal. Only air, fluids, empty shapes and faces too small for a legible decal
are rejected; there is no edge-distance rejection threshold.

Bullet impact particles
-----------------------
Separate from the persistent decal, every impact can spawn block debris. It is
controlled per gun/pack by "max_block_particles" (Java default 22, clamped to
0-128). 0 disables debris for that configuration, small values spawn a few
shards and larger values spawn heavier debris. The setting is merged exactly
like the other bullet-hole properties, so packs without it inherit the Java
default.

Debris is spawned on the client from the actual BlockState at the impact
position through Minecraft's ParticleTypes.BLOCK / BlockParticleOption provider.
Vanilla and modded blocks therefore use their own particle texture (including
model-based and position-dependent sprites) and no block list is hardcoded.
States that do not support terrain particles simply spawn none. The existing
bullet-hole impact packet already carries the hit position, so no extra packet
is sent. Debris is independent of the decal master switch (enableBulletHoles);
the per-gun "enabled" flag and the global "enableMod" switch gate it.

Impact sounds
-------------
Every gun also gets material impact sounds by default. The sound block lives
inside the same bullet-hole settings and uses the exact key
"soundsPlayOnperBlockHit":

    {
      "soundsPlayOnperBlockHit": {
        "enabled": true,
        "default": "pointblankextra:bullet_holes.default",
        "impact_volume": 1.5,
        "max_distance": 16,
        "surface_sounds": {
          "minecraft:oak_planks": "minecraft:block.wood.hit",
          "minecraft:stone": "minecraft:block.stone.hit",
          "minecraft:iron_block": "minecraft:block.metal.hit",
          "minecraft:glass": "minecraft:block.glass.hit",
          "minecraft:dirt": "minecraft:block.gravel.hit",
          "minecraft:sand": "minecraft:block.sand.hit"
        },
        "tag_sounds": {
          "minecraft:logs": "minecraft:block.wood.hit",
          "minecraft:planks": "minecraft:block.wood.hit"
        }
      }
    }

Sound resolution uses the same hierarchy as textures: exact block override,
then the first matching tag in lexicographic identifier order, then the
configured default. A value of "none" (also "disabled", "off", "false" or
"silent") explicitly disables that exact/tag/default entry; an absent entry
inherits normally and is not treated as disabled. Sound ids are registry ids,
not filesystem paths.

As an additional option, an identifier ending in ".ogg" is treated as a direct
physical resource:

    "soundsPlayOnperBlockHit": {
      "enabled": true,
      "default": "pointblankextra:sounds/bullet_holes/impact_default/impact_default_1.ogg",
      "impact_volume": 0.5,
      "surface_sounds": {
        "minecraft:oak_planks": "pointblankextra:sounds/bullet_holes/wood_impact_default/wood_impact_default_1.ogg",
        "minecraft:glass": "pointblankextra:sounds/bullet_holes/glass_impact_default/glass_impact_default_1.ogg",
        "minecraft:dirt": "pointblankextra:sounds/bullet_holes/dirt_impact_default/dirt_impact_default_1.ogg"
      },
      "tag_sounds": {
        "minecraft:logs": "pointblankextra:sounds/bullet_holes/wood_impact_default/wood_impact_default_1.ogg"
      }
    }

The physical location maps to assets/<namespace>/sounds/<path>.ogg. Registered
sound event ids still take precedence and keep the ordinary registered-event
playback path. A physical .ogg is played through a client SoundInstance backed
by the resource manager, so it does not need a sounds.json entry. The resource
manager validates the file, the normal sound engine supplies positional
attenuation, and its static SoundBuffer cache handles decoded audio. Missing
files are warned about once and skipped. Content-pack OGGs must be visible
through an installed resource pack; Point Blank's pointblank_resources pack
exposes assets from <gameDir>/pointblank/ extension packs, but an arbitrary ZIP
that is not mounted as a resource pack is not visible to Minecraft. Use the
namespace present under that pack's assets/ directory. For example, a file in
pointblank/MyPack/assets/pointblank/sounds/custom/hit.ogg is referenced as
pointblank:sounds/custom/hit.ogg.

"impact_volume" is optional and defaults to the value in BulletHoleDefaults.java
(1.5 by default). It is read as a finite number and negative values are clamped
to 0. It controls loudness only. Minecraft clamps the final SoundEngine channel
gain to 1.0, so the addon caps the instance volume at 1.0; values below 1 make
impacts quieter, values at or above 1 play at full gain.

"max_distance" is optional and defaults to 16 blocks (the value in
BulletHoleDefaults.java), clamped to 1-128. The impact sound is positioned at
the exact hit coordinates, fades linearly with distance and is stopped at
max_distance, so it becomes inaudible there. This limit is completely
independent of impact_volume: raising the volume cannot make the sound travel
farther. The same rule is applied to registered SoundEvents and to physical
.ogg sounds. Pitch remains 1.0. The pack/gun inheritance rules apply
independently to both values: absent inherits, an explicit value overrides.

The bundled impact OGGs are mono. Minecraft's OpenAL Soft backend only applies
directional panning (and, in older setups, distance attenuation) to mono
three-dimensional sources; stereo files are treated as non-positional. Keep
custom impact OGGs mono if you want them to pan correctly.

Built-in sound defaults (the authoritative list is in BulletHoleDefaults.java):
  minecraft:stone, cobblestone,
  deepslate, stone_bricks             -> minecraft:block.stone.hit
  iron/gold/copper/netherite blocks,
  iron bars/doors/trapdoors,
  anvils and chains                   -> pointblankextra:bullet_holes.metal
  minecraft:sand                      -> minecraft:block.sand.hit
  minecraft:beacon                    -> pointblankextra:bullet_holes.glass
  minecraft:logs, planks,
  bamboo_blocks (tags)                -> pointblankextra:bullet_holes.wood
  c:glass_blocks, c:glass_panes       -> pointblankextra:bullet_holes.glass
  minecraft:dirt (tag)                -> pointblankextra:bullet_holes.dirt
  c:metal_blocks (tag)                -> pointblankextra:bullet_holes.metal
  stone tags and stone surfaces       -> minecraft:block.stone.hit
  unmatched blocks                    -> pointblankextra:bullet_holes.default
The fallback is the mod's own impact sound, registered as
pointblankextra:bullet_holes.default and backed by
assets/pointblankextra/sounds/bullet_holes/impact_default/ (see sounds.json).
bullet_holes.wood, bullet_holes.glass, bullet_holes.dirt and
bullet_holes.metal are registered as well and are used by the built-in
mappings; they are also available to content packs. The remaining ids are
vanilla 26.1.2 SoundEvents.

The sound object obeys the same built-in -> pack -> gun inheritance as the
textures. A weapon can override one entry without losing the rest; surface and
tag maps merge entry by entry, the default is independent and enabled can be
overridden explicitly. The sound feature is independent of visual decal
creation: a gun/pack can disable decals but keep sounds (or vice versa). The
sound object's own enabled switch is the audio gate. The client global
"enableMod" switch also mutes addon sound playback; "enableBulletHoles" remains
the visual decal master switch and does not mute sounds by itself.

The existing impact payload carries the selected sound id to nearby clients,
which play it locally through Minecraft's sound engine; no second sound packet
is sent and the dedicated server never calls client audio code. A syntactically
valid but unregistered optional sound falls back to the configured default; a
missing default simply produces no sound. Custom sound ids must be registered
sound events. Dropping an OGG or a sounds.json file into an arbitrary ZIP does
not register a new event; a mod/resource pack must provide the registered event
and its audio resource. Vanilla ids can be used immediately without registering
anything or adding sound files.



Weapon JSON placement
---------------------
Point Blank reads weapons from:
  1. Built-in: data/pointblank/items/<name>.json inside the Point Blank JAR.
  2. Extensions: <gamedir>/pointblank/<pack>/items/<name>.json (folder or zip).

This addon reads those same JSON files and extracts only "toggle_aiming",
"melee", the ADS-related settings, the optional "custom_crosshair" feature and
the optional "bulletHoles" feature. It never writes to them and never modifies
Point Blank's parsing. No separate per-weapon config file is needed.

See examples/example_weapon.json and examples/example_weapon_disabled.json.
Copy them into an extension's items/ folder and rename the "name" field.

IMPORTANT: animation names must exactly match names in the weapon model's
GeckoLib .animation.json file. Point Blank conventionally prefixes with
"animation.model." (e.g. "animation.model.fire").

Building
--------
Java 25 required (NeoForge 26.1.2). Run:  ./gradlew build
Output: build/libs/pointblankextra-<version>.jar

Integration (without replacing Point Blank)
-------------------------------------------
- JSON:       indexes original weapon JSON; Point Blank's parser is untouched.
- Toggle aim: Mixin into ClientEventHandler.onClientTick latches RMB input;
              Point Blank's own aiming system does the real work.
- Melee anim: Mixin into GunItem.registerControllers adds one dedicated,
              non-interfering GeckoLib controller ("pbextra_melee") that stays
              stopped unless a melee animation is triggered.
- Networking: separate NeoForge payload channel; Point Blank's channel is
              untouched.
- Particles:  Listener on Point Blank's own event bus (Platform#getEventBus,
              @SubscribeEvent2) for BlockHitEvent, never cancelled; Point Blank's
              block-hit logic still runs unchanged.
- Bullet holes: same BlockHitEvent, plus a HurtingItem mixin that only records
              the firing item while the event is dispatched. The server sends a
              small impact payload to nearby clients; clients keep chunk-indexed
              decal records and render them through SubmitCustomGeometryEvent.
              No entities, block entities, particles or per-decal ticking.
- Crosshair:  Mixin redirect on Point Blank's own
              ClientEventHandler.onRenderOverlay crosshair draw call. Native
              visibility, global crosshair configuration, scope handling and
              smoothed expansion are reused; custom artwork replaces only the
              draw call. No layer wrapper and no parallel spread simulation.

Config: config/pointblankextra-client.toml  ->  enableMod (master switch),
        enableWaterLavaImpactParticles (Water/Lava impact particle toggle),
        enableBulletHoles (bullet-hole master switch, default true).

