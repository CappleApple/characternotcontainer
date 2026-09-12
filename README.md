# Character Not Container

Character Not Container adds a dedicated character/equipment screen for Minecraft 1.21.1 on NeoForge.

It opens separately from the normal inventory (default key: **C**), so `E` can stay the vanilla inventory. The screen is built around the player model rather than a grid of equipment slots: click the head, chest, legs, or feet to manage armor, and use nearby Curios anchors for accessories when Curios is installed.

## What it does

- Renders the real local player with their skin, armor, animations, and normal render layers.
- Uses body regions as armor controls instead of a traditional four-slot armor grid.
- Opens an item picker beside the selected region with compatible equipment from your inventory.
- Can also pull from nearby armor stands and NeoForge item-handler inventories when the mod is installed server-side.
- Supports dynamic Curios slot types and multiple slots of the same type.
- Supports Curios cosmetic slots and per-slot render toggles.
- Shows player attributes that differ from their base value and breaks down known sources such as equipment, effects, Curios, Pufferfish Skills, and Needs, Not Necessities.
- Uses normal item tooltips for equipment, so other mods can keep adding their own tooltip information.

Curios is optional. Without it, the screen still works as a character/stat and vanilla-equipment interface.

## Attribute breakdowns

The stat panel is meant to answer a simple question: **where is this number coming from?**

For supported sources, changed attributes are shown with the exact additive or percentage contribution from each source. Anything the mod cannot identify safely is grouped into a `???` remainder instead of guessing.

There are direct integrations for a few mods where their own mechanics need special interpretation. For example, Armor Damage Scaling can provide its live Damage Resistance / Heavy Hit Resistance values, and Pufferfish Skills rewards can use the loaded skill names.

`stats.json` controls which attributes appear, their order, name, icon, number format, scale, and suffix.

## Equipment picker

Click a body region or Curios slot to open the equipment picker. It shows compatible items from the player's inventory and, if enabled, nearby equipment sources.

Nearby sources can include:

- armor stands;
- blocks/entities exposing NeoForge's item-handler capability; and
- other compatible equipment storage within the configured radius.

Nearby equipment and server-authoritative equipment changes require Character Not Container on the server. A client-only install still provides the screen and stat display.

## Curios and Relics

When Curios is installed, its inventory button can redirect to the character screen and all discovered Curios slot types are placed on configurable character-screen anchors.

Functional and cosmetic Curios can be switched directly from the screen. Empty-hand sneak-clicking a displayed Curio equips it into a compatible slot when possible.

Relics is also supported optionally: holding its configured research key over a relic can open the normal Relics research interface and return to the character screen afterward.

## Configuration

Files are created under:

```text
config/characternotcontainer/
```

The main files are:

```text
general.json
stats.json
equipment_screen.json
slots.json
```

`general.json` controls screen/key behavior, nearby equipment sources, picker layout, and a few display options.

`stats.json` controls the attribute list and formatting.

`equipment_screen.json` maps Curios slot IDs to named anchors, while `slots.json` controls the position and layout of those anchors. This makes mod-added Curios types configurable without hardcoding them into Character Not Container.

A minimal anchor looks like:

```json
{
  "slots": {
    "head": { "x": 319, "y": 52 },
    "hands": {
      "x": 199,
      "y": 165,
      "direction": "horizontal",
      "wrap": 2,
      "spacing": 20
    }
  }
}
```

Missing Curios bindings fall back to the `other` anchor.

Use:

```text
/characterui attributes
/characterui reload
```

when working on attribute/config definitions. `equipment_screen.json` and `slots.json` are read whenever the screen opens.

## Resource packs

The default layout is procedural, but resource packs can replace the major screen, stat-panel, equipment-slot, picker, scrollbar, and tooltip surfaces with GUI sprites.

Sprite IDs live below:

```text
characternotcontainer:character_screen/
```

which maps to:

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

Missing sprites simply fall back to the built-in rendering, so a pack can replace only the pieces it wants. Dynamic backgrounds can use Minecraft's normal GUI nine-slice metadata.

## API

Other mods can register stat definitions through:

```java
CharacterUiApi.registerStat(...)
```

Registry-ID configuration through `stats.json` remains available without an API dependency.

## Building

Requires Java 21.

```powershell
.\gradlew.bat test build
.\gradlew.bat runClient
.\gradlew.bat runServer
```

Curios and the other supported integrations are optional at runtime; the Gradle project includes flags for launching development runs without individual optional mods.
