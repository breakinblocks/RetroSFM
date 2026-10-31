package com.breakinblocks.retrofactorymanager.client.gui;

import ca.teamdman.sfm.client.registry.SFMTextEditors;
import ca.teamdman.sfm.client.screen.SFMScreenChangeHelpers;
import ca.teamdman.sfm.client.screen.text_editor.ISFMTextEditScreen;
import ca.teamdman.sfm.client.text_editor.ISFMTextEditScreenOpenContext;
import ca.teamdman.sfm.client.text_editor.ISFMTextEditorRegistration;
import ca.teamdman.sfm.common.config.SFMConfig;
import ca.teamdman.sfm.common.label.LabelPositionHolder;
import ca.teamdman.sfm.common.util.SFMResourceLocation;
import com.breakinblocks.retrofactorymanager.common.RFMTranslations;
import com.breakinblocks.retrofactorymanager.config.RFMConfig;
import com.breakinblocks.retrofactorymanager.integration.RFMTextEditors;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class RFMEditorSwitcher {
    private static final int PANEL_PADDING = 3;
    private static final int PANEL_BACKGROUND = 0xFF141414;
    private static final int PANEL_BORDER = 0xFF8B8B8B;
    private static final int ROW_HOVER_BACKGROUND = 0x40FFFFFF;
    private static final int TEXT_COLOR = 0xFFE0E0E0;
    private static final int TEXT_COLOR_HOVERED = 0xFFFFFFFF;
    private static final int TEXT_COLOR_CURRENT = 0xFFFFD83D;

    private RFMEditorSwitcher() {
    }

    public static Button createForEditor(
            int x,
            int y,
            int width,
            int height,
            ISFMTextEditScreen editorScreen,
            ResourceLocation shownEditorId,
            Supplier<String> latestContent
    ) {
        return Button.builder(
                        buttonMessage(shownEditorId),
                        button -> SFMScreenChangeHelpers.setOrPushScreen(new PickerScreen(
                                x,
                                y,
                                width,
                                height,
                                shownEditorId,
                                id -> switchEditor(editorScreen, latestContent.get())
                        )))
                .pos(x, y)
                .size(width, height)
                .build();
    }

    private static void switchEditor(
            ISFMTextEditScreen editorScreen,
            String latestContent
    ) {
        ISFMTextEditScreenOpenContext oldContext = editorScreen.openContext();
        ISFMTextEditScreenOpenContext newContext = new ISFMTextEditScreenOpenContext() {
            @Override
            public String initialValue() {
                return latestContent;
            }

            @Override
            public void onTryClose(
                    String newLatestContent,
                    Runnable finalizeClose
            ) {
                oldContext.onTryClose(newLatestContent, finalizeClose);
            }

            @Override
            public void onSaveAndClose(String newLatestContent) {
                oldContext.onSaveAndClose(newLatestContent);
            }

            @Override
            public Consumer<String> saveWriter() {
                return oldContext.saveWriter();
            }

            @Override
            public LabelPositionHolder labelPositionHolder() {
                return oldContext.labelPositionHolder();
            }
        };
        boolean returnedToPrevious = switch (editorScreen.openBehaviour()) {
            case Push -> {
                SFMScreenChangeHelpers.popScreen();
                yield true;
            }
            case Replace -> RFMSfmEditorScreenTweaks.returnToPreviousScreen(editorScreen);
        };
        ISFMTextEditScreen newScreen = SFMScreenChangeHelpers.createProgramEditScreen(newContext);
        if (returnedToPrevious) {
            SFMScreenChangeHelpers.showTextEditScreen(newScreen);
        } else {
            SFMScreenChangeHelpers.setScreen(newScreen.asScreen());
        }
    }

    static Component getDisplayName(ResourceLocation id) {
        return Component.translatableWithFallback(
                "gui.sfm.preferred_editor." + id.getNamespace() + "." + id.getPath(),
                id.toString()
        );
    }

    static List<ResourceLocation> getEditorIds() {
        List<ResourceLocation> editorIds = new ArrayList<>();
        for (ISFMTextEditorRegistration registration : SFMTextEditors.registry()) {
            ResourceLocation id = SFMTextEditors.registry().getId(registration);
            if (id != null) {
                editorIds.add(id);
            }
        }
        return editorIds;
    }

    static ResourceLocation getCurrentEditorId() {
        List<ResourceLocation> editorIds = getEditorIds();
        @Nullable ResourceLocation current = SFMResourceLocation.tryParse(
                SFMConfig.CLIENT_TEXT_EDITOR_CONFIG.preferredEditor.get()
        );
        if (current == null || !editorIds.contains(current)) {
            current = editorIds.getFirst();
        }
        return current;
    }

    private static Component buttonMessage(ResourceLocation id) {
        return getDisplayName(id).copy().append(" ▼");
    }

    static void applyEditorSelection(ResourceLocation id) {
        var preferredEditor = SFMConfig.CLIENT_TEXT_EDITOR_CONFIG.preferredEditor;
        if (!id.toString().equals(preferredEditor.get())) {
            preferredEditor.set(id.toString());
            preferredEditor.save();
        }
        boolean isBlueprint = RFMTextEditors.BLUEPRINT.getId().equals(id);
        if (RFMConfig.CLIENT.setBlueprintAsDefaultEditor.get() != isBlueprint) {
            RFMConfig.CLIENT.setBlueprintAsDefaultEditor.set(isBlueprint);
            RFMConfig.CLIENT.setBlueprintAsDefaultEditor.save();
        }
    }

    private static class PickerScreen extends Screen {
        private final int anchorX;
        private final int anchorY;
        private final int anchorWidth;
        private final int anchorHeight;
        private final ResourceLocation currentEditorId;
        private final Consumer<ResourceLocation> onSelect;
        private List<ResourceLocation> editorIds = List.of();
        private int popupX;
        private int popupY;
        private int popupWidth;
        private int popupHeight;
        private int rowHeight;

        private PickerScreen(
                int anchorX,
                int anchorY,
                int anchorWidth,
                int anchorHeight,
                ResourceLocation currentEditorId,
                Consumer<ResourceLocation> onSelect
        ) {
            super(RFMTranslations.SFM_PREFERRED_EDITOR.component());
            this.anchorX = anchorX;
            this.anchorY = anchorY;
            this.anchorWidth = anchorWidth;
            this.anchorHeight = anchorHeight;
            this.currentEditorId = currentEditorId;
            this.onSelect = onSelect;
        }

        @Override
        protected void init() {
            super.init();
            editorIds = getEditorIds();
            rowHeight = this.font.lineHeight + 4;
            popupWidth = anchorWidth;
            for (ResourceLocation id : editorIds) {
                popupWidth = Math.max(popupWidth, this.font.width(getDisplayName(id)) + 2 * PANEL_PADDING + 8);
            }
            popupHeight = editorIds.size() * rowHeight + 2 * PANEL_PADDING;
            boolean dropUp = anchorY + anchorHeight + popupHeight + 2 > this.height;
            popupX = Math.max(0, Math.min(anchorX, this.width - popupWidth));
            popupY = dropUp ? anchorY - popupHeight - 2 : anchorY + anchorHeight + 2;
        }

        @Override
        public void render(
                GuiGraphics graphics,
                int mouseX,
                int mouseY,
                float partialTick
        ) {
            super.render(graphics, mouseX, mouseY, partialTick);
            graphics.fill(
                    popupX - 1,
                    popupY - 1,
                    popupX + popupWidth + 1,
                    popupY + popupHeight + 1,
                    PANEL_BORDER
            );
            graphics.fill(
                    popupX,
                    popupY,
                    popupX + popupWidth,
                    popupY + popupHeight,
                    PANEL_BACKGROUND
            );
            int hoveredIndex = rowIndexAt(mouseX, mouseY);
            for (int i = 0; i < editorIds.size(); i++) {
                ResourceLocation id = editorIds.get(i);
                int rowY = popupY + PANEL_PADDING + i * rowHeight;
                if (i == hoveredIndex) {
                    graphics.fill(
                            popupX + 1,
                            rowY,
                            popupX + popupWidth - 1,
                            rowY + rowHeight,
                            ROW_HOVER_BACKGROUND
                    );
                }
                int color = id.equals(currentEditorId)
                            ? TEXT_COLOR_CURRENT
                            : i == hoveredIndex ? TEXT_COLOR_HOVERED : TEXT_COLOR;
                graphics.drawString(this.font, getDisplayName(id), popupX + PANEL_PADDING + 4, rowY + 2, color);
            }
        }

        @Override
        public void renderBackground(
                GuiGraphics graphics,
                int mouseX,
                int mouseY,
                float partialTick
        ) {
        }

        private int rowIndexAt(
                double mouseX,
                double mouseY
        ) {
            if (mouseX < popupX || mouseX >= popupX + popupWidth) {
                return -1;
            }
            if (mouseY < popupY + PANEL_PADDING) {
                return -1;
            }
            int index = (int) ((mouseY - popupY - PANEL_PADDING) / rowHeight);
            return index >= 0 && index < editorIds.size() ? index : -1;
        }

        @Override
        public boolean mouseClicked(
                double mouseX,
                double mouseY,
                int button
        ) {
            int index = rowIndexAt(mouseX, mouseY);
            this.onClose();
            if (index >= 0) {
                ResourceLocation id = editorIds.get(index);
                applyEditorSelection(id);
                if (!id.equals(currentEditorId)) {
                    onSelect.accept(id);
                }
            }
            return true;
        }

        @Override
        public void onClose() {
            SFMScreenChangeHelpers.popScreen();
        }
    }
}
