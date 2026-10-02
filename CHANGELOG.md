# Changelog

## 1.0.4

### Added

* Brazilian Portuguese (pt_br) translation, contributed by PrincessStellar

## 1.0.3

### Fixed

* Timer nodes with global align turned on and an offset set now generate valid SFML. The alignment keyword was written after the offset (`EVERY 20 + 11 GLOBAL TICKS`), which SFM rejects; it now comes first (`EVERY 20 GLOBAL + 11 TICKS`)
* The side panel now scrolls far enough to read the whole SFML preview while a node is selected. The scroll range ignored the preview section, so it stopped at the end of the node settings
* The item suggestion dropdown and the add-node menu now draw above everything else. Text from the panel underneath was showing through them even though the panel behind the list is opaque

### Changed

* Timer node labels list the alignment before the offset, matching the generated program
* The timer offset hint reads "Offset (+ seconds)" when the interval is set to seconds, since SFM scales the offset by 20 in that mode
* The timer inspector warns when the offset is at or above the interval, a combination that stops the timer from ever running

## 1.0.2

### Fixed

* The item suggestion dropdown, the add-node menu and the inspector panel no longer let the text behind them show through
* Placeholder hints in text fields now disappear as soon as you start typing, instead of overlapping what you type, and come back when you clear the field

## 1.0.1

### Changed

* The editor switcher in the blueprint editor is now provided by RetroFactoryManager itself, so you can swap between Blueprint, V1, V2, and Draw editors on current SFM releases
* Added English display names for SFM's built-in editors in the switcher

### 1.21.1

* Backported to Minecraft 1.21.1 / NeoForge (requires SFM 4.34.0+)
* SFM's Program Editor Config screen now lists every registered editor, including Blueprint, instead of only V1 and V2

## 1.0.0

Initial release.

RetroFactoryManager adds a visual node editor to Super Factory Manager. Build your factory logic on a canvas and the mod writes the SFML program for you.

### Other

* Fully translatable, with English included
