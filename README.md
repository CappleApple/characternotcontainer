# Character Not Container

Character Not Container is a NeoForge 1.21.1 character and equipment screen. It opens independently on a configurable key (default `C`); Minecraft's `E` inventory remains vanilla, while the Curios buttons in the survival and creative player inventories open this character screen by default.

## Features

- Shows configured and automatically discovered attributes whose effective value differs from that player's own base value.
- Explains changed attributes with modifiers supplied by equipped armor, hands, and Curios only. Non-equipment changes still appear but do not receive invented tooltip sources.
- Renders the real local player, including skin, equipped armor, animation, normal render layers, and explicit head/body tracking toward the mouse.
- Turns broad, transparent head/chest/legs/feet body regions into the equipment controls instead of showing an armor grid.
- Opens a content-sized picker beside the selected body slot containing compatible player and nearby-source items plus an explicit Empty/Unequip action.
- Pulls from nearby armor stands and any block or entity exposing NeoForge's generic item-handler capability when the server also has the mod.
- Performs validated, server-authoritative swaps without dropping or deleting items. A one-for-one replacement needs no spare slot; a standalone unequip is refused when no inventory capacity exists.
- Discovers visible, active Curios slots dynamically, uses their native icons and validation, supports multiple slots, and keeps unknown slot types visible in the fallback region.
- Switches between functional and cosmetic Curios slots from inside the character screen, with both paths using validated server-authoritative swaps.
- Runs without Curios installed; Curios classes are isolated behind guarded compatibility entry points.

## Configuration

First use creates these files under `config/characternotcontainer/`:

```text
general.json
stats.json
equipment_screen.json
```

`equipment_screen.json` starts as a byte-for-byte copy of the supplied UI Builder layout. Its named custom widgets control the real player preview, body hitboxes, semantic Curios anchors, unknown-Curios fallback region, and picker bounds. Existing copies are never overwritten, so edits remain yours.

`general.json` contains:

```json
{
  "enableSeparateKeybind": true,
  "redirectCuriosInventoryButton": true,
  "enableNearbyEquipmentSources": true,
  "nearbyEquipmentSearchRadius": 8.0,
  "equipmentPickerOpensLeft": false,
  "equipmentPickerColumns": 5,
  "equipmentPickerVisibleRows": 3,
  "debugEquipmentHitboxes": false,
  "showEquippedItemIcons": false
}
```

Set `redirectCuriosInventoryButton` to `false` to restore Curios' normal button destination. Set `debugEquipmentHitboxes` to `true` while tuning the body regions. `showEquippedItemIcons` controls whether equipped vanilla armor also gets a small item indicator over the model; empty slots always show the vanilla empty-equipment icon.

Nearby equipment is server-authoritative and activates only when Character Not Container is present on the server. A client-only installation still connects normally and retains the character UI, but equipment changes and nearby pulls are unavailable. `nearbyEquipmentSearchRadius` is capped at 32 blocks. Set `equipmentPickerOpensLeft` to `true` to place the picker left of its selected slot; `equipmentPickerColumns` and `equipmentPickerVisibleRows` control wrapping and scrolling (defaults: 5 columns and 3 rows).

`stats.json` defines the ordered attribute catalog and per-attribute formatting. A stat can optionally set `icon` to a sprite resource location. Changed syncable attributes not listed in the catalog are appended automatically with generic formatting.

Example:

```json
{
  "attribute": "minecraft:generic.movement_speed",
  "name": "Movement Speed",
  "icon": "",
  "format": "CUSTOM",
  "decimalPlaces": 0,
  "scale": 1000.0,
  "offset": 0.0,
  "invert": false,
  "prefix": "",
  "suffix": "%"
}
```

Run `/characterui attributes` to log registered attribute IDs. Operators can run `/characterui reload` after editing `general.json` or `stats.json`; the equipment layout is re-read whenever the screen opens.

## API

Other mods may register configured-style attributes through `CharacterUiApi.registerCategory(...)` and `CharacterUiApi.registerStat(...)`. Registry-ID configuration remains the dependency-free option.

## Development

Requires Java 21.

```powershell
.\gradlew.bat test build
.\gradlew.bat runServer
.\gradlew.bat runClient
```

Pass `-PskipCuriosRuntime` when launching to verify the optional Curios path. Sophisticated Storage and Sophisticated Core are development-only runtime dependencies; pass `-PskipSophisticatedStorageRuntime` to omit both.
