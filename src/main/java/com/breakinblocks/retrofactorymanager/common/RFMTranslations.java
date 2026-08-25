package com.breakinblocks.retrofactorymanager.common;

import com.breakinblocks.retrofactorymanager.data.IoSide;
import com.breakinblocks.retrofactorymanager.data.NodeType;
import com.breakinblocks.retrofactorymanager.data.PinRole;
import com.breakinblocks.retrofactorymanager.data.RoundRobinMode;
import com.breakinblocks.retrofactorymanager.data.TagMode;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.ArrayList;
import java.util.List;

public final class RFMTranslations {
    public record Entry(String key, String english) {
        public MutableComponent component(Object... args) {
            return args.length == 0 ? Component.translatable(key) : Component.translatable(key, args);
        }

        public String string(Object... args) {
            return component(args).getString();
        }
    }

    private static final List<Entry> ALL = new ArrayList<>();

    private RFMTranslations() {
    }

    private static Entry add(String key, String english) {
        Entry entry = new Entry(key, english);
        ALL.add(entry);
        return entry;
    }

    private static Entry gui(String suffix, String english) {
        return add("gui.retrofactorymanager." + suffix, english);
    }

    private static Entry error(String suffix, String english) {
        return add("error.retrofactorymanager." + suffix, english);
    }

    private static Entry summary(String suffix, String english) {
        return add("summary.retrofactorymanager." + suffix, english);
    }

    public static final Entry TITLE = gui("title", "Blueprint Editor");
    public static final Entry SFM_EDITOR_NAME = add("gui.sfm.preferred_editor.retrofactorymanager.blueprint", "Blueprint (RFM)");
    public static final Entry SFM_PREFERRED_EDITOR = add("gui.sfm.preferred_editor", "Editor");
    public static final Entry SFM_EDITOR_V1 = add("gui.sfm.preferred_editor.sfm.v1", "V1 (Default)");
    public static final Entry SFM_EDITOR_V2 = add("gui.sfm.preferred_editor.sfm.v2", "V2");
    public static final Entry SFM_EDITOR_DRAW = add("gui.sfm.preferred_editor.sfm.draw", "Draw");
    public static final Entry SAVE_CLOSE = gui("save_close", "Save & Close");
    public static final Entry SFML_PREVIEW = gui("sfml_preview", "SFML Preview");
    public static final Entry CANCEL = gui("cancel", "Cancel");
    public static final Entry ADD = gui("add", "Add");
    public static final Entry ADD_CONDITION = gui("add_condition", "+ Add");
    public static final Entry ADD_NODE = gui("add_node", "Add Node");
    public static final Entry TEXT_MODE = gui("text_mode", "Text mode");
    public static final Entry VISUAL_EDITOR = gui("visual_editor", "Visual editor");
    public static final Entry JOIN = gui("join", "Join: %s");
    public static final Entry ON = gui("on", "On");
    public static final Entry OFF = gui("off", "Off");
    public static final Entry EACH = gui("each", "Each: %s");
    public static final Entry EMPTY_ONLY = gui("empty_only", "Empty only: %s");
    public static final Entry GLOBAL_ALIGN = gui("global_align", "Global align: %s");
    public static final Entry NOT = gui("not", "Not: %s");
    public static final Entry TICKS = gui("ticks", "Ticks");
    public static final Entry SECONDS = gui("seconds", "Seconds");
    public static final Entry SET_ANY = gui("set_any", "Any");
    public static final Entry ROW_LABEL = gui("row_label", "Label");
    public static final Entry ROW_REDSTONE = gui("row_redstone", "Redstone");
    public static final Entry STATS = gui("stats", "%s nodes · %s wires · %s%%");
    public static final Entry LABELS_AVAILABLE = gui("labels_available", "%s labels available");
    public static final Entry HINT = gui("hint", "Right-click: add node · Drag pins: connect · Alt-click pin or wire: disconnect");
    public static final Entry LEGACY_WARNING = gui("legacy_warning", "Existing program has no graph data - saving will replace it");
    public static final Entry ERRORS_ONE = gui("errors_one", "1 program error - fix to save");
    public static final Entry ERRORS_MANY = gui("errors_many", "%s program errors - fix to save");
    public static final Entry VALIDATION_FAILED = gui("validation_failed", "Validation failed: %s");
    public static final Entry INTERVAL = gui("interval", "Interval");
    public static final Entry OFFSET = gui("offset", "Offset");
    public static final Entry OFFSET_HINT = gui("offset_hint", "Offset (+ ticks)");
    public static final Entry OFFSET_HINT_SECONDS = gui("offset_hint_seconds", "Offset (+ seconds)");
    public static final Entry OFFSET_WARNING = gui("offset_warning", "Offset >= interval:");
    public static final Entry OFFSET_WARNING2 = gui("offset_warning2", "timer will never run");
    public static final Entry MIN_INTERVAL_WARNING = gui("min_interval_warning", "Below server min (20t)");
    public static final Entry MIN_INTERVAL_WARNING2 = gui("min_interval_warning2", "unless energy-only IO");
    public static final Entry NO_OPTIONS = gui("no_options", "No options");
    public static final Entry QTY = gui("qty", "Qty");
    public static final Entry KEEP = gui("keep", "Keep");
    public static final Entry AMOUNT = gui("amount", "Amount");
    public static final Entry RESOURCE = gui("resource", "Resource");
    public static final Entry ITEMS_HEADER = gui("items_header", "Items (blank = all)");
    public static final Entry FILTER_GROUP = gui("filter_group", "Filter %s");
    public static final Entry ADD_FILTER = gui("add_filter", "+ Add filter");
    public static final Entry MORE_ITEMS = gui("more_items", "+%s more");
    public static final Entry SIDES = gui("sides", "Sides");
    public static final Entry SIDE_EACH = gui("side_each", "Each");
    public static final Entry SLOTS = gui("slots", "Slots");
    public static final Entry LABELS_HEADER = gui("labels_header", "Labels");
    public static final Entry LABEL_SCROLL = gui("label_scroll", "%s/%s (scroll)");
    public static final Entry NO_LABELS = gui("no_labels", "(no labels yet - add below)");
    public static final Entry GUN_ONLY_NOTE = gui("gun_only_note", "* on gun only - push to the manager");
    public static final Entry UNASSIGNED_NOTE = gui("unassigned_note", "Some labels have no blocks assigned");
    public static final Entry PROGRAM_NAME = gui("program_name", "Program name");
    public static final Entry CONDITION_SFML = gui("condition_sfml", "Condition (SFML)");
    public static final Entry CONDITIONS = gui("conditions", "Conditions");
    public static final Entry FORGET_NOTE = gui("forget_note", "Empty = forget all");
    public static final Entry COMMENT_TEXT = gui("comment_text", "Text");
    public static final Entry EMPTY_PROGRAM = gui("empty_program", "(empty program)");
    public static final Entry NEW_LABEL = gui("new_label", "New label");
    public static final Entry NEW_ITEM = gui("new_item", "New item");
    public static final Entry LABEL_PLACEHOLDER = gui("label_placeholder", "label");
    public static final Entry ITEM_FILTER_OPTIONAL = gui("item_filter_optional", "item filter (optional)");
    public static final Entry NOTHING_TO_UNDO = gui("nothing_to_undo", "Nothing to undo");
    public static final Entry NOTHING_TO_REDO = gui("nothing_to_redo", "Nothing to redo");
    public static final Entry UNDO_HINT = gui("undo_hint", "Ctrl+Z undo · Ctrl+Y redo · Ctrl+C/V/D copy · G snap");
    public static final Entry IMPORT_PROGRAM = gui("import_program", "Import to canvas");
    public static final Entry IMPORT_AVAILABLE = gui("import_available", "This program was written as text - use Import to build it as nodes");
    public static final Entry IMPORT_APPROXIMATE = gui("import_approximate", "Import is approximate: %s");
    public static final Entry IMPORT_DONE = gui("import_done", "Imported %s nodes");
    public static final Entry IMPORT_CHECK_PREVIEW = gui("import_check_preview", "check the SFML preview against the original");
    public static final Entry COPIED = gui("copied", "Copied %s nodes");
    public static final Entry PASTED = gui("pasted", "Pasted %s nodes");
    public static final Entry SNAP = gui("snap", "Snap to grid: %s");
    public static final Entry FORGE_ENERGY = gui("forge_energy", "Forge Energy");
    public static final Entry WARN_NO_LABELS = gui("warn_no_labels", "No labels selected");
    public static final Entry WARN_FAST_TIMER = gui("warn_fast_timer", "Interval below the 20 tick server minimum");
    public static final Entry ERRORS_CLICK_HINT = gui("errors_click_hint", "click to show");
    public static final Entry TAGS_HEADER = gui("tags_header", "Tags");
    public static final Entry TAG_MODE = gui("tag_mode", "Tags: %s");
    public static final Entry TAG_PLACEHOLDER = gui("tag_placeholder", "c:ores");
    public static final Entry EXCEPT_HEADER = gui("except_header", "Except (never move these)");
    public static final Entry EXCEPT_PLACEHOLDER = gui("except_placeholder", "minecraft:dirt");
    public static final Entry ROUND_ROBIN = gui("round_robin", "Round robin: %s");
    public static final Entry PIN_EXEC_IN = gui("pin_exec_in", "Runs after the connected node");
    public static final Entry PIN_EXEC_OUT = gui("pin_exec_out", "Runs the next node");
    public static final Entry PIN_TRUE_HINT = gui("pin_true_hint", "Runs when the condition is true");
    public static final Entry PIN_FALSE_HINT = gui("pin_false_hint", "Runs when the condition is false");
    public static final Entry PIN_AFTER_HINT = gui("pin_after_hint", "Runs after the branch finishes, either way");
    public static final Entry WARN_NO_BLOCKS = gui("warn_no_blocks", "No blocks assigned to this label");
    public static final Entry LABEL_BLOCK_COUNT = gui("label_block_count", "%s blocks");
    public static final Entry UNSAVED = gui("unsaved", "Unsaved changes");
    public static final Entry SAVE_BLOCKED = gui("save_blocked", "Fix the program errors before saving");

    public static final Entry ERROR_NODE_MISSING = error("node_missing", "Node no longer exists");
    public static final Entry ERROR_SELF_CONNECT = error("self_connect", "Cannot connect a node to itself");
    public static final Entry ERROR_INVALID_SOURCE = error("invalid_source", "Invalid source pin");
    public static final Entry ERROR_NO_INPUT_PIN = error("no_input_pin", "Target node has no input pin");
    public static final Entry ERROR_CYCLE = error("cycle", "Connection would create a loop");
    public static final Entry ERROR_INCOMPATIBLE_PINS = error("incompatible_pins", "Connect an output pin to an input pin");

    public static final Entry SUMMARY_TIMER = summary("timer", "Every %s%s %s");
    public static final Entry SUMMARY_TIMER_GLOBAL = summary("timer_global", "Every %s global%s %s");
    public static final Entry SUMMARY_TICKS = summary("ticks", "ticks");
    public static final Entry SUMMARY_SECONDS = summary("seconds", "seconds");
    public static final Entry SUMMARY_REDSTONE_PULSE = summary("redstone_pulse", "Every redstone pulse");
    public static final Entry SUMMARY_FROM = summary("from", "From: %s");
    public static final Entry SUMMARY_FROM_EACH = summary("from_each", "From each: %s");
    public static final Entry SUMMARY_TO = summary("to", "To: %s");
    public static final Entry SUMMARY_TO_EACH = summary("to_each", "To each: %s");
    public static final Entry SUMMARY_UNSET = summary("unset", "(unset)");
    public static final Entry SUMMARY_EMPTY_SLOTS = summary("empty_slots", "Empty slots only");
    public static final Entry SUMMARY_EACH_SIDE = summary("each_side", "Each side");
    public static final Entry SUMMARY_EXCEPT = summary("except", "Except %s");
    public static final Entry SUMMARY_SIDES = summary("sides", "%s side");
    public static final Entry SUMMARY_SLOTS = summary("slots", "Slots %s");
    public static final Entry SUMMARY_ALL_LABELS = summary("all_labels", "All labels");
    public static final Entry SUMMARY_COMMENT_EMPTY = summary("comment_empty", "(empty)");
    public static final Entry SUMMARY_NO_CONDITION = summary("no_condition", "(no condition)");

    static {
        for (NodeType type : NodeType.values()) {
            add(type.translationKey(), type.displayName());
        }
        add(PinRole.TRUE_OUT.labelKey(), "True");
        add(PinRole.FALSE_OUT.labelKey(), "False");
        add(PinRole.AFTER_OUT.labelKey(), "After");
        for (IoSide side : IoSide.values()) {
            add(side.translationKey(), side.shortName());
        }
        add(RoundRobinMode.NONE.translationKey(), "Off");
        add(RoundRobinMode.BY_LABEL.translationKey(), "By label");
        add(RoundRobinMode.BY_BLOCK.translationKey(), "By block");
        add(TagMode.NONE.translationKey(), "Off");
        add(TagMode.WITH.translationKey(), "With");
        add(TagMode.WITHOUT.translationKey(), "Without");
    }

    public static List<Entry> entries() {
        return List.copyOf(ALL);
    }
}
