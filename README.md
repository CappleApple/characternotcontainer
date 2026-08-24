# Character Not Container

Character Not Container is a NeoForge 1.21.1 character and equipment screen. It opens independently on a configurable key (default `C`), while Minecraft's `E` inventory remains vanilla. When Curios is installed, its inventory buttons open the character screen by default.

## Features

* Shows the current effective value of each player attribute that differs from that player's base value.
* Explains changed attributes with exact additive or percentage amounts from armor, held items, Curios, active effects, and direct Needs, Not Necessities sources.
* Active meals use the food's item icon and name. Modifiers that cannot be identified are combined into a single `???` remainder rather than attributed incorrectly.
* Renders the real local player with their skin, armor, animation, render layers, and mouse-driven head/body tracking.
* Uses broad head, chest, legs, and feet regions on the player model as equipment controls instead of displaying a traditional armor grid.
* Opens a picker beside the selected equipment region containing compatible items from the player inventory and nearby equipment sources.
* Supports nearby armor stands and blocks or entities exposing NeoForge's generic item-handler capability when the mod is installed on the server.
* Supports dynamic Curios slots, including multiple slots of the same type and mod-added slot types.
* Uses Curios' native slot icons and validation.
* Supports switching between functional and cosmetic Curios equipment directly from the character screen.
* Curios is fully optional.

## Configuration

First use creates the following files under:

```text
config/characternotcontainer/
```

```text
general.json
stats.json
equipment_screen.json
```

### Equipment anchors

The interface and anchor positions are built into the mod.

`equipment_screen.json` maps Curios slot-type IDs to fixed character-screen anchors. The generated file includes mappings for common Curios slot names:

```json
{
  "slots": {
    "head": "head",
    "hat": "head",
    "face": "head",
    "mask": "head",
    "neck": "neck",
    "necklace": "neck",
    "amulet": "neck",
    "back": "back",
    "cape": "back",
    "wings": "back",
    "belt": "belt",
    "spellbook": "belt",
    "waist": "belt",
    "sheath": "belt",
    "body": "belt",
    "hands": "hands",
    "hand": "hands",
    "ring": "hands",
    "bracelet": "hands",
    "feet": "feet",
    "shoes": "feet",
    "anklet": "feet",
    "charm": "other",
    "curio": "other"
  }
}
```

Valid anchors are:

```text
head
neck
back
belt
hands
left_hand
right_hand
feet
other
```

Add another slot ID as a key to bind a modded Curios type to an anchor. Namespaced IDs are supported. Unknown slot types use `other`.

Older UI-builder and movable-anchor configurations are migrated automatically and deprecated fields are removed.

### General settings

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

Set `redirectCuriosInventoryButton` to `false` to restore Curios' normal inventory button behavior.

`enableNearbyEquipmentSources` controls whether the picker searches nearby armor stands and item-handler inventories. Nearby equipment requires Character Not Container on the server. Client-only installations retain the character UI, but nearby pulls and equipment changes are unavailable.

`nearbyEquipmentSearchRadius` is capped at 32 blocks.

Set `equipmentPickerOpensLeft` to `true` to open the picker on the left side of the selected slot.

`equipmentPickerColumns` and `equipmentPickerVisibleRows` control picker wrapping and scrolling. Their defaults are 5 columns and 3 visible rows.

`debugEquipmentHitboxes` displays the body-region equipment controls for layout tuning.

`showEquippedItemIcons` adds a small item icon over occupied vanilla armor regions. Empty equipment regions always use the vanilla empty-equipment icon.

### Attribute configuration

`stats.json` defines the ordered attribute catalog shown on the character screen.

An attribute definition can:

* Set `visible` to `false`.
* Override its localized `name`.
* Provide an `icon` sprite.
* Adjust its value formatting.

Supported player attributes missing from the file are automatically appended when joining a world or reloading client resources.

Example:

```json
{
  "attributes": [
    {
      "attribute": "minecraft:generic.movement_speed",
      "visible": true,
      "format": "DECIMAL",
      "decimalPlaces": 0,
      "scale": 1000.0,
      "suffix": "%"
    }
  ]
}
```

Leave `name` blank to use the attribute's localized display name.

If an attribute has no translation, its registry ID is converted into a readable fallback name. For example:

```text
minecraft:generic.movement_speed
```

becomes:

```text
Movement Speed
```

A nonblank JSON `name` always takes priority.

Run:

```text
/characterui attributes
```

to log registered attribute IDs.

Operators can run:

```text
/characterui reload
```

after editing `general.json` or `stats.json`.

`equipment_screen.json` is re-read whenever the character screen opens.

Attribute changes are measured against the player's current attribute-instance base value. If another mod replaces that base value directly, the replacement becomes the new baseline.

Sources are identified from supported equipment, active effects, and direct integrations. Any remaining unattributed difference is shown as a combined `???` row with its net effect.

## Optional GUI Sprites

The character-screen layout is built into the mod, but resource packs can replace its major visual elements through fixed GUI sprite resource locations.

Character Not Container does not bundle sprites at these locations. If a PNG is absent, that element uses the default procedural rendering.

No configuration entry is required.

Every sprite ID begins with:

```text
characternotcontainer:character_screen/
```

and maps to:

```text
assets/characternotcontainer/textures/gui/sprites/character_screen/
```

For example:

```text
characternotcontainer:character_screen/picker/background
```

loads:

```text
assets/characternotcontainer/textures/gui/sprites/character_screen/picker/background.png
```

Supported sprite IDs:

| Resource location after `character_screen/`                                                                        | Destination size and purpose                                                                             |
| ------------------------------------------------------------------------------------------------------------------ | -------------------------------------------------------------------------------------------------------- |
| `screen/background`                                                                                                | Entire 400 x 320 logical screen                                                                          |
| `screen/player_preview_background`                                                                                 | 220 x 296, behind the player                                                                             |
| `screen/player_preview_frame`                                                                                      | 220 x 296, above the player and below equipment controls                                                 |
| `stats/panel`                                                                                                      | 134 pixels wide; height shrinks to the visible rows                                                      |
| `stats/row_hovered`                                                                                                | 134 x 26 logical row                                                                                     |
| `stats/scrollbar_track`                                                                                            | 8 x 258 logical track                                                                                    |
| `stats/scrollbar_thumb`                                                                                            | 6 pixels wide with a dynamic height                                                                      |
| `stats/fallback_icon`                                                                                              | 16 x 16 replacement for the generated letter icon                                                        |
| `equipment/slot`                                                                                                   | Base body-control overlay; 60 x 52 for head, 112 x 88 for chest, 78 x 75 for legs, or 78 x 56 for feet   |
| `equipment/slot_hovered`, `equipment/slot_selected`                                                                | Hover/selection overlays using the same body-control destination                                         |
| `equipment/curio_slot`, `equipment/curio_slot_hovered`, `equipment/curio_slot_selected`                            | Functional Curios slot and state overlays, 18 x 18                                                       |
| `equipment/cosmetic_curio_slot`, `equipment/cosmetic_curio_slot_hovered`, `equipment/cosmetic_curio_slot_selected` | Cosmetic Curios slot and state overlays, 18 x 18                                                         |
| `equipment/show_cosmetics_button`, `equipment/show_cosmetics_button_hovered`                                       | Complete 18 x 18 button shown in functional mode                                                         |
| `equipment/show_equipment_button`, `equipment/show_equipment_button_hovered`                                       | Complete 18 x 18 button shown in cosmetic mode                                                           |
| `picker/background`                                                                                                | Dynamic picker panel: columns x 20 + 10 pixels, plus 5 pixels when scrolling; rows x 20 + 10 pixels high |
| `picker/cell`, `picker/cell_hovered`                                                                               | Picker cell and hover overlay, 18 x 18                                                                   |
| `picker/unequip`                                                                                                   | 16 x 16 replacement for the default red `x`                                                              |
| `picker/scrollbar_track`, `picker/scrollbar_thumb`                                                                 | 2 pixels wide with dynamic heights                                                                       |
| `tooltip/source_background`                                                                                        | Dynamic background used by the custom attribute-source tooltip                                           |

State sprites are drawn over their corresponding base slot or cell sprite, so transparent borders and highlights work as expected.

If a hover or selection sprite is missing, the mod falls back to its procedural outline.

The cosmetics/equipment toggle sprites replace the complete button state, including the default `C` or `E` label.

Dynamic GUI elements can provide a sibling `.png.mcmeta` file using Minecraft GUI nine-slice scaling metadata to prevent borders from stretching.

Reload resource packs with:

```text
F3 + T
```

after changing GUI sprites.

## API

Other mods can register configured-style attributes through:

```java
CharacterUiApi.registerStat(...)
```

Registry-ID configuration through `stats.json` remains available without requiring an API dependency.

## Development

Requires Java 21.

```powershell
.\gradlew.bat test build
.\gradlew.bat runServer
.\gradlew.bat runClient
```

Pass:

```text
-PskipCuriosRuntime
```

when launching to test without the optional Curios runtime.

Sophisticated Storage and Sophisticated Core are development-only runtime dependencies. Pass:

```text
-PskipSophisticatedStorageRuntime
```

to omit both.

For local Needs, Not Necessities integration testing, pass:

```text
-PneedsNotNecessitiesRuntimeJar=<absolute-path-to-jar>
```
