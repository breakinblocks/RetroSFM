package com.breakinblocks.retrofactorymanager.client.gui;

import ca.teamdman.sfm.client.screen.SFMScreenChangeHelpers;
import ca.teamdman.sfm.client.screen.text_editor.ISFMTextEditScreen;
import ca.teamdman.sfm.client.text_editor.ISFMTextEditScreenOpenContext;
import ca.teamdman.sfm.common.item.LabelGunItem;
import ca.teamdman.sfm.common.label.LabelPositionHolder;
import ca.teamdman.sfm.common.registry.SFMRegistryWrapper;
import ca.teamdman.sfm.common.registry.registration.SFMResourceTypes;
import ca.teamdman.sfm.common.resourcetype.RegistryBackedResourceType;
import ca.teamdman.sfm.common.resourcetype.ResourceType;
import ca.teamdman.sfml.program_builder.ProgramBuilder;
import com.breakinblocks.retrofactorymanager.RetroFactoryManager;
import com.breakinblocks.retrofactorymanager.common.RFMTranslations;
import com.breakinblocks.retrofactorymanager.data.BlueprintEdge;
import com.breakinblocks.retrofactorymanager.data.BlueprintGraph;
import com.breakinblocks.retrofactorymanager.data.BlueprintNode;
import com.breakinblocks.retrofactorymanager.data.ConditionRow;
import com.breakinblocks.retrofactorymanager.data.GraphSerde;
import com.breakinblocks.retrofactorymanager.data.IoSide;
import com.breakinblocks.retrofactorymanager.data.LimitEntry;
import com.breakinblocks.retrofactorymanager.data.NodeCategory;
import com.breakinblocks.retrofactorymanager.data.NodeSettings;
import com.breakinblocks.retrofactorymanager.data.NodeType;
import com.breakinblocks.retrofactorymanager.data.PinRole;
import com.breakinblocks.retrofactorymanager.data.SfmlGenerator;
import com.breakinblocks.retrofactorymanager.data.SfmlImporter;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RFMBlueprintEditorScreen extends Screen implements ISFMTextEditScreen {
    private static final int BACKGROUND = 0xFF15191E;
    private static final int GRID_MINOR = 0xFF1E242B;
    private static final int GRID_MAJOR = 0xFF2A323C;
    private static final int NODE_BODY = 0xFF1C232B;
    private static final int NODE_BORDER = 0xFF39434F;
    private static final int NODE_BORDER_SELECTED = 0xFF60A5FA;
    private static final int TEXT_PRIMARY = 0xFFE6EDF3;
    private static final int TEXT_MUTED = 0xFFA7B2BD;
    private static final int TEXT_STATUS_ERROR = 0xFFFF6B6B;
    private static final int TEXT_STATUS_WARN = 0xFFE8A33D;
    private static final int PIN_BORDER = 0xFF10151A;
    private static final int PIN_EXEC = 0xFFE6EDF3;
    private static final int PIN_TRUE = 0xFF5FBF7F;
    private static final int PIN_FALSE = 0xFFC96A6A;
    private static final int WIRE_EXEC = 0xFFCBD5E1;
    private static final int WIRE_HOVER = 0xFFFF6B6B;
    private static final int BOX_FILL = 0x3060A5FA;
    private static final int BOX_BORDER = 0xFF60A5FA;
    private static final int PANEL_BACKGROUND = 0xFF181E26;
    private static final int PANEL_BORDER = 0xFF4B5563;
    private static final int PALETTE_BACKGROUND = 0xFF202833;
    private static final int PALETTE_ROW_HOVER = 0xFF2E3947;
    private static final int CHECKBOX_BORDER = 0xFF6B7684;
    private static final double MIN_ZOOM = 0.25D;
    private static final double MAX_ZOOM = 3.0D;
    private static final double ZOOM_STEP = 1.15D;
    private static final double BASE_GRID_STEP = 32.0D;
    private static final int MAJOR_GRID_INTERVAL = 5;
    private static final double MIN_GRID_PIXEL_STEP = 12.0D;
    private static final int PALETTE_WIDTH = 150;
    private static final int PALETTE_HEADER_HEIGHT = 15;
    private static final int PALETTE_ROW_HEIGHT = 13;
    private static final int DOCK_WIDTH = 220;
    private static final int LABEL_ROW_HEIGHT = 13;
    private static final int MAX_VISIBLE_LABEL_ROWS = 6;
    private static final int MAX_VISIBLE_RESOURCE_ROWS = 4;
    private static final int MAX_LIMIT_GROUPS = 6;

    private enum DragMode {NONE, PAN_MMB, PAN_RMB, NODES, WIRE, BOX}

    private record PinHit(UUID nodeId, PinRole role) {
    }

    private final ISFMTextEditScreenOpenContext openContext;
    private final BlueprintGraph graph;
    private final boolean legacyProgramWithoutGraph;
    private String generatedProgram = "";
    private List<String> previewLines = List.of();
    private List<String> programErrors = List.of();
    private static final Pattern ERROR_LINE_PATTERN = Pattern.compile("line\\s+(\\d+):");
    private final Map<UUID, List<String>> nodeErrors = new HashMap<>();
    private final Map<UUID, String> nodeWarnings = new HashMap<>();
    private final List<UUID> errorNodeOrder = new ArrayList<>();
    private final List<int[]> errorRowHitboxes = new ArrayList<>();
    private Button saveButton;
    private boolean edited;
    private boolean unsavedChanges;

    private static final int MAX_HISTORY = 64;
    private final Deque<BlueprintGraph> undoStack = new ArrayDeque<>();
    private final Deque<BlueprintGraph> redoStack = new ArrayDeque<>();
    private BlueprintGraph previousState;
    private String lastHistoryKey;
    private String coalesceKey;
    private SfmlImporter.Result importable;
    private Button importButton;

    private double cameraX;
    private double cameraY;
    private double zoom = 1.0D;

    private final Set<UUID> selection = new LinkedHashSet<>();
    private DragMode dragMode = DragMode.NONE;
    private double panAnchorMouseX;
    private double panAnchorMouseY;
    private double panAnchorCameraX;
    private double panAnchorCameraY;
    private boolean rightDragMoved;
    private final Map<UUID, double[]> nodeDragOffsets = new HashMap<>();
    private UUID wireFromNode;
    private PinRole wireFromPin;
    private double wireMouseX;
    private double wireMouseY;
    private double boxStartX;
    private double boxStartY;
    private double boxCurrentX;
    private double boxCurrentY;
    private boolean boxAdditive;

    private static final String SFM_NAMESPACE = "sfm";
    private static final double SNAP_STEP = 16.0D;
    private static final double WIRE_HIT_DISTANCE = 6.0D;

    private record Clipboard(List<BlueprintNode> nodes, List<BlueprintEdge> edges) {
    }

    private static Clipboard clipboard;
    private boolean snapToGrid;
    private double lastMouseX;
    private double lastMouseY;
    private BlueprintEdge hoveredWire;
    private UUID pendingWireNode;
    private PinRole pendingWirePin;

    private boolean paletteOpen;
    private int paletteX;
    private int paletteY;
    private double paletteCanvasX;
    private double paletteCanvasY;

    private boolean previewOpen;

    private final List<AbstractWidget> inspectorWidgets = new ArrayList<>();
    private final Map<AbstractWidget, Integer> inspectorWidgetBaseY = new HashMap<>();
    private int dockScroll;
    private boolean needsInspectorRefresh;
    private UUID inspectedNodeId;
    private int inspectorBottom;
    private int labelRowsStartY;
    private int labelScroll;
    private String customLabelText = "";
    private EditBox customLabelField;
    private String customResourceText = "";
    private int customResourceGroup = -1;
    private final Map<Integer, EditBox> resourceAddFields = new HashMap<>();
    private final Map<Integer, EditBox> tagAddFields = new HashMap<>();
    private EditBox exceptAddField;
    private static final int EXCEPT_TARGET = -2;

    private record ItemEntry(String id, String name, String searchText, @Nullable ItemStack stack) {
    }

    private static List<ItemEntry> itemIndex;
    private List<ItemEntry> suggestions = List.of();
    private int suggestionConditionRow = -1;
    private int suggestionLimitGroup = -1;
    private int suggestionX;
    private int suggestionY;
    private boolean suggestionsAbove;

    private Component statusMessage = Component.empty();
    private long statusExpiresAt;

    public RFMBlueprintEditorScreen(ISFMTextEditScreenOpenContext openContext) {
        super(RFMTranslations.TITLE.component());
        this.openContext = openContext;
        String initial = openContext.initialValue();
        this.graph = GraphSerde.extract(initial).orElseGet(BlueprintGraph::new);
        this.legacyProgramWithoutGraph = graph.isEmpty() && !GraphSerde.strip(initial).isBlank();
        this.importable = legacyProgramWithoutGraph
                ? SfmlImporter.importProgram(GraphSerde.strip(initial)).orElse(null)
                : null;
        this.previousState = graph.copy();
        regenerate();
    }

    private void applyImport() {
        if (importable == null) {
            return;
        }
        graph.restoreFrom(importable.graph());
        if (importable.lossless()) {
            setStatusMessage(RFMTranslations.IMPORT_DONE.component(graph.nodes().size()));
        } else {
            String detail = importable.notes().isEmpty()
                    ? RFMTranslations.IMPORT_CHECK_PREVIEW.string()
                    : String.join("; ", importable.notes());
            setStatusMessage(RFMTranslations.IMPORT_APPROXIMATE.component(detail));
        }
        importable = null;
        selection.clear();
        markEdited();
        requestInspectorRefresh();
        frameAll();
        if (importButton != null) {
            removeWidget(importButton);
            importButton = null;
        }
    }

    @Override
    public ISFMTextEditScreenOpenContext openContext() {
        return openContext;
    }

    @Override
    protected void init() {
        super.init();
        int y = height - 26;
        saveButton = addRenderableWidget(Button.builder(
                        RFMTranslations.SAVE_CLOSE.component(),
                        button -> openContext.onSaveAndClose(buildContent()))
                .pos(8, y)
                .size(110, 20)
                .build());
        updateSaveButton();
        addRenderableWidget(Button.builder(
                        RFMTranslations.SFML_PREVIEW.component(),
                        button -> {
                            previewOpen = !previewOpen;
                            requestInspectorRefresh();
                        })
                .pos(126, y)
                .size(96, 20)
                .build());
        addRenderableWidget(RFMEditorSwitcher.createForEditor(
                230,
                y,
                110,
                20,
                this,
                this::buildContent
        ));
        addRenderableWidget(Button.builder(
                        RFMTranslations.CANCEL.component(),
                        button -> onClose())
                .pos(348, y)
                .size(70, 20)
                .build());
        if (importable != null) {
            importButton = addRenderableWidget(Button.builder(
                            RFMTranslations.IMPORT_PROGRAM.component(),
                            button -> applyImport())
                    .pos(426, y)
                    .size(150, 20)
                    .build());
        }
        inspectorWidgets.clear();
        requestInspectorRefresh();
        if (!graph.isEmpty() && !edited) {
            frameAll();
        }
    }

    @Override
    public void onClose() {
        openContext.onTryClose(
                edited ? buildContent() : openContext.initialValue(),
                SFMScreenChangeHelpers::popScreen
        );
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean isInGameUi() {
        return true;
    }

    private void regenerate() {
        SfmlGenerator.Generated generated = SfmlGenerator.generateDetailed(graph);
        generatedProgram = generated.text();
        previewLines = generatedProgram.isBlank() ? List.of() : List.of(generatedProgram.split("\n", -1));
        if (generatedProgram.isBlank()) {
            programErrors = List.of();
        } else {
            try {
                programErrors = new ProgramBuilder(generatedProgram).build().metadata().errors().stream()
                        .map(contents -> MutableComponent.create(contents).getString())
                        .toList();
            } catch (Throwable t) {
                RetroFactoryManager.LOGGER.warn("Failed to validate generated program", t);
                programErrors = List.of(RFMTranslations.VALIDATION_FAILED.string(t.getClass().getSimpleName()));
            }
        }
        mapErrorsToNodes(generated.lineOwners());
        collectWarnings();
        updateSaveButton();
    }

    private void updateSaveButton() {
        if (saveButton == null) {
            return;
        }
        boolean ok = programErrors.isEmpty();
        saveButton.active = ok;
        saveButton.setTooltip(ok ? null : Tooltip.create(RFMTranslations.SAVE_BLOCKED.component()));
    }

    private void mapErrorsToNodes(Map<Integer, UUID> lineOwners) {
        nodeErrors.clear();
        errorNodeOrder.clear();
        for (String error : programErrors) {
            Matcher matcher = ERROR_LINE_PATTERN.matcher(error);
            UUID owner = matcher.find() ? lineOwners.get(Integer.parseInt(matcher.group(1))) : null;
            if (owner == null || graph.node(owner) == null) {
                continue;
            }
            nodeErrors.computeIfAbsent(owner, ignored -> new ArrayList<>()).add(error);
            if (!errorNodeOrder.contains(owner)) {
                errorNodeOrder.add(owner);
            }
        }
    }

    /// Problems the parser cannot see because the generated text is still valid SFML.
    private void collectWarnings() {
        nodeWarnings.clear();
        for (BlueprintNode node : graph.nodes()) {
            if (node.settings() instanceof NodeSettings.Io io && io.labels().isEmpty()) {
                nodeWarnings.put(node.id(), RFMTranslations.WARN_NO_LABELS.string());
            } else if (node.settings() instanceof NodeSettings.Timer timer
                    && !timer.seconds() && timer.amount() < 20) {
                nodeWarnings.put(node.id(), RFMTranslations.WARN_FAST_TIMER.string());
            }
        }
    }

    @Nullable
    private UUID ownerOfError(String error) {
        for (Map.Entry<UUID, List<String>> entry : nodeErrors.entrySet()) {
            if (entry.getValue().contains(error)) {
                return entry.getKey();
            }
        }
        return null;
    }

    private void focusNode(UUID nodeId) {
        BlueprintNode node = graph.node(nodeId);
        if (node == null) {
            return;
        }
        NodeLayout layout = NodeLayout.of(node, font);
        cameraX = node.x() + layout.width() / 2;
        cameraY = node.y() + layout.height() / 2;
        selection.clear();
        selection.add(nodeId);
        requestInspectorRefresh();
    }

    private void markEdited() {
        edited = true;
        pushHistory(coalesceKey);
        regenerate();
        refreshUnsavedFlag();
    }

    private void refreshUnsavedFlag() {
        try {
            unsavedChanges = !buildContent().equals(openContext.initialValue());
        } catch (Throwable t) {
            unsavedChanges = edited;
        }
    }

    private void pushHistory(@Nullable String key) {
        if (key == null || !key.equals(lastHistoryKey)) {
            undoStack.push(previousState);
            while (undoStack.size() > MAX_HISTORY) {
                undoStack.removeLast();
            }
            redoStack.clear();
        }
        lastHistoryKey = key;
        previousState = graph.copy();
    }

    private void editing(String key, Runnable mutation) {
        String outer = coalesceKey;
        coalesceKey = key;
        try {
            mutation.run();
        } finally {
            coalesceKey = outer;
        }
    }

    private void undo() {
        if (dragMode != DragMode.NONE) {
            return;
        }
        if (undoStack.isEmpty()) {
            setStatus(RFMTranslations.NOTHING_TO_UNDO.key());
            return;
        }
        redoStack.push(graph.copy());
        graph.restoreFrom(undoStack.pop());
        afterHistoryJump();
    }

    private void redo() {
        if (dragMode != DragMode.NONE) {
            return;
        }
        if (redoStack.isEmpty()) {
            setStatus(RFMTranslations.NOTHING_TO_REDO.key());
            return;
        }
        undoStack.push(graph.copy());
        graph.restoreFrom(redoStack.pop());
        afterHistoryJump();
    }

    private void afterHistoryJump() {
        edited = true;
        refreshUnsavedFlag();
        previousState = graph.copy();
        lastHistoryKey = null;
        selection.removeIf(id -> graph.node(id) == null);
        nodeDragOffsets.clear();
        wireFromNode = null;
        wireFromPin = null;
        clearSuggestions();
        setFocused(null);
        requestInspectorRefresh();
        regenerate();
    }

    private String buildContent() {
        if (graph.isEmpty()) {
            return "";
        }
        String program = generatedProgram;
        String embedded = GraphSerde.embed(graph);
        return program.isBlank() ? embedded : program + "\n\n" + embedded;
    }

    private void setStatus(String translationKey) {
        setStatusMessage(Component.translatable(translationKey));
    }

    private void setStatusMessage(Component message) {
        statusMessage = message;
        statusExpiresAt = System.currentTimeMillis() + 6000;
    }

    private double canvasToScreenX(double canvasX) {
        return (canvasX - cameraX) * zoom + width / 2.0D;
    }

    private double canvasToScreenY(double canvasY) {
        return (canvasY - cameraY) * zoom + height / 2.0D;
    }

    private double screenToCanvasX(double screenX) {
        return (screenX - width / 2.0D) / zoom + cameraX;
    }

    private double screenToCanvasY(double screenY) {
        return (screenY - height / 2.0D) / zoom + cameraY;
    }

    private void frameAll() {
        if (graph.isEmpty()) {
            cameraX = 0;
            cameraY = 0;
            zoom = 1.0D;
            return;
        }
        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE;
        double maxY = -Double.MAX_VALUE;
        for (BlueprintNode node : graph.nodes()) {
            NodeLayout layout = NodeLayout.of(node, font);
            minX = Math.min(minX, node.x());
            minY = Math.min(minY, node.y());
            maxX = Math.max(maxX, node.x() + layout.width());
            maxY = Math.max(maxY, node.y() + layout.height());
        }
        cameraX = (minX + maxX) / 2.0D;
        cameraY = (minY + maxY) / 2.0D;
        double contentWidth = Math.max(maxX - minX, 1);
        double contentHeight = Math.max(maxY - minY, 1);
        zoom = Mth.clamp(Math.min(width / (contentWidth + 80), height / (contentHeight + 80)), MIN_ZOOM, 1.5D);
    }

    private List<BlueprintNode> nodesTopFirst() {
        List<BlueprintNode> ordered = new ArrayList<>(graph.nodes());
        java.util.Collections.reverse(ordered);
        return ordered;
    }

    @Nullable
    private PinHit findPin(double mouseX, double mouseY) {
        double hitRadius = Math.max(6, 5 * zoom);
        for (BlueprintNode node : nodesTopFirst()) {
            NodeLayout layout = NodeLayout.of(node, font);
            for (NodeLayout.PinSpot pin : layout.pins()) {
                double px = canvasToScreenX(node.x() + pin.dx());
                double py = canvasToScreenY(node.y() + pin.dy());
                if (Math.abs(mouseX - px) <= hitRadius && Math.abs(mouseY - py) <= hitRadius) {
                    return new PinHit(node.id(), pin.role());
                }
            }
        }
        return null;
    }

    @Nullable
    private BlueprintNode findNode(double mouseX, double mouseY) {
        double canvasX = screenToCanvasX(mouseX);
        double canvasY = screenToCanvasY(mouseY);
        for (BlueprintNode node : nodesTopFirst()) {
            if (NodeLayout.of(node, font).contains(node, canvasX, canvasY)) {
                return node;
            }
        }
        return null;
    }

    @Nullable
    private BlueprintNode singleSelectedNode() {
        if (selection.size() != 1) {
            return null;
        }
        return graph.node(selection.iterator().next());
    }

    private boolean dockVisible() {
        return previewOpen || singleSelectedNode() != null;
    }

    private int dockX() {
        return width - DOCK_WIDTH;
    }

    private boolean inDock(double mouseX) {
        return dockVisible() && mouseX >= dockX();
    }

    private void requestInspectorRefresh() {
        needsInspectorRefresh = true;
    }

    private int paletteHeight() {
        return PALETTE_HEADER_HEIGHT + paletteTypes().size() * PALETTE_ROW_HEIGHT + 4;
    }

    private boolean inPalette(double mouseX, double mouseY) {
        return paletteOpen
                && mouseX >= paletteX && mouseX <= paletteX + PALETTE_WIDTH
                && mouseY >= paletteY && mouseY <= paletteY + paletteHeight();
    }

    private double snap(double value) {
        return snapToGrid ? Math.round(value / SNAP_STEP) * SNAP_STEP : value;
    }

    private List<NodeType> paletteTypes() {
        if (pendingWireNode == null) {
            return List.of(NodeType.values());
        }
        // Dropped from an output pin: only nodes that can be entered; from an input pin: only nodes that can lead in.
        return java.util.Arrays.stream(NodeType.values())
                .filter(type -> pendingWirePin.isOutput()
                        ? type.hasPin(PinRole.EXEC_IN)
                        : !type.outputPins().isEmpty())
                .toList();
    }

    @Nullable
    private Clipboard fragmentFromSelection() {
        if (selection.isEmpty()) {
            return null;
        }
        List<BlueprintNode> nodes = new ArrayList<>();
        for (UUID id : selection) {
            BlueprintNode node = graph.node(id);
            if (node != null) {
                nodes.add(new BlueprintNode(node.id(), node.x(), node.y(), node.settings()));
            }
        }
        if (nodes.isEmpty()) {
            return null;
        }
        List<BlueprintEdge> edges = graph.edges().stream()
                .filter(edge -> selection.contains(edge.fromNode()) && selection.contains(edge.toNode()))
                .toList();
        return new Clipboard(nodes, List.copyOf(edges));
    }

    private void copySelection() {
        Clipboard fragment = fragmentFromSelection();
        if (fragment != null) {
            clipboard = fragment;
            setStatusMessage(RFMTranslations.COPIED.component(fragment.nodes().size()));
        }
    }

    private void duplicateSelection() {
        Clipboard fragment = fragmentFromSelection();
        if (fragment != null) {
            paste(fragment, 0, 0, false);
        }
    }

    private void pasteClipboard(double canvasX, double canvasY) {
        paste(clipboard, canvasX, canvasY, true);
    }

    private void paste(@Nullable Clipboard fragment, double canvasX, double canvasY, boolean atCursor) {
        if (fragment == null || fragment.nodes().isEmpty()) {
            return;
        }
        double minX = fragment.nodes().stream().mapToDouble(BlueprintNode::x).min().orElse(0);
        double minY = fragment.nodes().stream().mapToDouble(BlueprintNode::y).min().orElse(0);
        double offsetX = atCursor ? canvasX - minX : 24;
        double offsetY = atCursor ? canvasY - minY : 24;
        Map<UUID, UUID> remap = new HashMap<>();
        selection.clear();
        for (BlueprintNode source : fragment.nodes()) {
            BlueprintNode copy = new BlueprintNode(
                    UUID.randomUUID(),
                    snap(source.x() + offsetX),
                    snap(source.y() + offsetY),
                    source.settings()
            );
            remap.put(source.id(), copy.id());
            graph.addNode(copy);
            selection.add(copy.id());
        }
        for (BlueprintEdge edge : fragment.edges()) {
            UUID from = remap.get(edge.fromNode());
            UUID to = remap.get(edge.toNode());
            if (from != null && to != null) {
                graph.connect(from, edge.fromPin(), to);
            }
        }
        markEdited();
        requestInspectorRefresh();
        setStatusMessage(RFMTranslations.PASTED.component(fragment.nodes().size()));
    }

    @Nullable
    private BlueprintEdge findWire(double mouseX, double mouseY) {
        BlueprintEdge best = null;
        double bestDistance = WIRE_HIT_DISTANCE;
        for (BlueprintEdge edge : graph.edges()) {
            BlueprintNode from = graph.node(edge.fromNode());
            BlueprintNode to = graph.node(edge.toNode());
            if (from == null || to == null) {
                continue;
            }
            double[] start = pinScreenPosition(from, edge.fromPin());
            double[] end = pinScreenPosition(to, edge.toPin());
            if (start == null || end == null) {
                continue;
            }
            double distance = distanceToWire(start[0], start[1], end[0], end[1], mouseX, mouseY);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = edge;
            }
        }
        return best;
    }

    private double distanceToWire(double x0, double y0, double x1, double y1, double px, double py) {
        double tangent = wireTangent(x0, x1);
        double c0x = x0 + tangent;
        double c1x = x1 - tangent;
        double best = Double.MAX_VALUE;
        int steps = 24;
        double previousX = x0;
        double previousY = y0;
        for (int i = 1; i <= steps; i++) {
            double t = (double) i / steps;
            double omt = 1 - t;
            double bx = omt * omt * omt * x0 + 3 * omt * omt * t * c0x + 3 * omt * t * t * c1x + t * t * t * x1;
            double by = omt * omt * omt * y0 + 3 * omt * omt * t * y0 + 3 * omt * t * t * y1 + t * t * t * y1;
            best = Math.min(best, distanceToSegment(previousX, previousY, bx, by, px, py));
            previousX = bx;
            previousY = by;
        }
        return best;
    }

    private static double distanceToSegment(double x0, double y0, double x1, double y1, double px, double py) {
        double dx = x1 - x0;
        double dy = y1 - y0;
        double lengthSquared = dx * dx + dy * dy;
        if (lengthSquared == 0) {
            return Math.hypot(px - x0, py - y0);
        }
        double t = Mth.clamp(((px - x0) * dx + (py - y0) * dy) / lengthSquared, 0, 1);
        return Math.hypot(px - (x0 + t * dx), py - (y0 + t * dy));
    }

    private double wireTangent(double x0, double x1) {
        return Math.max(30 * zoom, Math.min(120 * zoom, Math.abs(x1 - x0) * 0.45D));
    }

    private void openPalette(double mouseX, double mouseY) {
        paletteCanvasX = screenToCanvasX(mouseX);
        paletteCanvasY = screenToCanvasY(mouseY);
        paletteX = (int) Math.min(mouseX, width - PALETTE_WIDTH - 4);
        paletteY = (int) Math.min(mouseY, height - paletteHeight() - 4);
        paletteOpen = true;
    }

    private boolean supportsLabels(NodeType type) {
        return type == NodeType.INPUT || type == NodeType.OUTPUT || type == NodeType.FORGET;
    }

    private List<String> nodeLabels(NodeSettings settings) {
        return switch (settings) {
            case NodeSettings.Input input -> input.labels();
            case NodeSettings.Output output -> output.labels();
            case NodeSettings.Forget forget -> forget.labels();
            default -> List.of();
        };
    }

    private void setNodeLabels(BlueprintNode node, List<String> labels) {
        switch (node.settings()) {
            case NodeSettings.Io io -> node.setSettings(io.withLabels(labels));
            case NodeSettings.Forget ignored -> node.setSettings(new NodeSettings.Forget(labels));
            default -> {
            }
        }
        markEdited();
    }

    private void mutateIo(java.util.function.UnaryOperator<NodeSettings.Io> update) {
        BlueprintNode current = singleSelectedNode();
        if (current != null && current.settings() instanceof NodeSettings.Io io) {
            NodeSettings updated = update.apply(io);
            if (!updated.equals(io)) {
                current.setSettings(updated);
                markEdited();
            }
        }
    }

    private record LabelInfo(int diskBlocks, int gunBlocks) {
        int totalBlocks() {
            return Math.max(diskBlocks, gunBlocks);
        }

        boolean gunOnly() {
            return diskBlocks == 0 && gunBlocks > 0;
        }
    }

    private Map<String, LabelInfo> worldLabelInfo() {
        Map<String, LabelInfo> info = new TreeMap<>();
        openContext.labelPositionHolder().labels().forEach((label, positions) ->
                info.put(label, new LabelInfo(positions.size(), 0)));
        Player player = minecraft == null ? null : minecraft.player;
        if (player != null) {
            Inventory inventory = player.getInventory();
            for (int i = 0; i < inventory.getContainerSize(); i++) {
                ItemStack stack = inventory.getItem(i);
                if (stack.getItem() instanceof LabelGunItem) {
                    LabelPositionHolder.from(stack).labels().forEach((label, positions) -> {
                        LabelInfo existing = info.getOrDefault(label, new LabelInfo(0, 0));
                        info.put(label, new LabelInfo(existing.diskBlocks(), Math.max(existing.gunBlocks(), positions.size())));
                    });
                }
            }
        }
        return info;
    }

    private Set<String> worldLabels() {
        return new TreeSet<>(worldLabelInfo().keySet());
    }

    private List<String> knownLabels(BlueprintNode node) {
        Set<String> labels = worldLabels();
        labels.addAll(nodeLabels(node.settings()));
        return new ArrayList<>(labels);
    }

    private void toggleLabel(BlueprintNode node, String label) {
        List<String> labels = new ArrayList<>(nodeLabels(node.settings()));
        if (!labels.remove(label)) {
            labels.add(label);
        }
        setNodeLabels(node, labels);
    }

    private void addCustomLabel() {
        BlueprintNode node = singleSelectedNode();
        if (node == null || customLabelField == null) {
            return;
        }
        String label = customLabelField.getValue().trim();
        if (label.isEmpty()) {
            return;
        }
        List<String> labels = new ArrayList<>(nodeLabels(node.settings()));
        if (!labels.contains(label)) {
            labels.add(label);
            setNodeLabels(node, labels);
        }
        customLabelText = "";
        customLabelField.setValue("");
        requestInspectorRefresh();
    }

    private void rebuildInspector() {
        for (AbstractWidget widget : inspectorWidgets) {
            removeWidget(widget);
        }
        inspectorWidgets.clear();
        inspectorWidgetBaseY.clear();
        customLabelField = null;
        resourceAddFields.clear();
        tagAddFields.clear();
        exceptAddField = null;
        clearSuggestions();
        inspectorBottom = 0;
        labelRowsStartY = 0;
        BlueprintNode node = singleSelectedNode();
        if (!inspectedNodeId(node)) {
            labelScroll = 0;
            dockScroll = 0;
        }
        inspectedNodeId = node == null ? null : node.id();
        if (!dockVisible()) {
            return;
        }
        int x = dockX() + 8;
        if (node == null) {
            EditBox nameField = new EditBox(font, x, 36, DOCK_WIDTH - 16, 16, RFMTranslations.PROGRAM_NAME.component());
            nameField.setMaxLength(64);
            nameField.setValue(graph.name());
            nameField.setResponder(value -> editing("name", () -> {
                if (!value.equals(graph.name())) {
                    graph.setName(value);
                    markEdited();
                }
            }));
            addInspectorWidget(nameField);
            inspectorBottom = 60;
            dockScroll = Mth.clamp(dockScroll, 0, maxDockScroll());
            applyDockScroll();
            return;
        }
        switch (node.settings()) {
            case NodeSettings.Timer timer -> buildTimerInspector(node, timer, x);
            case NodeSettings.RedstonePulse ignored -> inspectorBottom = 44;
            case NodeSettings.Io io -> buildIoInspector(node, io, x);
            case NodeSettings.Forget ignored -> buildLabelPicker(node, 38);
            case NodeSettings.If ifSettings -> buildIfInspector(ifSettings, x);
            case NodeSettings.Comment comment -> {
                EditBox textField = new EditBox(font, x, 36, DOCK_WIDTH - 16, 16, RFMTranslations.COMMENT_TEXT.component());
                textField.setMaxLength(1000);
                textField.setValue(comment.text());
                textField.setResponder(value -> editing("comment:" + node.id(), () -> {
                    BlueprintNode current = singleSelectedNode();
                    if (current != null && current.settings() instanceof NodeSettings.Comment existing
                            && !value.equals(existing.text())) {
                        current.setSettings(new NodeSettings.Comment(value));
                        markEdited();
                    }
                }));
                addInspectorWidget(textField);
                inspectorBottom = 64;
            }
        }
        dockScroll = Mth.clamp(dockScroll, 0, maxDockScroll());
        applyDockScroll();
    }

    private boolean inspectedNodeId(@Nullable BlueprintNode node) {
        UUID id = node == null ? null : node.id();
        return id == null ? inspectedNodeId == null : id.equals(inspectedNodeId);
    }

    private record LimitGroup(
            int headerY,
            int qtyY,
            int itemRowsY,
            int shownItemRows,
            boolean moreItemsLine,
            int itemAddY,
            int tagModeY,
            int tagRowsY,
            int shownTagRows,
            int tagAddY
    ) {
    }

    private record IoSection(
            List<LimitGroup> groups,
            int addFilterY,
            int exceptHeaderY,
            int exceptRowsY,
            int shownExceptRows,
            int exceptAddY,
            int roundRobinY,
            int sidesHeaderY,
            int chipsY,
            int slotsY,
            int labelsHeaderY
    ) {
        static IoSection of(NodeSettings.Io io) {
            List<LimitEntry> entries = io.limits().isEmpty() ? List.of(LimitEntry.EMPTY) : io.limits();
            List<LimitGroup> groups = new ArrayList<>();
            int y = 44;
            for (LimitEntry entry : entries) {
                int headerY = y;
                int qtyY = headerY + 12;
                int itemRowsY = qtyY + 20;
                int shown = Math.min(entry.resources().size(), MAX_VISIBLE_RESOURCE_ROWS);
                boolean more = entry.resources().size() > MAX_VISIBLE_RESOURCE_ROWS;
                int itemAddY = itemRowsY + shown * LABEL_ROW_HEIGHT + (more ? 11 : 0);
                int tagModeY = itemAddY + 30;
                int tagRowsY = tagModeY + 20;
                int shownTags = Math.min(entry.tags().size(), MAX_VISIBLE_RESOURCE_ROWS);
                int tagAddY = tagRowsY + shownTags * LABEL_ROW_HEIGHT;
                groups.add(new LimitGroup(headerY, qtyY, itemRowsY, shown, more, itemAddY,
                        tagModeY, tagRowsY, shownTags, tagAddY));
                y = tagAddY + 22;
            }
            int addFilterY = y;
            int exceptHeaderY = addFilterY + 24;
            int exceptRowsY = exceptHeaderY + 12;
            int shownExcept = Math.min(io.except().size(), MAX_VISIBLE_RESOURCE_ROWS);
            int exceptAddY = exceptRowsY + shownExcept * LABEL_ROW_HEIGHT;
            int roundRobinY = exceptAddY + 22;
            int sidesHeaderY = roundRobinY + 22;
            int chipsY = sidesHeaderY + 12;
            int slotsY = chipsY + 46;
            int labelsHeaderY = slotsY + 22;
            return new IoSection(groups, addFilterY, exceptHeaderY, exceptRowsY, shownExcept, exceptAddY,
                    roundRobinY, sidesHeaderY, chipsY, slotsY, labelsHeaderY);
        }
    }

    private int[] sideChipRect(int chipIndex, int chipsY) {
        int row = chipIndex / 5;
        int col = chipIndex % 5;
        return new int[]{dockX() + 8 + col * 40, chipsY + row * 14, 38, 12};
    }

    private void buildIoInspector(BlueprintNode node, NodeSettings.Io io, int x) {
        IoSection section = IoSection.of(io);
        addToggleButton(x, 26, 96, RFMTranslations.EACH.component(onOffText(io.each())), () -> {
            mutateIo(current -> current.withEach(!current.each()));
            requestInspectorRefresh();
        });
        if (io instanceof NodeSettings.Output output) {
            addToggleButton(x + 100, 26, 104, RFMTranslations.EMPTY_ONLY.component(onOffText(output.emptySlotsOnly())), () -> {
                mutateIo(current -> current instanceof NodeSettings.Output out
                        ? out.withEmptySlotsOnly(!out.emptySlotsOnly())
                        : current);
                requestInspectorRefresh();
            });
        }
        resourceAddFields.clear();
        for (int i = 0; i < section.groups().size(); i++) {
            buildLimitGroup(node, io, section.groups().get(i), i, section.groups().size(), x);
        }
        EditBox exceptField = new EditBox(font, x, section.exceptAddY(), DOCK_WIDTH - 72, 16,
                RFMTranslations.EXCEPT_HEADER.component());
        exceptField.setMaxLength(256);
        setPlaceholder(exceptField, RFMTranslations.EXCEPT_PLACEHOLDER.string(),
                value -> updateSuggestions(exceptField, value, -1, EXCEPT_TARGET));
        addInspectorWidget(exceptField);
        exceptAddField = exceptField;
        addInspectorWidget(Button.builder(RFMTranslations.ADD.component(), button -> addExceptEntry())
                .pos(x + DOCK_WIDTH - 66, section.exceptAddY())
                .size(50, 16)
                .build());
        addToggleButton(x, section.roundRobinY(), 190,
                RFMTranslations.ROUND_ROBIN.component(
                        Component.translatable(io.roundRobin().translationKey())),
                () -> {
                    mutateIo(current -> current.withRoundRobin(current.roundRobin().next()));
                    requestInspectorRefresh();
                });
        Button addFilterButton = Button.builder(RFMTranslations.ADD_FILTER.component(), button -> {
                    mutateIo(NodeSettings.Io::withLimitAdded);
                    requestInspectorRefresh();
                })
                .pos(x, section.addFilterY())
                .size(110, 16)
                .build();
        addFilterButton.active = section.groups().size() < MAX_LIMIT_GROUPS;
        addInspectorWidget(addFilterButton);

        EditBox slotsField = new EditBox(font, x + 34, section.slotsY(), 116, 16, RFMTranslations.SLOTS.component());
        slotsField.setMaxLength(64);
        slotsField.setFilter(value -> value.matches("[0-9,\\- ]*"));
        slotsField.setValue(io.slots());
        setPlaceholder(slotsField, "0,2-8", value -> editing("slots:" + node.id(),
                () -> mutateIo(current -> current.withSlots(value.trim()))));
        addInspectorWidget(slotsField);

        buildLabelPicker(node, section.labelsHeaderY());
    }

    private void buildLimitGroup(
            BlueprintNode node,
            NodeSettings.Io io,
            LimitGroup group,
            int index,
            int groupCount,
            int x
    ) {
        LimitEntry entry = io.limitAt(index);
        if (groupCount > 1) {
            addInspectorWidget(Button.builder(Component.literal("x"), button -> {
                        mutateIo(current -> current.withLimitRemoved(index));
                        requestInspectorRefresh();
                    })
                    .pos(x + DOCK_WIDTH - 32, group.headerY() - 2)
                    .size(16, 14)
                    .build());
        }
        EditBox quantityField = new EditBox(font, x + 24, group.qtyY(), 40, 16, RFMTranslations.QTY.component());
        quantityField.setFilter(value -> value.isEmpty() || value.matches("\\d{1,7}"));
        quantityField.setValue(entry.quantity() < 0 ? "" : String.valueOf(entry.quantity()));
        quantityField.setResponder(value -> editing("qty:" + node.id() + ":" + index, () -> mutateIo(current ->
                current.withLimitAt(index, current.limitAt(index)
                        .withQuantity(value.isEmpty() ? -1 : parseIntOr(value, current.limitAt(index).quantity()))))));
        addInspectorWidget(quantityField);
        EditBox retainField = new EditBox(font, x + 98, group.qtyY(), 40, 16, RFMTranslations.KEEP.component());
        retainField.setFilter(value -> value.isEmpty() || value.matches("\\d{1,7}"));
        retainField.setValue(entry.retain() < 0 ? "" : String.valueOf(entry.retain()));
        retainField.setResponder(value -> editing("retain:" + node.id() + ":" + index, () -> mutateIo(current ->
                current.withLimitAt(index, current.limitAt(index)
                        .withRetain(value.isEmpty() ? -1 : parseIntOr(value, current.limitAt(index).retain()))))));
        addInspectorWidget(retainField);

        EditBox addField = new EditBox(font, x, group.itemAddY(), DOCK_WIDTH - 72, 16, RFMTranslations.NEW_ITEM.component());
        addField.setMaxLength(256);
        String pending = index == customResourceGroup ? customResourceText : "";
        addField.setValue(pending);
        setPlaceholder(addField, "minecraft:iron_ingot", value -> {
            customResourceText = value;
            customResourceGroup = index;
            updateSuggestions(addField, value, -1, index);
        });
        addInspectorWidget(addField);
        resourceAddFields.put(index, addField);
        addInspectorWidget(Button.builder(RFMTranslations.ADD.component(), button -> addCustomResource(index))
                .pos(x + DOCK_WIDTH - 66, group.itemAddY())
                .size(50, 16)
                .build());

        addToggleButton(x, group.tagModeY(), 92,
                RFMTranslations.TAG_MODE.component(Component.translatable(entry.tagMode().translationKey())),
                () -> {
                    mutateIo(current -> current.withLimitAt(index, current.limitAt(index)
                            .withTagMode(current.limitAt(index).tagMode().next())));
                    requestInspectorRefresh();
                });
        if (entry.tags().size() > 1) {
            addToggleButton(x + 96, group.tagModeY(), 92,
                    RFMTranslations.JOIN.component(entry.tagsOr() ? "OR" : "AND"),
                    () -> {
                        mutateIo(current -> current.withLimitAt(index, current.limitAt(index)
                                .withTagsOr(!current.limitAt(index).tagsOr())));
                        requestInspectorRefresh();
                    });
        }
        EditBox tagField = new EditBox(font, x, group.tagAddY(), DOCK_WIDTH - 72, 16,
                RFMTranslations.TAGS_HEADER.component());
        tagField.setMaxLength(256);
        setPlaceholder(tagField, RFMTranslations.TAG_PLACEHOLDER.string(), null);
        addInspectorWidget(tagField);
        tagAddFields.put(index, tagField);
        addInspectorWidget(Button.builder(RFMTranslations.ADD.component(), button -> addTagEntry(index))
                .pos(x + DOCK_WIDTH - 66, group.tagAddY())
                .size(50, 16)
                .build());
    }

    private void addTagEntry(int groupIndex) {
        EditBox field = tagAddFields.get(groupIndex);
        if (field == null) {
            return;
        }
        String tag = field.getValue().trim().replaceFirst("^#", "");
        if (tag.isEmpty()) {
            return;
        }
        mutateIo(current -> current.withLimitAt(groupIndex, current.limitAt(groupIndex).withTagAdded(tag)));
        field.setValue("");
        requestInspectorRefresh();
    }

    private void addExceptEntry() {
        if (exceptAddField == null) {
            return;
        }
        String value = exceptAddField.getValue().trim();
        if (value.isEmpty()) {
            return;
        }
        mutateIo(current -> {
            if (current.except().contains(value)) {
                return current;
            }
            List<String> updated = new ArrayList<>(current.except());
            updated.add(value);
            return current.withExcept(updated);
        });
        exceptAddField.setValue("");
        clearSuggestions();
        requestInspectorRefresh();
    }

    private static final int CONDITION_ROW_HEIGHT = 58;
    private static final int MAX_CONDITION_ROWS = 4;

    private void mutateIf(java.util.function.UnaryOperator<NodeSettings.If> update) {
        BlueprintNode current = singleSelectedNode();
        if (current != null && current.settings() instanceof NodeSettings.If ifSettings) {
            NodeSettings.If updated = update.apply(ifSettings);
            if (!updated.equals(ifSettings)) {
                current.setSettings(updated);
                markEdited();
            }
        }
    }

    private void updateConditionRow(int index, java.util.function.UnaryOperator<ConditionRow> update) {
        mutateIf(ifSettings -> {
            if (index >= ifSettings.rows().size()) {
                return ifSettings;
            }
            List<ConditionRow> rows = new ArrayList<>(ifSettings.rows());
            rows.set(index, update.apply(rows.get(index)));
            return new NodeSettings.If(ifSettings.condition(), rows, ifSettings.joinOr(), ifSettings.useText());
        });
    }

    private void buildIfInspector(NodeSettings.If ifSettings, int x) {
        if (ifSettings.useText()) {
            EditBox conditionField = new EditBox(font, x, 36, DOCK_WIDTH - 16, 16, RFMTranslations.CONDITION_SFML.component());
            conditionField.setMaxLength(500);
            conditionField.setValue(ifSettings.condition());
            conditionField.setResponder(value -> editing("cond_text", () -> mutateIf(current ->
                    new NodeSettings.If(value, current.rows(), current.joinOr(), current.useText()))));
            addInspectorWidget(conditionField);
            addInspectorWidget(Button.builder(RFMTranslations.VISUAL_EDITOR.component(), button -> {
                        mutateIf(current -> new NodeSettings.If(current.condition(), current.rows(), current.joinOr(), false));
                        requestInspectorRefresh();
                    })
                    .pos(x, 58)
                    .size(90, 16)
                    .build());
            inspectorBottom = 80;
            return;
        }
        List<ConditionRow> rows = ifSettings.rows();
        for (int i = 0; i < rows.size(); i++) {
            buildConditionRow(rows.get(i), i, x, 46 + i * CONDITION_ROW_HEIGHT);
        }
        int controlsY = 46 + rows.size() * CONDITION_ROW_HEIGHT + 2;
        addInspectorWidget(Button.builder(RFMTranslations.JOIN.component(ifSettings.joinOr() ? "OR" : "AND"), button -> {
                    mutateIf(current -> new NodeSettings.If(current.condition(), current.rows(), !current.joinOr(), false));
                    requestInspectorRefresh();
                })
                .pos(x, controlsY)
                .size(64, 16)
                .build());
        Button addRowButton = Button.builder(RFMTranslations.ADD_CONDITION.component(), button -> {
                    mutateIf(current -> {
                        List<ConditionRow> updated = new ArrayList<>(current.rows());
                        updated.add(ConditionRow.DEFAULT_LABEL);
                        return new NodeSettings.If(current.condition(), updated, current.joinOr(), false);
                    });
                    requestInspectorRefresh();
                })
                .pos(x + 68, controlsY)
                .size(56, 16)
                .build();
        addRowButton.active = rows.size() < MAX_CONDITION_ROWS;
        addInspectorWidget(addRowButton);
        addInspectorWidget(Button.builder(RFMTranslations.TEXT_MODE.component(), button -> {
                    mutateIf(current -> new NodeSettings.If(
                            current.condition().isBlank() ? current.conditionSfml() : current.condition(),
                            current.rows(),
                            current.joinOr(),
                            true
                    ));
                    requestInspectorRefresh();
                })
                .pos(x + 128, controlsY)
                .size(76, 16)
                .build());
        inspectorBottom = controlsY + 22;
    }

    private void buildConditionRow(ConditionRow row, int index, int x, int rowY) {
        addInspectorWidget(Button.builder((row.redstone() ? RFMTranslations.ROW_REDSTONE : RFMTranslations.ROW_LABEL).component(), button -> {
                    updateConditionRow(index, current -> new ConditionRow(!current.redstone(), current.negate(),
                            current.setOp(), current.label(), current.comparison(), current.amount(), current.resource()));
                    requestInspectorRefresh();
                })
                .pos(x, rowY)
                .size(56, 16)
                .build());
        addInspectorWidget(Button.builder(RFMTranslations.NOT.component(onOffText(row.negate())), button -> {
                    updateConditionRow(index, current -> new ConditionRow(current.redstone(), !current.negate(),
                            current.setOp(), current.label(), current.comparison(), current.amount(), current.resource()));
                    requestInspectorRefresh();
                })
                .pos(x + 60, rowY)
                .size(56, 16)
                .build());
        if (!row.redstone()) {
            addInspectorWidget(Button.builder(row.setOp().isEmpty() ? RFMTranslations.SET_ANY.component() : Component.literal(row.setOp()), button -> {
                        updateConditionRow(index, current -> {
                            int next = (ConditionRow.SET_OPS.indexOf(current.setOp()) + 1) % ConditionRow.SET_OPS.size();
                            return new ConditionRow(current.redstone(), current.negate(), ConditionRow.SET_OPS.get(next),
                                    current.label(), current.comparison(), current.amount(), current.resource());
                        });
                        requestInspectorRefresh();
                    })
                    .pos(x + 120, rowY)
                    .size(62, 16)
                    .build());
        }
        addInspectorWidget(Button.builder(Component.literal("x"), button -> {
                    mutateIf(current -> {
                        List<ConditionRow> updated = new ArrayList<>(current.rows());
                        if (index < updated.size()) {
                            updated.remove(index);
                        }
                        return new NodeSettings.If(current.condition(), updated, current.joinOr(), false);
                    });
                    requestInspectorRefresh();
                })
                .pos(x + 188, rowY)
                .size(16, 16)
                .build());

        int rowB = rowY + 19;
        int cmpX = row.redstone() ? x : x + 104;
        if (!row.redstone()) {
            EditBox labelField = new EditBox(font, x, rowB, 100, 16, RFMTranslations.ROW_LABEL.component());
            labelField.setMaxLength(256);
            labelField.setValue(row.label());
            setPlaceholder(labelField, RFMTranslations.LABEL_PLACEHOLDER.string(),
                    value -> editing("cond_label:" + index,
                            () -> updateConditionRow(index, current -> new ConditionRow(current.redstone(),
                                    current.negate(), current.setOp(), value, current.comparison(), current.amount(), current.resource()))));
            addInspectorWidget(labelField);
        }
        addInspectorWidget(Button.builder(Component.literal(row.comparison()), button -> {
                    updateConditionRow(index, current -> {
                        int next = (ConditionRow.COMPARISONS.indexOf(current.comparison()) + 1) % ConditionRow.COMPARISONS.size();
                        return new ConditionRow(current.redstone(), current.negate(), current.setOp(),
                                current.label(), ConditionRow.COMPARISONS.get(next), current.amount(), current.resource());
                    });
                    requestInspectorRefresh();
                })
                .pos(cmpX, rowB)
                .size(32, 16)
                .build());
        EditBox amountField = new EditBox(font, cmpX + 36, rowB, 48, 16, RFMTranslations.AMOUNT.component());
        amountField.setFilter(value -> value.isEmpty() || value.matches("\\d{1,9}"));
        amountField.setValue(String.valueOf(row.amount()));
        amountField.setResponder(value -> editing("cond_amount:" + index,
                () -> updateConditionRow(index, current -> new ConditionRow(current.redstone(),
                        current.negate(), current.setOp(), current.label(), current.comparison(),
                        parseIntOr(value, current.amount()), current.resource()))));
        addInspectorWidget(amountField);
        if (!row.redstone()) {
            int rowC = rowY + 38;
            EditBox resourceField = new EditBox(font, x, rowC, DOCK_WIDTH - 16, 16, RFMTranslations.RESOURCE.component());
            resourceField.setMaxLength(256);
            resourceField.setValue(row.resource());
            setPlaceholder(resourceField, RFMTranslations.ITEM_FILTER_OPTIONAL.string(), value -> {
                editing("cond_res:" + index, () -> updateConditionRow(index, current -> new ConditionRow(current.redstone(),
                        current.negate(), current.setOp(), current.label(), current.comparison(), current.amount(), value)));
                updateSuggestions(resourceField, value, index);
            });
            addInspectorWidget(resourceField);
        }
    }

    private static final int SUGGESTION_ROW_HEIGHT = 20;
    private static final int MAX_SUGGESTIONS = 8;

    private static List<ItemEntry> itemIndex() {
        if (itemIndex == null) {
            List<ItemEntry> entries = new ArrayList<>();
            for (Item item : BuiltInRegistries.ITEM) {
                if (item == Items.AIR) {
                    continue;
                }
                Identifier key = BuiltInRegistries.ITEM.getKey(item);
                ItemStack stack = new ItemStack(item);
                addEntry(entries, key.toString(), stack.getHoverName().getString(), stack);
            }
            addNonItemResources(entries);
            itemIndex = entries;
        }
        return itemIndex;
    }

    private static void addEntry(List<ItemEntry> entries, String id, String name, @Nullable ItemStack icon) {
        entries.add(new ItemEntry(id, name, (id + " " + name).toLowerCase(Locale.ROOT), icon));
    }

    /// Fluids, chemicals and energy come from SFM's resource-type registry so any mod's types are covered.
    private static void addNonItemResources(List<ItemEntry> entries) {
        SFMRegistryWrapper<ResourceType<?, ?, ?>> registry;
        try {
            registry = SFMResourceTypes.registry();
        } catch (Throwable t) {
            RetroFactoryManager.LOGGER.warn("SFM resource types unavailable for autocomplete", t);
            return;
        }
        for (ResourceType<?, ?, ?> type : registry) {
            try {
                Identifier typeId = registry.getId(type);
                if (typeId == null
                        || typeId.getPath().equals("item")
                        || !(type instanceof RegistryBackedResourceType<?, ?, ?> backed)) {
                    continue;
                }
                String prefix = (typeId.getNamespace().equals(SFM_NAMESPACE) ? "" : typeId.getNamespace() + ":")
                        + typeId.getPath() + ":";
                for (Identifier key : backed.getRegistryKeys()) {
                    String id = prefix + key.getNamespace() + ":" + key.getPath();
                    ItemStack icon = null;
                    try {
                        icon = iconFor(backed.getItemFromRegistryKey(key));
                    } catch (Throwable ignored) {
                        // An entry that will not resolve is still usable as a plain id.
                    }
                    String name = icon != null && !icon.isEmpty()
                            ? icon.getHoverName().getString()
                            : prettify(key.getPath());
                    addEntry(entries, id, name, icon);
                }
            } catch (Throwable t) {
                RetroFactoryManager.LOGGER.warn("Skipping resource type during autocomplete indexing", t);
            }
        }
        addEntry(entries, "fe::", RFMTranslations.FORGE_ENERGY.string(), null);
    }

    @Nullable
    private static ItemStack iconFor(@Nullable Object resource) {
        if (resource instanceof Item item) {
            return new ItemStack(item);
        }
        if (resource instanceof Fluid fluid) {
            Item bucket = fluid.getBucket();
            return bucket == Items.AIR ? null : new ItemStack(bucket);
        }
        return null;
    }

    private static String prettify(String path) {
        String[] parts = path.split("[_/]");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            if (!sb.isEmpty()) {
                sb.append(' ');
            }
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return sb.isEmpty() ? path : sb.toString();
    }

    private void updateSuggestions(EditBox anchor, String value, int conditionRow) {
        updateSuggestions(anchor, value, conditionRow, -1);
    }

    private void updateSuggestions(EditBox anchor, String value, int conditionRow, int limitGroup) {
        suggestionLimitGroup = limitGroup;
        String query = value.trim().toLowerCase(java.util.Locale.ROOT);
        if (query.length() < 2) {
            clearSuggestions();
            return;
        }
        List<ItemEntry> starts = new ArrayList<>();
        List<ItemEntry> contains = new ArrayList<>();
        for (ItemEntry entry : itemIndex()) {
            if (starts.size() >= MAX_SUGGESTIONS) {
                break;
            }
            String path = entry.id().substring(entry.id().lastIndexOf(':') + 1);
            if (path.startsWith(query) || entry.name().toLowerCase(Locale.ROOT).startsWith(query)) {
                starts.add(entry);
            } else if (contains.size() < MAX_SUGGESTIONS && entry.searchText().contains(query)) {
                contains.add(entry);
            }
        }
        List<ItemEntry> merged = new ArrayList<>(starts);
        for (ItemEntry entry : contains) {
            if (merged.size() >= MAX_SUGGESTIONS) {
                break;
            }
            merged.add(entry);
        }
        suggestions = merged;
        suggestionConditionRow = conditionRow;
        suggestionX = dockX() + 8;
        int below = anchor.getY() + 18;
        int popupHeight = suggestions.size() * SUGGESTION_ROW_HEIGHT + 2;
        suggestionsAbove = below + popupHeight > height - 4 && anchor.getY() - popupHeight > 4;
        suggestionY = suggestionsAbove ? anchor.getY() - popupHeight - 2 : below;
    }

    private void clearSuggestions() {
        suggestions = List.of();
        suggestionConditionRow = -1;
        suggestionLimitGroup = -1;
    }

    private boolean inSuggestions(double mouseX, double mouseY) {
        return !suggestions.isEmpty()
                && mouseX >= suggestionX && mouseX <= suggestionX + DOCK_WIDTH - 16
                && mouseY >= suggestionY && mouseY <= suggestionY + suggestions.size() * SUGGESTION_ROW_HEIGHT + 2;
    }

    private void applySuggestion(double mouseY) {
        int index = (int) ((mouseY - suggestionY - 1) / SUGGESTION_ROW_HEIGHT);
        if (index < 0 || index >= suggestions.size()) {
            return;
        }
        String id = suggestions.get(index).id();
        int conditionRow = suggestionConditionRow;
        int limitGroup = suggestionLimitGroup;
        clearSuggestions();
        if (limitGroup == EXCEPT_TARGET) {
            mutateIo(current -> {
                if (current.except().contains(id)) {
                    return current;
                }
                List<String> updated = new ArrayList<>(current.except());
                updated.add(id);
                return current.withExcept(updated);
            });
            requestInspectorRefresh();
        } else if (conditionRow < 0) {
            mutateIo(current -> current.withLimitAt(
                    Math.max(limitGroup, 0),
                    current.limitAt(Math.max(limitGroup, 0)).withResourceAdded(id)
            ));
            customResourceText = "";
            customResourceGroup = -1;
            requestInspectorRefresh();
        } else {
            updateConditionRow(conditionRow, current -> new ConditionRow(current.redstone(), current.negate(),
                    current.setOp(), current.label(), current.comparison(), current.amount(), id));
            requestInspectorRefresh();
        }
    }

    private static void setPlaceholder(EditBox field, String placeholder, @Nullable Consumer<String> responder) {
        field.setSuggestion(field.getValue().isEmpty() ? placeholder : "");
        field.setResponder(value -> {
            field.setSuggestion(value.isEmpty() ? placeholder : "");
            if (responder != null) {
                responder.accept(value);
            }
        });
    }

    private void renderSuggestions(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (suggestions.isEmpty()) {
            return;
        }
        graphics.nextStratum();
        int x0 = suggestionX;
        int x1 = x0 + DOCK_WIDTH - 16;
        int y1 = suggestionY + suggestions.size() * SUGGESTION_ROW_HEIGHT + 2;
        graphics.fill(x0 - 1, suggestionY - 1, x1 + 1, y1 + 1, PANEL_BORDER);
        graphics.fill(x0, suggestionY, x1, y1, PALETTE_BACKGROUND);
        for (int i = 0; i < suggestions.size(); i++) {
            int rowY = suggestionY + 1 + i * SUGGESTION_ROW_HEIGHT;
            boolean hovered = mouseX >= x0 && mouseX <= x1 && mouseY >= rowY && mouseY < rowY + SUGGESTION_ROW_HEIGHT;
            if (hovered) {
                graphics.fill(x0, rowY, x1, rowY + SUGGESTION_ROW_HEIGHT, PALETTE_ROW_HOVER);
            }
            ItemEntry entry = suggestions.get(i);
            if (entry.stack() != null && !entry.stack().isEmpty()) {
                graphics.item(entry.stack(), x0 + 2, rowY + 1);
            } else {
                graphics.fill(x0 + 5, rowY + 4, x0 + 15, rowY + 14, CHECKBOX_BORDER);
                graphics.fill(x0 + 6, rowY + 5, x0 + 14, rowY + 13, PALETTE_BACKGROUND);
            }
            int textWidth = x1 - x0 - 26;
            graphics.text(font, Component.literal(truncateToWidth(entry.name(), textWidth)), x0 + 22, rowY + 1, TEXT_PRIMARY);
            graphics.text(font, Component.literal(truncateToWidth(entry.id(), textWidth)), x0 + 22, rowY + 10, TEXT_MUTED);
        }
    }

    private void addCustomResource(int groupIndex) {
        BlueprintNode node = singleSelectedNode();
        EditBox field = resourceAddFields.get(groupIndex);
        if (node == null || field == null || !(node.settings() instanceof NodeSettings.Io)) {
            return;
        }
        String resource = field.getValue().trim();
        if (resource.isEmpty()) {
            return;
        }
        mutateIo(current -> current.withLimitAt(groupIndex, current.limitAt(groupIndex).withResourceAdded(resource)));
        customResourceText = "";
        customResourceGroup = -1;
        field.setValue("");
        requestInspectorRefresh();
    }

    private void buildTimerInspector(BlueprintNode node, NodeSettings.Timer timer, int x) {
        EditBox amountField = new EditBox(font, x, 36, 60, 16, RFMTranslations.INTERVAL.component());
        amountField.setFilter(value -> value.isEmpty() || value.matches("\\d{1,7}"));
        amountField.setValue(String.valueOf(timer.amount()));
        amountField.setResponder(value -> editing("timer_amount:" + node.id(), () -> updateTimer(t ->
                new NodeSettings.Timer(parseIntOr(value, t.amount()), t.seconds(), t.global(), t.offset()))));
        addInspectorWidget(amountField);
        addToggleButton(x + 66, 36, 80, (timer.seconds() ? RFMTranslations.SECONDS : RFMTranslations.TICKS).component(), () -> {
            updateTimer(t -> new NodeSettings.Timer(t.amount(), !t.seconds(), t.global(), t.offset()));
            requestInspectorRefresh();
        });
        addToggleButton(x, 58, 130, RFMTranslations.GLOBAL_ALIGN.component(onOffText(timer.global())), () -> {
            updateTimer(t -> new NodeSettings.Timer(t.amount(), t.seconds(), !t.global(), t.offset()));
            requestInspectorRefresh();
        });
        EditBox offsetField = new EditBox(font, x, 90, 60, 16, RFMTranslations.OFFSET.component());
        offsetField.setFilter(value -> value.isEmpty() || value.matches("\\d{1,7}"));
        offsetField.setValue(String.valueOf(timer.offset()));
        offsetField.setResponder(value -> editing("timer_offset:" + node.id(), () -> updateTimer(t ->
                new NodeSettings.Timer(t.amount(), t.seconds(), t.global(), parseIntOr(value, t.offset())))));
        addInspectorWidget(offsetField);
        int warnings = (timer.offset() >= timer.amount() ? 1 : 0)
                       + (!timer.seconds() && timer.amount() < 20 ? 1 : 0);
        inspectorBottom = 108 + warnings * 22;
    }

    private void updateTimer(java.util.function.UnaryOperator<NodeSettings.Timer> update) {
        BlueprintNode current = singleSelectedNode();
        if (current != null && current.settings() instanceof NodeSettings.Timer timer) {
            NodeSettings.Timer updated = update.apply(timer);
            if (!updated.equals(timer)) {
                current.setSettings(updated);
                markEdited();
            }
        }
    }

    private static int parseIntOr(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private void buildLabelPicker(BlueprintNode node, int headerY) {
        labelRowsStartY = headerY + 12;
        List<String> known = knownLabels(node);
        int visibleRows = Math.min(known.size(), MAX_VISIBLE_LABEL_ROWS);
        labelScroll = Mth.clamp(labelScroll, 0, Math.max(0, known.size() - MAX_VISIBLE_LABEL_ROWS));
        int rowsEnd = labelRowsStartY + Math.max(visibleRows, 1) * LABEL_ROW_HEIGHT;
        int x = dockX() + 8;
        customLabelField = new EditBox(font, x, rowsEnd + 6, DOCK_WIDTH - 72, 16, RFMTranslations.NEW_LABEL.component());
        customLabelField.setMaxLength(256);
        customLabelField.setValue(customLabelText);
        customLabelField.setResponder(value -> customLabelText = value);
        addInspectorWidget(customLabelField);
        addInspectorWidget(Button.builder(RFMTranslations.ADD.component(), button -> addCustomLabel())
                .pos(x + DOCK_WIDTH - 66, rowsEnd + 6)
                .size(50, 16)
                .build());
        inspectorBottom = rowsEnd + 40;
    }

    private void addToggleButton(int x, int y, int width, Component label, Runnable action) {
        addInspectorWidget(Button.builder(label, button -> action.run())
                .pos(x, y)
                .size(width, 16)
                .build());
    }

    private void addInspectorWidget(AbstractWidget widget) {
        inspectorWidgets.add(widget);
        inspectorWidgetBaseY.put(widget, widget.getY());
        addRenderableWidget(widget);
    }

    private int previewHeight() {
        if (!previewOpen || font == null) {
            return 0;
        }
        int lines = previewLines.isEmpty() ? 1 : previewLines.size();
        return 24 + lines * (font.lineHeight + 2);
    }

    private int dockContentBottom() {
        return inspectorBottom + previewHeight();
    }

    private int maxDockScroll() {
        return Math.max(0, dockContentBottom() + 6 - (height - 12));
    }

    private void applyDockScroll() {
        for (Map.Entry<AbstractWidget, Integer> entry : inspectorWidgetBaseY.entrySet()) {
            entry.getKey().setY(entry.getValue() - dockScroll);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();
        if (paletteOpen) {
            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && inPalette(mouseX, mouseY)) {
                int index = (int) ((mouseY - paletteY - PALETTE_HEADER_HEIGHT) / PALETTE_ROW_HEIGHT);
                List<NodeType> types = paletteTypes();
                if (index >= 0 && index < types.size()) {
                    BlueprintNode node = BlueprintNode.create(types.get(index), snap(paletteCanvasX), snap(paletteCanvasY));
                    graph.addNode(node);
                    if (pendingWireNode != null) {
                        String error = pendingWirePin.isOutput()
                                ? graph.connect(pendingWireNode, pendingWirePin, node.id())
                                : graph.connect(node.id(), node.type().continuationPin(), pendingWireNode);
                        if (error != null) {
                            setStatus(error);
                        }
                    }
                    selection.clear();
                    selection.add(node.id());
                    markEdited();
                    requestInspectorRefresh();
                }
            }
            paletteOpen = false;
            pendingWireNode = null;
            pendingWirePin = null;
            return true;
        }
        if (!suggestions.isEmpty() && button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            if (inSuggestions(mouseX, mouseY)) {
                applySuggestion(mouseY);
                return true;
            }
            clearSuggestions();
        }
        if (super.mouseClicked(event, doubleClick)) {
            return true;
        }
        if (inDock(mouseX)) {
            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                handleDockContentClick(mouseX, mouseY + dockScroll);
            }
            return true;
        }
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            for (int[] hitbox : errorRowHitboxes) {
                if (mouseX >= 8 && mouseX <= hitbox[1] && mouseY >= hitbox[0] - 1 && mouseY < hitbox[0] + 11) {
                    if (hitbox[2] >= 0 && hitbox[2] < errorNodeOrder.size()) {
                        focusNode(errorNodeOrder.get(hitbox[2]));
                    }
                    return true;
                }
            }
        }
        setFocused(null);
        if (button == GLFW.GLFW_MOUSE_BUTTON_MIDDLE) {
            dragMode = DragMode.PAN_MMB;
            beginPan(mouseX, mouseY);
            return true;
        }
        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            dragMode = DragMode.PAN_RMB;
            rightDragMoved = false;
            beginPan(mouseX, mouseY);
            return true;
        }
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            PinHit pin = findPin(mouseX, mouseY);
            if (pin != null) {
                if (event.hasAltDown()) {
                    if (graph.disconnect(pin.nodeId(), pin.role())) {
                        markEdited();
                    }
                } else {
                    dragMode = DragMode.WIRE;
                    wireFromNode = pin.nodeId();
                    wireFromPin = pin.role();
                    wireMouseX = mouseX;
                    wireMouseY = mouseY;
                }
                return true;
            }
            BlueprintNode node = findNode(mouseX, mouseY);
            if (node == null && event.hasAltDown()) {
                BlueprintEdge wire = findWire(mouseX, mouseY);
                if (wire != null && graph.removeEdge(wire)) {
                    hoveredWire = null;
                    markEdited();
                    return true;
                }
            }
            if (node != null) {
                boolean additive = event.hasShiftDown() || event.hasControlDown();
                if (additive) {
                    if (!selection.remove(node.id())) {
                        selection.add(node.id());
                    }
                } else if (!selection.contains(node.id())) {
                    selection.clear();
                    selection.add(node.id());
                }
                requestInspectorRefresh();
                if (selection.contains(node.id())) {
                    dragMode = DragMode.NODES;
                    nodeDragOffsets.clear();
                    double canvasX = screenToCanvasX(mouseX);
                    double canvasY = screenToCanvasY(mouseY);
                    for (UUID id : selection) {
                        BlueprintNode selected = graph.node(id);
                        if (selected != null) {
                            nodeDragOffsets.put(id, new double[]{selected.x() - canvasX, selected.y() - canvasY});
                        }
                    }
                }
                return true;
            }
            dragMode = DragMode.BOX;
            boxAdditive = event.hasShiftDown();
            boxStartX = screenToCanvasX(mouseX);
            boxStartY = screenToCanvasY(mouseY);
            boxCurrentX = boxStartX;
            boxCurrentY = boxStartY;
            return true;
        }
        return false;
    }

    private void handleDockContentClick(double mouseX, double mouseY) {
        BlueprintNode node = singleSelectedNode();
        if (node == null) {
            return;
        }
        if (node.settings() instanceof NodeSettings.Io io) {
            IoSection section = IoSection.of(io);
            int x0 = dockX();
            for (int groupIndex = 0; groupIndex < section.groups().size(); groupIndex++) {
                LimitGroup group = section.groups().get(groupIndex);
                boolean inColumn = mouseX >= x0 + 8 && mouseX <= x0 + DOCK_WIDTH - 8;
                if (inColumn && mouseY >= group.itemRowsY()
                        && mouseY < group.itemRowsY() + group.shownItemRows() * LABEL_ROW_HEIGHT) {
                    int rowIndex = (int) ((mouseY - group.itemRowsY()) / LABEL_ROW_HEIGHT);
                    int finalGroup = groupIndex;
                    mutateIo(current -> current.withLimitAt(
                            finalGroup,
                            current.limitAt(finalGroup).withResourceRemoved(rowIndex)
                    ));
                    requestInspectorRefresh();
                    return;
                }
                if (inColumn && mouseY >= group.tagRowsY()
                        && mouseY < group.tagRowsY() + group.shownTagRows() * LABEL_ROW_HEIGHT) {
                    int rowIndex = (int) ((mouseY - group.tagRowsY()) / LABEL_ROW_HEIGHT);
                    int finalGroup = groupIndex;
                    mutateIo(current -> current.withLimitAt(
                            finalGroup,
                            current.limitAt(finalGroup).withTagRemoved(rowIndex)
                    ));
                    requestInspectorRefresh();
                    return;
                }
            }
            if (mouseX >= x0 + 8 && mouseX <= x0 + DOCK_WIDTH - 8
                    && mouseY >= section.exceptRowsY()
                    && mouseY < section.exceptRowsY() + section.shownExceptRows() * LABEL_ROW_HEIGHT) {
                int rowIndex = (int) ((mouseY - section.exceptRowsY()) / LABEL_ROW_HEIGHT);
                mutateIo(current -> {
                    if (rowIndex < 0 || rowIndex >= current.except().size()) {
                        return current;
                    }
                    List<String> updated = new ArrayList<>(current.except());
                    updated.remove(rowIndex);
                    return current.withExcept(updated);
                });
                requestInspectorRefresh();
                return;
            }
            IoSide[] sides = IoSide.values();
            for (int i = 0; i <= sides.length; i++) {
                int[] rect = sideChipRect(i, section.chipsY());
                if (mouseX >= rect[0] && mouseX <= rect[0] + rect[2]
                        && mouseY >= rect[1] && mouseY <= rect[1] + rect[3]) {
                    if (i == sides.length) {
                        mutateIo(current -> current.withEachSide(!current.eachSide()));
                    } else {
                        IoSide side = sides[i];
                        List<IoSide> updated = new ArrayList<>(io.sides());
                        if (!updated.remove(side)) {
                            updated.add(side);
                        }
                        mutateIo(current -> current.withSides(updated));
                    }
                    return;
                }
            }
        }
        handleLabelRowClick(mouseX, mouseY);
    }

    private void handleLabelRowClick(double mouseX, double mouseY) {
        BlueprintNode node = singleSelectedNode();
        if (node == null || !supportsLabels(node.type()) || labelRowsStartY <= 0) {
            return;
        }
        List<String> known = knownLabels(node);
        if (known.isEmpty()) {
            return;
        }
        int x = dockX() + 8;
        if (mouseX < x || mouseX > dockX() + DOCK_WIDTH - 8) {
            return;
        }
        int visibleRows = Math.min(known.size(), MAX_VISIBLE_LABEL_ROWS);
        int row = (int) ((mouseY - labelRowsStartY) / LABEL_ROW_HEIGHT);
        if (mouseY < labelRowsStartY || row < 0 || row >= visibleRows) {
            return;
        }
        int index = row + labelScroll;
        if (index < known.size()) {
            toggleLabel(node, known.get(index));
        }
    }

    private void beginPan(double mouseX, double mouseY) {
        panAnchorMouseX = mouseX;
        panAnchorMouseY = mouseY;
        panAnchorCameraX = cameraX;
        panAnchorCameraY = cameraY;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        double mouseX = event.x();
        double mouseY = event.y();
        switch (dragMode) {
            case PAN_MMB, PAN_RMB -> {
                if (Math.abs(mouseX - panAnchorMouseX) + Math.abs(mouseY - panAnchorMouseY) > 3) {
                    rightDragMoved = true;
                }
                cameraX = panAnchorCameraX - (mouseX - panAnchorMouseX) / zoom;
                cameraY = panAnchorCameraY - (mouseY - panAnchorMouseY) / zoom;
                return true;
            }
            case NODES -> {
                double canvasX = screenToCanvasX(mouseX);
                double canvasY = screenToCanvasY(mouseY);
                for (Map.Entry<UUID, double[]> entry : nodeDragOffsets.entrySet()) {
                    BlueprintNode node = graph.node(entry.getKey());
                    if (node != null) {
                        node.setPosition(
                                snap(canvasX + entry.getValue()[0]),
                                snap(canvasY + entry.getValue()[1])
                        );
                    }
                }
                return true;
            }
            case WIRE -> {
                wireMouseX = mouseX;
                wireMouseY = mouseY;
                return true;
            }
            case BOX -> {
                boxCurrentX = screenToCanvasX(mouseX);
                boxCurrentY = screenToCanvasY(mouseY);
                return true;
            }
            default -> {
                return super.mouseDragged(event, dragX, dragY);
            }
        }
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        double mouseX = event.x();
        double mouseY = event.y();
        DragMode mode = dragMode;
        dragMode = DragMode.NONE;
        switch (mode) {
            case PAN_RMB -> {
                if (!rightDragMoved) {
                    openPalette(mouseX, mouseY);
                }
                return true;
            }
            case PAN_MMB -> {
                return true;
            }
            case NODES -> {
                nodeDragOffsets.clear();
                markEdited();
                return true;
            }
            case WIRE -> {
                finishWireDrag(mouseX, mouseY);
                return true;
            }
            case BOX -> {
                finishBoxSelect();
                return true;
            }
            default -> {
                return super.mouseReleased(event);
            }
        }
    }

    private void finishWireDrag(double mouseX, double mouseY) {
        PinHit target = findPin(mouseX, mouseY);
        UUID fromNode = wireFromNode;
        PinRole fromPin = wireFromPin;
        wireFromNode = null;
        wireFromPin = null;
        if (fromNode == null) {
            return;
        }
        if (target == null) {
            if (findNode(mouseX, mouseY) == null) {
                pendingWireNode = fromNode;
                pendingWirePin = fromPin;
                openPalette(mouseX, mouseY);
            }
            return;
        }
        String error;
        if (fromPin.isOutput() && target.role() == PinRole.EXEC_IN) {
            error = graph.connect(fromNode, fromPin, target.nodeId());
        } else if (fromPin == PinRole.EXEC_IN && target.role().isOutput()) {
            error = graph.connect(target.nodeId(), target.role(), fromNode);
        } else {
            error = "error.retrofactorymanager.incompatible_pins";
        }
        if (error != null) {
            setStatus(error);
        } else {
            markEdited();
        }
    }

    private void finishBoxSelect() {
        double screenDX = Math.abs(boxCurrentX - boxStartX) * zoom;
        double screenDY = Math.abs(boxCurrentY - boxStartY) * zoom;
        if (screenDX < 3 && screenDY < 3) {
            if (!boxAdditive) {
                selection.clear();
                requestInspectorRefresh();
            }
            return;
        }
        double minX = Math.min(boxStartX, boxCurrentX);
        double maxX = Math.max(boxStartX, boxCurrentX);
        double minY = Math.min(boxStartY, boxCurrentY);
        double maxY = Math.max(boxStartY, boxCurrentY);
        if (!boxAdditive) {
            selection.clear();
        }
        for (BlueprintNode node : graph.nodes()) {
            NodeLayout layout = NodeLayout.of(node, font);
            boolean intersects = node.x() < maxX && node.x() + layout.width() > minX
                    && node.y() < maxY && node.y() + layout.height() > minY;
            if (intersects) {
                selection.add(node.id());
            }
        }
        requestInspectorRefresh();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        if (deltaY == 0.0D) {
            return super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
        }
        if (inDock(mouseX)) {
            BlueprintNode node = singleSelectedNode();
            double contentY = mouseY + dockScroll;
            if (node != null && supportsLabels(node.type()) && labelRowsStartY > 0) {
                int visibleRows = Math.min(knownLabels(node).size(), MAX_VISIBLE_LABEL_ROWS);
                if (contentY >= labelRowsStartY && contentY < labelRowsStartY + visibleRows * LABEL_ROW_HEIGHT) {
                    int maxScroll = Math.max(0, knownLabels(node).size() - MAX_VISIBLE_LABEL_ROWS);
                    labelScroll = Mth.clamp(labelScroll - (int) Math.signum(deltaY), 0, maxScroll);
                    return true;
                }
            }
            if (maxDockScroll() > 0) {
                dockScroll = Mth.clamp(dockScroll - (int) Math.signum(deltaY) * 16, 0, maxDockScroll());
                clearSuggestions();
                applyDockScroll();
            }
            return true;
        }
        double focusX = screenToCanvasX(mouseX);
        double focusY = screenToCanvasY(mouseY);
        zoom = Mth.clamp(zoom * Math.pow(ZOOM_STEP, deltaY), MIN_ZOOM, MAX_ZOOM);
        cameraX = focusX - (mouseX - width / 2.0D) / zoom;
        cameraY = focusY - (mouseY - height / 2.0D) / zoom;
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int keyCode = event.key();
        if (keyCode == GLFW.GLFW_KEY_ESCAPE && paletteOpen) {
            paletteOpen = false;
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE && !suggestions.isEmpty()) {
            clearSuggestions();
            return true;
        }
        if (event.hasControlDown() && keyCode == GLFW.GLFW_KEY_Z) {
            if (event.hasShiftDown()) {
                redo();
            } else {
                undo();
            }
            return true;
        }
        if (event.hasControlDown() && keyCode == GLFW.GLFW_KEY_Y) {
            redo();
            return true;
        }
        if (event.hasControlDown() && !(getFocused() instanceof EditBox)) {
            if (keyCode == GLFW.GLFW_KEY_C) {
                copySelection();
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_V) {
                pasteClipboard(screenToCanvasX(lastMouseX), screenToCanvasY(lastMouseY));
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_D) {
                duplicateSelection();
                return true;
            }
        }
        if (keyCode == GLFW.GLFW_KEY_G && !(getFocused() instanceof EditBox)) {
            snapToGrid = !snapToGrid;
            setStatusMessage(RFMTranslations.SNAP.component(onOffText(snapToGrid)));
            return true;
        }
        if (getFocused() instanceof EditBox) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                setFocused(null);
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                if (getFocused() == customLabelField) {
                    addCustomLabel();
                    return true;
                }
                for (Map.Entry<Integer, EditBox> pending : resourceAddFields.entrySet()) {
                    if (getFocused() == pending.getValue()) {
                        addCustomResource(pending.getKey());
                        return true;
                    }
                }
                for (Map.Entry<Integer, EditBox> pending : tagAddFields.entrySet()) {
                    if (getFocused() == pending.getValue()) {
                        addTagEntry(pending.getKey());
                        return true;
                    }
                }
                if (getFocused() == exceptAddField) {
                    addExceptEntry();
                    return true;
                }
            }
            return super.keyPressed(event);
        }
        if ((keyCode == GLFW.GLFW_KEY_DELETE || keyCode == GLFW.GLFW_KEY_BACKSPACE) && !selection.isEmpty()) {
            graph.removeNodes(new ArrayList<>(selection));
            selection.clear();
            markEdited();
            requestInspectorRefresh();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_HOME) {
            frameAll();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        lastMouseX = mouseX;
        lastMouseY = mouseY;
        hoveredWire = dragMode == DragMode.NONE && !inDock(mouseX) && findPin(mouseX, mouseY) == null
                && findNode(mouseX, mouseY) == null
                ? findWire(mouseX, mouseY)
                : null;
        super.mouseMoved(mouseX, mouseY);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        if (needsInspectorRefresh) {
            needsInspectorRefresh = false;
            rebuildInspector();
        }
        graphics.fill(0, 0, width, height, BACKGROUND);
        renderGrid(graphics);
        renderEdges(graphics);
        renderWireDrag(graphics);
        renderNodes(graphics);
        renderBoxSelect(graphics);
        renderDock(graphics, mouseX, mouseY);
        renderHud(graphics);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        renderSuggestions(graphics, mouseX, mouseY);
        renderPalette(graphics, mouseX, mouseY);
        extractTooltips(graphics, mouseX, mouseY);
    }

    private void extractTooltips(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (paletteOpen || !suggestions.isEmpty() || dragMode != DragMode.NONE) {
            return;
        }
        if (inDock(mouseX)) {
            List<Component> dockTooltip = dockTooltipAt(mouseX, mouseY + dockScroll);
            if (dockTooltip != null && !dockTooltip.isEmpty()) {
                graphics.setTooltipForNextFrame(font, dockTooltip, Optional.empty(), mouseX, mouseY);
            }
            return;
        }
        PinHit pin = findPin(mouseX, mouseY);
        if (pin != null) {
            graphics.setTooltipForNextFrame(pinTooltip(pin.role()), mouseX, mouseY);
            return;
        }
        BlueprintNode node = findNode(mouseX, mouseY);
        if (node != null) {
            graphics.setTooltipForNextFrame(font, nodeTooltip(node), Optional.empty(), mouseX, mouseY);
        }
    }

    private Component pinTooltip(PinRole role) {
        return switch (role) {
            case EXEC_IN -> RFMTranslations.PIN_EXEC_IN.component();
            case EXEC_OUT -> RFMTranslations.PIN_EXEC_OUT.component();
            case TRUE_OUT -> RFMTranslations.PIN_TRUE_HINT.component();
            case FALSE_OUT -> RFMTranslations.PIN_FALSE_HINT.component();
            case AFTER_OUT -> RFMTranslations.PIN_AFTER_HINT.component();
        };
    }

    private List<Component> nodeTooltip(BlueprintNode node) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable(node.type().translationKey()));
        lines.addAll(NodeSummaries.lines(node.settings()));
        List<String> errors = nodeErrors.get(node.id());
        if (errors != null) {
            for (String error : errors) {
                lines.add(Component.literal(error.replace("\n", " ")).withStyle(style -> style.withColor(0xFF6B6B)));
            }
        }
        String warning = nodeWarnings.get(node.id());
        if (warning != null) {
            lines.add(Component.literal(warning).withStyle(style -> style.withColor(0xE8A33D)));
        }
        return lines;
    }

    /// Full text for dock rows that render truncated. Coordinates are already scroll-adjusted.
    @Nullable
    private List<Component> dockTooltipAt(double mouseX, double contentY) {
        BlueprintNode node = singleSelectedNode();
        if (node == null) {
            return null;
        }
        int x0 = dockX();
        if (mouseX < x0 + 8 || mouseX > x0 + DOCK_WIDTH - 8) {
            return null;
        }
        if (node.settings() instanceof NodeSettings.Io io) {
            IoSection section = IoSection.of(io);
            for (int i = 0; i < section.groups().size(); i++) {
                LimitGroup group = section.groups().get(i);
                String resource = rowAt(io.limitAt(i).resources(), group.itemRowsY(), group.shownItemRows(), contentY);
                if (resource != null) {
                    return List.of(Component.literal(resource));
                }
                String tag = rowAt(io.limitAt(i).tags(), group.tagRowsY(), group.shownTagRows(), contentY);
                if (tag != null) {
                    return List.of(Component.literal("#" + tag));
                }
            }
            String except = rowAt(io.except(), section.exceptRowsY(), section.shownExceptRows(), contentY);
            if (except != null) {
                return List.of(Component.literal(except));
            }
        }
        if (supportsLabels(node.type()) && labelRowsStartY > 0) {
            List<String> known = knownLabels(node);
            int visible = Math.min(known.size(), MAX_VISIBLE_LABEL_ROWS);
            int row = (int) ((contentY - labelRowsStartY) / LABEL_ROW_HEIGHT);
            if (contentY >= labelRowsStartY && row >= 0 && row < visible) {
                int index = row + labelScroll;
                if (index < known.size()) {
                    String label = known.get(index);
                    LabelInfo info = worldLabelInfo().getOrDefault(label, new LabelInfo(0, 0));
                    List<Component> lines = new ArrayList<>();
                    lines.add(Component.literal(label));
                    if (info.totalBlocks() == 0) {
                        lines.add(RFMTranslations.WARN_NO_BLOCKS.component()
                                .withStyle(style -> style.withColor(0xFF6B6B)));
                    } else if (info.gunOnly()) {
                        lines.add(RFMTranslations.GUN_ONLY_NOTE.component()
                                .withStyle(style -> style.withColor(0xE8A33D)));
                    } else {
                        lines.add(RFMTranslations.LABEL_BLOCK_COUNT.component(info.totalBlocks())
                                .withStyle(style -> style.withColor(0xA7B2BD)));
                    }
                    return lines;
                }
            }
        }
        return null;
    }

    @Nullable
    private String rowAt(List<String> values, int startY, int shownRows, double contentY) {
        if (contentY < startY || contentY >= startY + shownRows * LABEL_ROW_HEIGHT) {
            return null;
        }
        int index = (int) ((contentY - startY) / LABEL_ROW_HEIGHT);
        return index >= 0 && index < values.size() ? values.get(index) : null;
    }

    private void renderGrid(GuiGraphicsExtractor graphics) {
        double step = BASE_GRID_STEP;
        while (step * zoom < MIN_GRID_PIXEL_STEP) {
            step *= 2.0D;
        }
        while (step * zoom >= MIN_GRID_PIXEL_STEP * 4.0D) {
            step /= 2.0D;
        }
        int firstVertical = Mth.floor(screenToCanvasX(0) / step);
        int lastVertical = Mth.ceil(screenToCanvasX(width) / step);
        for (int gridX = firstVertical; gridX <= lastVertical; gridX++) {
            int screenX = (int) Math.round(canvasToScreenX(gridX * step));
            int color = Math.floorMod(gridX, MAJOR_GRID_INTERVAL) == 0 ? GRID_MAJOR : GRID_MINOR;
            graphics.fill(screenX, 0, screenX + 1, height, color);
        }
        int firstHorizontal = Mth.floor(screenToCanvasY(0) / step);
        int lastHorizontal = Mth.ceil(screenToCanvasY(height) / step);
        for (int gridY = firstHorizontal; gridY <= lastHorizontal; gridY++) {
            int screenY = (int) Math.round(canvasToScreenY(gridY * step));
            int color = Math.floorMod(gridY, MAJOR_GRID_INTERVAL) == 0 ? GRID_MAJOR : GRID_MINOR;
            graphics.fill(0, screenY, width, screenY + 1, color);
        }
    }

    private int pinColor(PinRole role) {
        return switch (role) {
            case TRUE_OUT -> PIN_TRUE;
            case FALSE_OUT -> PIN_FALSE;
            default -> PIN_EXEC;
        };
    }

    private int wireColor(PinRole fromPin) {
        return switch (fromPin) {
            case TRUE_OUT -> PIN_TRUE;
            case FALSE_OUT -> PIN_FALSE;
            default -> WIRE_EXEC;
        };
    }

    private void renderEdges(GuiGraphicsExtractor graphics) {
        for (BlueprintEdge edge : graph.edges()) {
            BlueprintNode from = graph.node(edge.fromNode());
            BlueprintNode to = graph.node(edge.toNode());
            if (from == null || to == null) {
                continue;
            }
            double[] start = pinScreenPosition(from, edge.fromPin());
            double[] end = pinScreenPosition(to, edge.toPin());
            if (start != null && end != null) {
                boolean hovered = edge.equals(hoveredWire);
                drawWire(graphics, start[0], start[1], end[0], end[1],
                        hovered ? WIRE_HOVER : wireColor(edge.fromPin()));
            }
        }
    }

    @Nullable
    private double[] pinScreenPosition(BlueprintNode node, PinRole role) {
        NodeLayout layout = NodeLayout.of(node, font);
        for (NodeLayout.PinSpot pin : layout.pins()) {
            if (pin.role() == role) {
                return new double[]{canvasToScreenX(node.x() + pin.dx()), canvasToScreenY(node.y() + pin.dy())};
            }
        }
        return null;
    }

    private void renderWireDrag(GuiGraphicsExtractor graphics) {
        if (dragMode != DragMode.WIRE || wireFromNode == null) {
            return;
        }
        BlueprintNode from = graph.node(wireFromNode);
        if (from == null) {
            return;
        }
        double[] start = pinScreenPosition(from, wireFromPin);
        if (start == null) {
            return;
        }
        if (wireFromPin.isOutput()) {
            drawWire(graphics, start[0], start[1], wireMouseX, wireMouseY, wireColor(wireFromPin));
        } else {
            drawWire(graphics, wireMouseX, wireMouseY, start[0], start[1], WIRE_EXEC);
        }
    }

    private void drawWire(GuiGraphicsExtractor graphics, double x0, double y0, double x1, double y1, int color) {
        double distance = Math.abs(x1 - x0) + Math.abs(y1 - y0);
        double tangent = wireTangent(x0, x1);
        double c0x = x0 + tangent;
        double c1x = x1 - tangent;
        int steps = (int) Mth.clamp(distance / 7, 10, 60);
        int thickness = Math.max(2, (int) Math.round(2.2D * zoom));
        int half = thickness / 2;
        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;
            double omt = 1 - t;
            double px = omt * omt * omt * x0 + 3 * omt * omt * t * c0x + 3 * omt * t * t * c1x + t * t * t * x1;
            double py = omt * omt * omt * y0 + 3 * omt * omt * t * y0 + 3 * omt * t * t * y1 + t * t * t * y1;
            int cx = (int) Math.round(px);
            int cy = (int) Math.round(py);
            graphics.fill(cx - half, cy - half, cx - half + thickness, cy - half + thickness, color);
        }
    }

    private int headerColor(NodeCategory category) {
        return switch (category) {
            case TRIGGER -> 0xFFC2622F;
            case IO -> 0xFF2F6FC2;
            case FLOW -> 0xFF7C4FC2;
            case MISC -> 0xFF55606B;
        };
    }

    private void renderNodes(GuiGraphicsExtractor graphics) {
        for (BlueprintNode node : graph.nodes()) {
            NodeLayout layout = NodeLayout.of(node, font);
            int sx = (int) Math.round(canvasToScreenX(node.x()));
            int sy = (int) Math.round(canvasToScreenY(node.y()));
            int sw = (int) Math.round(layout.width() * zoom);
            int sh = (int) Math.round(layout.height() * zoom);
            int headerH = (int) Math.round(layout.headerHeight() * zoom);
            if (sx > width || sy > height || sx + sw < 0 || sy + sh < 0) {
                continue;
            }
            boolean selected = selection.contains(node.id());
            boolean hasError = nodeErrors.containsKey(node.id());
            boolean hasWarning = !hasError && nodeWarnings.containsKey(node.id());
            int border = hasError ? TEXT_STATUS_ERROR
                    : hasWarning ? TEXT_STATUS_WARN
                    : selected ? NODE_BORDER_SELECTED
                    : NODE_BORDER;
            int borderThickness = selected || hasError ? 2 : 1;
            graphics.fill(sx - borderThickness, sy - borderThickness, sx + sw + borderThickness, sy + sh + borderThickness, border);
            graphics.fill(sx, sy, sx + sw, sy + headerH, headerColor(node.type().category()));
            graphics.fill(sx, sy + headerH, sx + sw, sy + sh, NODE_BODY);

            graphics.pose().pushMatrix();
            graphics.pose().translate((float) canvasToScreenX(node.x()), (float) canvasToScreenY(node.y()));
            graphics.pose().scale((float) zoom, (float) zoom);
            drawText(graphics, Component.translatable(node.type().translationKey()), 5, 3, TEXT_PRIMARY);
            List<Component> summary = layout.summaryLines();
            for (int i = 0; i < summary.size(); i++) {
                drawText(graphics, summary.get(i), 8, (int) (NodeLayout.HEADER_HEIGHT + 5 + i * NodeLayout.LINE_HEIGHT), TEXT_MUTED);
            }
            for (NodeLayout.PinSpot pin : layout.pins()) {
                if (pin.role().hasLabel()) {
                    Component pinLabel = Component.translatable(pin.role().labelKey());
                    int labelX = (int) (layout.width() - 8 - font.width(pinLabel));
                    drawText(graphics, pinLabel, labelX, (int) (pin.dy() - 4), TEXT_MUTED);
                }
            }
            graphics.pose().popMatrix();

            if (hasError || hasWarning) {
                int badgeSize = Math.max(7, (int) Math.round(9 * zoom));
                int badgeX = sx + sw - badgeSize / 2;
                int badgeY = sy - badgeSize / 2;
                int badgeColor = hasError ? TEXT_STATUS_ERROR : TEXT_STATUS_WARN;
                graphics.fill(badgeX - 1, badgeY - 1, badgeX + badgeSize + 1, badgeY + badgeSize + 1, PIN_BORDER);
                graphics.fill(badgeX, badgeY, badgeX + badgeSize, badgeY + badgeSize, badgeColor);
                if (badgeSize >= 8) {
                    graphics.text(font, Component.literal("!"), badgeX + badgeSize / 2 - 1, badgeY, 0xFF10151A);
                }
            }
            for (NodeLayout.PinSpot pin : layout.pins()) {
                int px = (int) Math.round(canvasToScreenX(node.x() + pin.dx()));
                int py = (int) Math.round(canvasToScreenY(node.y() + pin.dy()));
                int radius = Math.max(3, (int) Math.round(4 * zoom));
                boolean highlight = dragMode == DragMode.WIRE && isCompatibleWireTarget(node, pin.role());
                if (highlight) {
                    graphics.fill(px - radius - 2, py - radius - 2, px + radius + 2, py + radius + 2, NODE_BORDER_SELECTED);
                }
                graphics.fill(px - radius, py - radius, px + radius, py + radius, PIN_BORDER);
                graphics.fill(px - radius + 1, py - radius + 1, px + radius - 1, py + radius - 1, pinColor(pin.role()));
            }
        }
    }

    private boolean isCompatibleWireTarget(BlueprintNode node, PinRole role) {
        if (wireFromNode == null || node.id().equals(wireFromNode)) {
            return false;
        }
        if (wireFromPin.isOutput()) {
            return role == PinRole.EXEC_IN;
        }
        return role.isOutput();
    }

    private void drawText(GuiGraphicsExtractor graphics, Component text, int x, int y, int color) {
        graphics.textRenderer().accept(x, y, text.copy().withStyle(style -> style.withColor(color & 0xFFFFFF)));
    }

    private Component onOffText(boolean value) {
        return (value ? RFMTranslations.ON : RFMTranslations.OFF).component();
    }

    private void renderBoxSelect(GuiGraphicsExtractor graphics) {
        if (dragMode != DragMode.BOX) {
            return;
        }
        int x0 = (int) Math.round(canvasToScreenX(Math.min(boxStartX, boxCurrentX)));
        int y0 = (int) Math.round(canvasToScreenY(Math.min(boxStartY, boxCurrentY)));
        int x1 = (int) Math.round(canvasToScreenX(Math.max(boxStartX, boxCurrentX)));
        int y1 = (int) Math.round(canvasToScreenY(Math.max(boxStartY, boxCurrentY)));
        graphics.fill(x0, y0, x1, y1, BOX_FILL);
        graphics.fill(x0, y0, x1, y0 + 1, BOX_BORDER);
        graphics.fill(x0, y1 - 1, x1, y1, BOX_BORDER);
        graphics.fill(x0, y0, x0 + 1, y1, BOX_BORDER);
        graphics.fill(x1 - 1, y0, x1, y1, BOX_BORDER);
    }

    private void renderDock(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (!dockVisible()) {
            return;
        }
        graphics.nextStratum();
        int x0 = dockX();
        graphics.fill(x0, 0, width, height, PANEL_BACKGROUND);
        graphics.fill(x0, 0, x0 + 1, height, PANEL_BORDER);
        BlueprintNode node = singleSelectedNode();
        if (node != null) {
            graphics.pose().pushMatrix();
            graphics.pose().translate(0.0F, (float) -dockScroll);
            renderInspectorContent(graphics, node, mouseX, mouseY + dockScroll);
            graphics.pose().popMatrix();
        } else if (previewOpen) {
            graphics.text(font, RFMTranslations.PROGRAM_NAME.component(), x0 + 8, 24, TEXT_MUTED);
        }
        if (previewOpen) {
            renderPreviewSection(graphics);
        }
        renderDockScrollbar(graphics);
    }

    private void renderDockScrollbar(GuiGraphicsExtractor graphics) {
        int maxScroll = maxDockScroll();
        if (maxScroll <= 0) {
            return;
        }
        int trackTop = 4;
        int trackHeight = height - 8;
        int contentHeight = dockContentBottom() + 6;
        int barHeight = Math.max(20, trackHeight * trackHeight / contentHeight);
        int barY = trackTop + (int) ((trackHeight - barHeight) * (double) dockScroll / maxScroll);
        graphics.fill(width - 4, trackTop, width - 2, trackTop + trackHeight, PALETTE_BACKGROUND);
        graphics.fill(width - 4, barY, width - 2, barY + barHeight, PANEL_BORDER);
    }

    private void renderInspectorContent(GuiGraphicsExtractor graphics, BlueprintNode node, int mouseX, int mouseY) {
        int x0 = dockX();
        graphics.fill(x0 + 8, 8, x0 + 16, 16, headerColor(node.type().category()));
        graphics.text(font, Component.translatable(node.type().translationKey()), x0 + 21, 8, TEXT_PRIMARY);
        switch (node.settings()) {
            case NodeSettings.Timer timer -> {
                graphics.text(font, RFMTranslations.INTERVAL.component(), x0 + 8, 26, TEXT_MUTED);
                RFMTranslations.Entry offsetHint = timer.seconds()
                        ? RFMTranslations.OFFSET_HINT_SECONDS
                        : RFMTranslations.OFFSET_HINT;
                graphics.text(font, offsetHint.component(), x0 + 8, 80, TEXT_MUTED);
                int warnY = 112;
                if (timer.offset() >= timer.amount()) {
                    graphics.text(font, RFMTranslations.OFFSET_WARNING.component(), x0 + 8, warnY, TEXT_STATUS_WARN);
                    graphics.text(font, RFMTranslations.OFFSET_WARNING2.component(), x0 + 8, warnY + 10, TEXT_STATUS_WARN);
                    warnY += 22;
                }
                if (!timer.seconds() && timer.amount() < 20) {
                    graphics.text(font, RFMTranslations.MIN_INTERVAL_WARNING.component(), x0 + 8, warnY, TEXT_STATUS_WARN);
                    graphics.text(font, RFMTranslations.MIN_INTERVAL_WARNING2.component(), x0 + 8, warnY + 10, TEXT_STATUS_WARN);
                }
            }
            case NodeSettings.RedstonePulse ignored ->
                    graphics.text(font, RFMTranslations.NO_OPTIONS.component(), x0 + 8, 26, TEXT_MUTED);
            case NodeSettings.Io io -> renderIoContent(graphics, node, io, mouseX, mouseY);
            case NodeSettings.Forget ignored -> {
                graphics.text(font, RFMTranslations.FORGET_NOTE.component(), x0 + 8, 26, TEXT_MUTED);
                renderLabelRows(graphics, node, mouseX, mouseY, 38);
            }
            case NodeSettings.If ifSettings -> {
                if (ifSettings.useText()) {
                    graphics.text(font, RFMTranslations.CONDITION_SFML.component(), x0 + 8, 26, TEXT_MUTED);
                } else {
                    graphics.text(font, RFMTranslations.CONDITIONS.component(), x0 + 8, 26, TEXT_MUTED);
                    for (int i = 1; i < ifSettings.rows().size(); i++) {
                        int separatorY = 46 + i * CONDITION_ROW_HEIGHT - 2;
                        graphics.fill(x0 + 8, separatorY, x0 + DOCK_WIDTH - 8, separatorY + 1, PANEL_BORDER);
                    }
                }
            }
            case NodeSettings.Comment ignored ->
                    graphics.text(font, RFMTranslations.COMMENT_TEXT.component(), x0 + 8, 26, TEXT_MUTED);
        }
    }

    private void renderIoContent(GuiGraphicsExtractor graphics, BlueprintNode node, NodeSettings.Io io, int mouseX, int mouseY) {
        int x0 = dockX();
        int x = x0 + 8;
        IoSection section = IoSection.of(io);
        for (int i = 0; i < section.groups().size(); i++) {
            LimitGroup group = section.groups().get(i);
            LimitEntry entry = io.limitAt(i);
            if (i > 0) {
                graphics.fill(x0 + 8, group.headerY() - 6, x0 + DOCK_WIDTH - 8, group.headerY() - 5, PANEL_BORDER);
            }
            Component header = section.groups().size() > 1
                    ? RFMTranslations.FILTER_GROUP.component(i + 1)
                    : RFMTranslations.ITEMS_HEADER.component();
            graphics.text(font, header, x, group.headerY(), TEXT_MUTED);
            graphics.text(font, RFMTranslations.QTY.component(), x, group.qtyY() + 4, TEXT_MUTED);
            graphics.text(font, RFMTranslations.KEEP.component(), x + 70, group.qtyY() + 4, TEXT_MUTED);
            List<String> resources = entry.resources();
            for (int row = 0; row < group.shownItemRows(); row++) {
                int rowY = group.itemRowsY() + row * LABEL_ROW_HEIGHT;
                boolean hovered = mouseX >= x && mouseX <= x0 + DOCK_WIDTH - 8
                        && mouseY >= rowY && mouseY < rowY + LABEL_ROW_HEIGHT;
                if (hovered) {
                    graphics.fill(x0 + 6, rowY - 1, x0 + DOCK_WIDTH - 6, rowY + LABEL_ROW_HEIGHT - 1, PALETTE_ROW_HOVER);
                }
                graphics.text(font, Component.literal("x"), x0 + DOCK_WIDTH - 14, rowY + 2, TEXT_STATUS_ERROR);
                String shown = truncateToWidth(resources.get(row), DOCK_WIDTH - 40);
                graphics.text(font, Component.literal(shown), x, rowY + 2, TEXT_PRIMARY);
            }
            if (group.moreItemsLine()) {
                int moreY = group.itemRowsY() + group.shownItemRows() * LABEL_ROW_HEIGHT;
                graphics.text(font, RFMTranslations.MORE_ITEMS.component(resources.size() - group.shownItemRows()), x, moreY, TEXT_MUTED);
            }
            graphics.text(font, RFMTranslations.TAGS_HEADER.component(), x, group.tagModeY() - 11, TEXT_MUTED);
            for (int row = 0; row < group.shownTagRows(); row++) {
                int rowY = group.tagRowsY() + row * LABEL_ROW_HEIGHT;
                boolean hovered = mouseX >= x && mouseX <= x0 + DOCK_WIDTH - 8
                        && mouseY >= rowY && mouseY < rowY + LABEL_ROW_HEIGHT;
                if (hovered) {
                    graphics.fill(x0 + 6, rowY - 1, x0 + DOCK_WIDTH - 6, rowY + LABEL_ROW_HEIGHT - 1, PALETTE_ROW_HOVER);
                }
                graphics.text(font, Component.literal("x"), x0 + DOCK_WIDTH - 14, rowY + 2, TEXT_STATUS_ERROR);
                String shown = truncateToWidth("#" + entry.tags().get(row), DOCK_WIDTH - 40);
                graphics.text(font, Component.literal(shown), x, rowY + 2, TEXT_PRIMARY);
            }
        }
        graphics.text(font, RFMTranslations.EXCEPT_HEADER.component(), x, section.exceptHeaderY(), TEXT_MUTED);
        for (int row = 0; row < section.shownExceptRows(); row++) {
            int rowY = section.exceptRowsY() + row * LABEL_ROW_HEIGHT;
            boolean hovered = mouseX >= x && mouseX <= x0 + DOCK_WIDTH - 8
                    && mouseY >= rowY && mouseY < rowY + LABEL_ROW_HEIGHT;
            if (hovered) {
                graphics.fill(x0 + 6, rowY - 1, x0 + DOCK_WIDTH - 6, rowY + LABEL_ROW_HEIGHT - 1, PALETTE_ROW_HOVER);
            }
            graphics.text(font, Component.literal("x"), x0 + DOCK_WIDTH - 14, rowY + 2, TEXT_STATUS_ERROR);
            String shown = truncateToWidth(io.except().get(row), DOCK_WIDTH - 40);
            graphics.text(font, Component.literal(shown), x, rowY + 2, TEXT_PRIMARY);
        }
        graphics.text(font, RFMTranslations.SIDES.component(), x, section.sidesHeaderY(), TEXT_MUTED);
        IoSide[] sides = IoSide.values();
        for (int i = 0; i <= sides.length; i++) {
            int[] rect = sideChipRect(i, section.chipsY());
            boolean eachChip = i == sides.length;
            boolean selected = eachChip ? io.eachSide() : io.sides().contains(sides[i]);
            boolean dimmed = !eachChip && io.eachSide();
            int background = selected ? NODE_BORDER_SELECTED : PALETTE_BACKGROUND;
            graphics.fill(rect[0] - 1, rect[1] - 1, rect[0] + rect[2] + 1, rect[1] + rect[3] + 1, CHECKBOX_BORDER);
            graphics.fill(rect[0], rect[1], rect[0] + rect[2], rect[1] + rect[3], background);
            Component text = eachChip ? RFMTranslations.SIDE_EACH.component() : Component.translatable(sides[i].translationKey());
            int textColor = dimmed ? TEXT_MUTED : selected ? 0xFF10151A : TEXT_PRIMARY;
            graphics.text(font, text, rect[0] + (rect[2] - font.width(text)) / 2, rect[1] + 2, textColor);
        }
        graphics.text(font, RFMTranslations.SLOTS.component(), x, section.slotsY() + 4, TEXT_MUTED);
        renderLabelRows(graphics, node, mouseX, mouseY, section.labelsHeaderY());
    }

    private void renderLabelRows(GuiGraphicsExtractor graphics, BlueprintNode node, int mouseX, int mouseY, int headerY) {
        int x0 = dockX();
        graphics.text(font, RFMTranslations.LABELS_HEADER.component(), x0 + 8, headerY, TEXT_MUTED);
        List<String> known = knownLabels(node);
        List<String> active = nodeLabels(node.settings());
        Map<String, LabelInfo> info = worldLabelInfo();
        int startY = headerY + 12;
        if (known.isEmpty()) {
            graphics.text(font, RFMTranslations.NO_LABELS.component(), x0 + 8, startY + 2, TEXT_MUTED);
            return;
        }
        int visibleRows = Math.min(known.size(), MAX_VISIBLE_LABEL_ROWS);
        for (int row = 0; row < visibleRows; row++) {
            int index = row + labelScroll;
            if (index >= known.size()) {
                break;
            }
            int rowY = startY + row * LABEL_ROW_HEIGHT;
            boolean hovered = mouseX >= x0 + 8 && mouseX <= x0 + DOCK_WIDTH - 8
                    && mouseY >= rowY && mouseY < rowY + LABEL_ROW_HEIGHT;
            if (hovered) {
                graphics.fill(x0 + 6, rowY - 1, x0 + DOCK_WIDTH - 6, rowY + LABEL_ROW_HEIGHT - 1, PALETTE_ROW_HOVER);
            }
            String label = known.get(index);
            boolean checked = active.contains(label);
            graphics.fill(x0 + 9, rowY + 1, x0 + 18, rowY + 10, CHECKBOX_BORDER);
            graphics.fill(x0 + 10, rowY + 2, x0 + 17, rowY + 9, PANEL_BACKGROUND);
            if (checked) {
                graphics.fill(x0 + 11, rowY + 3, x0 + 16, rowY + 8, NODE_BORDER_SELECTED);
            }
            LabelInfo labelInfo = info.getOrDefault(label, new LabelInfo(0, 0));
            String count = labelInfo.totalBlocks() + (labelInfo.gunOnly() ? "*" : "");
            int countColor = labelInfo.totalBlocks() == 0 ? TEXT_STATUS_ERROR
                    : labelInfo.gunOnly() ? TEXT_STATUS_WARN
                    : TEXT_MUTED;
            int countX = x0 + DOCK_WIDTH - 10 - font.width(count);
            graphics.text(font, Component.literal(count), countX, rowY + 2, countColor);
            String shown = truncateToWidth(label, countX - (x0 + 23) - 4);
            graphics.text(font, Component.literal(shown), x0 + 23, rowY + 2, checked ? TEXT_PRIMARY : TEXT_MUTED);
        }
        if (known.size() > MAX_VISIBLE_LABEL_ROWS) {
            Component hint = RFMTranslations.LABEL_SCROLL.component(labelScroll + visibleRows, known.size());
            graphics.text(font, hint, x0 + DOCK_WIDTH - 12 - font.width(hint), headerY, TEXT_MUTED);
        }
        int noteY = startY + Math.max(visibleRows, 1) * LABEL_ROW_HEIGHT + 26;
        boolean anyGunOnly = active.stream().anyMatch(label -> info.getOrDefault(label, new LabelInfo(0, 0)).gunOnly());
        boolean anyUnassigned = active.stream().anyMatch(label -> info.getOrDefault(label, new LabelInfo(0, 0)).totalBlocks() == 0);
        if (anyUnassigned) {
            graphics.text(font, RFMTranslations.UNASSIGNED_NOTE.component(), x0 + 8, noteY, TEXT_STATUS_ERROR);
        } else if (anyGunOnly) {
            graphics.text(font, RFMTranslations.GUN_ONLY_NOTE.component(), x0 + 8, noteY, TEXT_STATUS_WARN);
        }
    }

    private String truncateToWidth(String text, int maxWidth) {
        if (font.width(text) <= maxWidth) {
            return text;
        }
        StringBuilder sb = new StringBuilder();
        int ellipsis = font.width("...");
        for (char c : text.toCharArray()) {
            if (font.width(sb.toString() + c) + ellipsis > maxWidth) {
                break;
            }
            sb.append(c);
        }
        return sb + "...";
    }

    private void renderPreviewSection(GuiGraphicsExtractor graphics) {
        int x0 = dockX();
        int y = inspectorBottom - dockScroll + 10;
        graphics.fill(x0 + 8, y - 4, x0 + DOCK_WIDTH - 8, y - 3, PANEL_BORDER);
        graphics.text(font, RFMTranslations.SFML_PREVIEW.component(), x0 + 8, y, TEXT_PRIMARY);
        y += 14;
        int lineHeight = font.lineHeight + 2;
        if (previewLines.isEmpty()) {
            graphics.text(font, RFMTranslations.EMPTY_PROGRAM.component(), x0 + 8, y, TEXT_MUTED);
            return;
        }
        for (String line : previewLines) {
            if (y > height - 12) {
                break;
            }
            if (y > -lineHeight) {
                graphics.text(font, Component.literal(line), x0 + 8, y, TEXT_MUTED);
            }
            y += lineHeight;
        }
    }

    private void renderHud(GuiGraphicsExtractor graphics) {
        Component title = RFMTranslations.TITLE.component();
        graphics.text(font, title, 8, 6, TEXT_PRIMARY);
        if (unsavedChanges) {
            graphics.text(font, RFMTranslations.UNSAVED.component(), 14 + font.width(title), 6, TEXT_STATUS_WARN);
        }
        graphics.text(font, RFMTranslations.STATS.component(graph.nodes().size(), graph.edges().size(), Math.round(zoom * 100)), 8, 18, TEXT_MUTED);
        int hudRight = (dockVisible() ? dockX() : width) - 12;
        Component labelsLine = RFMTranslations.LABELS_AVAILABLE.component(worldLabels().size());
        graphics.text(font, labelsLine, 8, height - 52, TEXT_MUTED);
        Component undoHint = RFMTranslations.UNDO_HINT.component();
        if (8 + font.width(labelsLine) + 12 + font.width(undoHint) <= hudRight) {
            graphics.text(font, undoHint, hudRight - font.width(undoHint), height - 52, TEXT_MUTED);
        }
        Component hint = RFMTranslations.HINT.component();
        if (font.width(hint) + 8 <= hudRight) {
            graphics.text(font, hint, 8, height - 40, TEXT_MUTED);
        } else {
            graphics.text(font, Component.literal(truncateToWidth(hint.getString(), hudRight - 8)), 8, height - 40, TEXT_MUTED);
        }
        if (legacyProgramWithoutGraph && graph.isEmpty()) {
            graphics.text(
                    font,
                    importable != null
                            ? RFMTranslations.IMPORT_AVAILABLE.component()
                            : RFMTranslations.LEGACY_WARNING.component(),
                    8,
                    height - 64,
                    TEXT_STATUS_WARN
            );
        }
        errorRowHitboxes.clear();
        if (!programErrors.isEmpty()) {
            int maxWidth = (dockVisible() ? dockX() : width) - 16;
            int shown = Math.min(programErrors.size(), 3);
            int y = height - 64 - shown * 12 - (legacyProgramWithoutGraph ? 12 : 0);
            Component header = programErrors.size() == 1
                    ? RFMTranslations.ERRORS_ONE.component()
                    : RFMTranslations.ERRORS_MANY.component(programErrors.size());
            graphics.text(font, header, 8, y, TEXT_STATUS_ERROR);
            if (!errorNodeOrder.isEmpty()) {
                Component clickHint = RFMTranslations.ERRORS_CLICK_HINT.component();
                graphics.text(font, clickHint, 12 + font.width(header), y, TEXT_MUTED);
            }
            for (int i = 0; i < shown; i++) {
                int rowY = y + 12 + i * 12;
                String error = programErrors.get(i);
                UUID owner = ownerOfError(error);
                boolean clickable = owner != null;
                boolean hovered = clickable && lastMouseX >= 8 && lastMouseX <= maxWidth
                        && lastMouseY >= rowY - 1 && lastMouseY < rowY + 11;
                if (hovered) {
                    graphics.fill(6, rowY - 1, maxWidth, rowY + 11, PALETTE_ROW_HOVER);
                }
                String line = truncateToWidth(error.replace("\n", " "), maxWidth);
                graphics.text(font, Component.literal(line), 8, rowY, TEXT_STATUS_ERROR);
                if (clickable) {
                    errorRowHitboxes.add(new int[]{rowY, maxWidth, errorNodeOrder.indexOf(owner)});
                }
            }
        }
        if (System.currentTimeMillis() < statusExpiresAt) {
            graphics.centeredText(font, statusMessage, width / 2, 30, TEXT_STATUS_ERROR);
        }
    }

    private void renderPalette(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (!paletteOpen) {
            return;
        }
        graphics.nextStratum();
        int h = paletteHeight();
        graphics.fill(paletteX - 1, paletteY - 1, paletteX + PALETTE_WIDTH + 1, paletteY + h + 1, PANEL_BORDER);
        graphics.fill(paletteX, paletteY, paletteX + PALETTE_WIDTH, paletteY + h, PALETTE_BACKGROUND);
        graphics.text(font, RFMTranslations.ADD_NODE.component(), paletteX + 6, paletteY + 4, TEXT_PRIMARY);
        List<NodeType> types = paletteTypes();
        for (int i = 0; i < types.size(); i++) {
            int rowY = paletteY + PALETTE_HEADER_HEIGHT + i * PALETTE_ROW_HEIGHT;
            boolean hovered = mouseX >= paletteX && mouseX <= paletteX + PALETTE_WIDTH
                    && mouseY >= rowY && mouseY < rowY + PALETTE_ROW_HEIGHT;
            if (hovered) {
                graphics.fill(paletteX, rowY, paletteX + PALETTE_WIDTH, rowY + PALETTE_ROW_HEIGHT, PALETTE_ROW_HOVER);
            }
            graphics.fill(paletteX + 6, rowY + 3, paletteX + 12, rowY + 9, headerColor(types.get(i).category()));
            graphics.text(font, Component.translatable(types.get(i).translationKey()), paletteX + 17, rowY + 2, TEXT_PRIMARY);
        }
    }
}
