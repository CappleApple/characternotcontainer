# Changelog

## 1.1 - 2026-08-24

### Added

- Added fixed optional GUI sprite locations for resource-pack skinning, with the existing procedural interface retained per missing asset.
- Added one-time world-join and client-reload discovery that persists new modded player attributes into the flat stats catalog.
- Added direct Needs, Not Necessities source support, including active-meal item icons and names.
- Added a per-attribute `visible` toggle to the flat stats configuration.

### Changed

- Replaced the full UI-builder equipment layout with Curios slot-type bindings to fixed built-in body anchors; existing full-layout and movable-anchor files migrate automatically.
- Replaced category-based stats configuration with a flat attribute list and removed deprecated delta-era formatting fields.
- Changed modified attributes to show their current effective value instead of exact additive or multiplicative changes.
- Changed source tooltips to show exact additive or percentage amounts for identified equipment, effect, and Needs sources.
- Shortened base-multiplier source amounts to their percentage and labeled multiplicatively stacking amounts as `stacking`.
- Changed attribute source tooltips to show the localized attribute name above their source list.
- Changed unresolved modifiers to one combined `???` row and stopped guessing names from modifier IDs or datapack paths.
- Changed the comparison baseline to the attribute instance's current base so base-value overrides alone are never listed as changes.

### Fixed

- Contained custom-rendered armor item models inside their picker cells so they cannot draw over the equipment selection menu.
- Captured replacement armor layers that write through Minecraft's world render buffers inside the isolated player preview, preventing AzureLib-style armor from drawing over slots or equipment pickers.
- Fixed per-source ADD_VALUE tooltips to show each attribute's sanitized standalone contribution instead of its raw modifier amount.

## 1.0.1 - 2026-08-22

### Changed

- Changed attribute labels to prefer explicit JSON names, then localized attribute names, then readable parsed registry IDs.

## 1.0 - 2026-08-22

### Added

- Added the separate keybound character/equipment screen while leaving the vanilla inventory unchanged.
- Added a JSON-driven real-player preview with transparent body-region equipment controls.
- Added compatible-item pickers and server-authoritative vanilla and Curios equipment swaps.
- Added dynamic semantic Curios placement, multiple-slot support, and an unknown-slot fallback region.
- Added an in-screen toggle between functional and cosmetic Curios slots.
- Added a configurable redirect from Curios' survival and creative inventory buttons to the character screen.
- Added optional-server nearby equipment sourcing from armor stands and generic NeoForge item-handler inventories.
- Added configurable slot-relative picker direction, columns, and visible rows with content-sized expansion.
- Added a scrollable changed-attributes-only panel with equipment-specific contribution tooltips.
- Added configurable attribute formatting, optional attribute icons, and equipment-hitbox debug rendering.

### Changed

- Changed the character preview to continuously turn its body and head toward the mouse.
