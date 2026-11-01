# Changelog

## 1.0.7

### Added

* The SFML preview scrolls sideways for lines too long to fit the side panel. Use Shift+scroll, a horizontal trackpad swipe, or drag the bar along the bottom of the panel

### Fixed

* The "Program name" label stayed in place when the side panel scrolled and drew over the SFML preview
* SFML preview lines ran past the edge of the side panel instead of being cut off at it
* Long If conditions drew over the True, False and After labels. Node text that does not fit now ends in "...", and hovering the node shows it in full

## 1.0.6

### Added

* Right-clicking a wire removes that connection
* Right-clicking a node opens a menu with a Delete option for the selected nodes

## 1.0.5

### Added

* Editor dropdown in SFM's V1 and V2 editors, so you can switch back to the Blueprint editor
* Editor choice on SFM's Program Editor Config screen, covering every registered editor including Blueprint

### Fixed

* Closing the V2 editor after switching to it reopened an out-of-date copy of the editor you switched from

### Changed

* Wires between nodes now draw as smooth curves at any zoom level or GUI scale

## 1.0.3

### Fixed

* Timer nodes with global align turned on and an offset set now generate valid SFML. The alignment keyword was written after the offset (`EVERY 20 + 11 GLOBAL TICKS`), which SFM rejects; it now comes first (`EVERY 20 GLOBAL + 11 TICKS`)
* The side panel now scrolls far enough to read the whole SFML preview while a node is selected. The scroll range ignored the preview section, so it stopped at the end of the node settings

### Changed

* Timer node labels list the alignment before the offset, matching the generated program
* The timer offset hint reads "Offset (+ seconds)" when the interval is set to seconds, since SFM scales the offset by 20 in that mode
* The timer inspector warns when the offset is at or above the interval, a combination that stops the timer from ever running

### Added

* Brazilian Portuguese (pt_br) translation, contributed by PrincessStellar

## 1.0.2

### Fixed

* The item suggestion dropdown, the add-node menu and the inspector panel no longer let the text behind them show through
* Placeholder hints in text fields now disappear as soon as you start typing, instead of overlapping what you type, and come back when you clear the field

## 1.0.1

### Changed

* The editor switcher in the blueprint editor is now provided by RetroFactoryManager itself, so you can swap between Blueprint, V1, V2, and Draw editors on current SFM releases
* Added English display names for SFM's built-in editors in the switcher

## 1.0.0

Initial release.

RetroFactoryManager adds a visual node editor to Super Factory Manager. Build your factory logic on a canvas and the mod writes the SFML program for you.

### Other

* Fully translatable, with English included
