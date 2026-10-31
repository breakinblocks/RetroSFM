package com.breakinblocks.retrofactorymanager.client.gui;

import ca.teamdman.sfm.client.registry.SFMTextEditors;
import ca.teamdman.sfm.client.screen.SFMScreenChangeHelpers;
import ca.teamdman.sfm.client.screen.text_editor.ISFMTextEditScreen;
import ca.teamdman.sfm.client.screen.text_editor.SFMTextEditScreenV1;
import ca.teamdman.sfm.client.screen.text_editor.SFMTextEditScreenV2;
import ca.teamdman.sfm.client.text_editor.TextEditContext;
import com.breakinblocks.retrofactorymanager.RetroFactoryManager;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

@EventBusSubscriber(modid = RetroFactoryManager.MOD_ID, value = Dist.CLIENT)
public final class RFMSfmEditorScreenTweaks {
    private static final int SWITCHER_WIDTH = 110;
    private static final int GAP = 4;
    private static final boolean UPSTREAM_SWITCHER_PRESENT =
            classExists("ca.teamdman.sfm.client.screen.widget.SFMPreferredEditorDropdown");

    private RFMSfmEditorScreenTweaks() {
    }

    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (UPSTREAM_SWITCHER_PRESENT) {
            return;
        }
        Screen screen = event.getScreen();
        List<GuiEventListener> listeners = event.getListenersList();
        ResourceLocation shownEditorId;
        Supplier<String> latestContent;
        if (screen instanceof SFMTextEditScreenV1) {
            shownEditorId = SFMTextEditors.V1.getId().orElseThrow().location();
            latestContent = v1Content(listeners);
        } else if (screen instanceof SFMTextEditScreenV2 v2) {
            shownEditorId = SFMTextEditors.V2.getId().orElseThrow().location();
            latestContent = v2Content(v2);
        } else {
            return;
        }
        Button configButton = findButton(listeners, button -> "#".equals(button.getMessage().getString()));
        if (latestContent == null || configButton == null) {
            return;
        }
        int x = configButton.getX() + configButton.getWidth() + GAP;
        int y = configButton.getY();
        Button done = findButton(listeners, button -> hasTranslationKey(button, "gui.done"));
        Button cancel = findButton(listeners, button -> hasTranslationKey(button, "gui.cancel"));
        if (done != null && cancel != null) {
            int rowRight = cancel.getX() + cancel.getWidth();
            int buttonWidth = (rowRight - x - SWITCHER_WIDTH - 2 * GAP) / 2;
            done.setX(x + SWITCHER_WIDTH + GAP);
            done.setWidth(buttonWidth);
            cancel.setX(rowRight - buttonWidth);
            cancel.setWidth(buttonWidth);
        }
        event.addListener(RFMEditorSwitcher.createForEditor(
                x,
                y,
                SWITCHER_WIDTH,
                configButton.getHeight(),
                (ISFMTextEditScreen) screen,
                shownEditorId,
                latestContent
        ));
    }

    static boolean returnToPreviousScreen(ISFMTextEditScreen editor) {
        if (!(editor instanceof SFMTextEditScreenV2) || V2Fields.PREVIOUS_SCREEN == null) {
            return false;
        }
        SFMScreenChangeHelpers.setScreen((Screen) readField(V2Fields.PREVIOUS_SCREEN, editor));
        return true;
    }

    private static @Nullable Supplier<String> v1Content(List<GuiEventListener> listeners) {
        for (GuiEventListener listener : listeners) {
            if (listener instanceof MultiLineEditBox box) {
                return box::getValue;
            }
        }
        return null;
    }

    private static @Nullable Supplier<String> v2Content(SFMTextEditScreenV2 screen) {
        Field field = V2Fields.TEXT_EDIT_CONTEXT;
        if (field == null) {
            return null;
        }
        return () -> ((TextEditContext) readField(field, screen)).getContent();
    }

    private static @Nullable Button findButton(
            List<GuiEventListener> listeners,
            Predicate<Button> predicate
    ) {
        for (GuiEventListener listener : listeners) {
            if (listener instanceof Button button && predicate.test(button)) {
                return button;
            }
        }
        return null;
    }

    private static boolean hasTranslationKey(
            Button button,
            String key
    ) {
        return button.getMessage().getContents() instanceof TranslatableContents translatable
                && key.equals(translatable.getKey());
    }

    private static boolean classExists(String name) {
        try {
            Class.forName(name, false, RFMSfmEditorScreenTweaks.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException | LinkageError e) {
            return false;
        }
    }

    private static Object readField(
            Field field,
            Object owner
    ) {
        try {
            return field.get(owner);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    private static final class V2Fields {
        static final @Nullable Field TEXT_EDIT_CONTEXT = find("textEditContext");
        static final @Nullable Field PREVIOUS_SCREEN = find("previousScreen");

        private static @Nullable Field find(String name) {
            try {
                Field field = SFMTextEditScreenV2.class.getDeclaredField(name);
                field.setAccessible(true);
                return field;
            } catch (ReflectiveOperationException | RuntimeException e) {
                RetroFactoryManager.LOGGER.warn("Could not access SFMTextEditScreenV2.{}", name, e);
                return null;
            }
        }
    }
}
