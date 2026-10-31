package com.breakinblocks.retrofactorymanager.client.gui;

import ca.teamdman.sfm.client.screen.SFMTextEditorConfigScreen;
import com.breakinblocks.retrofactorymanager.RetroFactoryManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@EventBusSubscriber(modid = RetroFactoryManager.MOD_ID, value = Dist.CLIENT)
public final class RFMEditorConfigScreenTweaks {
    private static final int BUTTON_WIDTH = 100;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_SPACING = 10;
    private static final int ROW_SPACING = 4;
    private static final Set<String> SFM_EDITOR_BUTTON_KEYS = Set.of(
            "gui.sfm.program_editor_config.preferred_editor.v1",
            "gui.sfm.program_editor_config.preferred_editor.v2",
            "gui.sfm.program_editor_config.preferred_editor.draw"
    );

    private RFMEditorConfigScreenTweaks() {
    }

    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof SFMTextEditorConfigScreen screen)) {
            return;
        }
        int rowX = screen.width / 2 - (3 * BUTTON_WIDTH) / 2 - BUTTON_SPACING;
        int rowY = screen.height / 2 + 50;
        boolean anchored = false;
        for (GuiEventListener listener : List.copyOf(event.getListenersList())) {
            if (listener instanceof Button button && isSfmEditorButton(button)) {
                if (!anchored) {
                    rowX = button.getX();
                    rowY = button.getY();
                    anchored = true;
                }
                event.removeListener(button);
            }
        }
        if (!anchored) {
            event.addListener(new StringWidget(
                    screen.width / 2 - 150,
                    rowY - 15,
                    300,
                    9,
                    Component.translatable("gui.sfm.program_editor_config.preferred_editor"),
                    Minecraft.getInstance().font
            ));
        }
        Map<Identifier, Button> row = new LinkedHashMap<>();
        List<Identifier> editorIds = RFMEditorSwitcher.getEditorIds();
        for (int i = 0; i < editorIds.size(); i++) {
            Identifier id = editorIds.get(i);
            Button button = Button.builder(RFMEditorSwitcher.getDisplayName(id), pressed -> {
                        RFMEditorSwitcher.applyEditorSelection(id);
                        updateActiveStates(row);
                    })
                    .pos(
                            rowX + (i % 3) * (BUTTON_WIDTH + BUTTON_SPACING),
                            rowY + (i / 3) * (BUTTON_HEIGHT + ROW_SPACING)
                    )
                    .size(BUTTON_WIDTH, BUTTON_HEIGHT)
                    .build();
            row.put(id, button);
            event.addListener(button);
        }
        updateActiveStates(row);
    }

    private static void updateActiveStates(Map<Identifier, Button> row) {
        Identifier current = RFMEditorSwitcher.getCurrentEditorId();
        row.forEach((id, button) -> button.active = !id.equals(current));
    }

    private static boolean isSfmEditorButton(Button button) {
        return button.getMessage().getContents() instanceof TranslatableContents translatable
                && SFM_EDITOR_BUTTON_KEYS.contains(translatable.getKey());
    }
}
