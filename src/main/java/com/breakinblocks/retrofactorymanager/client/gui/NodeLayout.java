package com.breakinblocks.retrofactorymanager.client.gui;

import com.breakinblocks.retrofactorymanager.data.BlueprintNode;
import com.breakinblocks.retrofactorymanager.data.PinRole;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public record NodeLayout(
        double width,
        double height,
        double headerHeight,
        List<Component> summaryLines,
        List<PinSpot> pins
) {
    public static final double HEADER_HEIGHT = 14;
    public static final double LINE_HEIGHT = 11;
    public static final double PIN_ROW_HEIGHT = 13;
    public static final double MIN_WIDTH = 90;
    public static final double MAX_WIDTH = 240;
    private static final int SUMMARY_LEFT = 8;
    private static final int RIGHT_PADDING = 12;
    private static final int LABEL_GAP = 8;
    private static final String ELLIPSIS = "...";

    public record PinSpot(PinRole role, double dx, double dy) {
    }

    public static NodeLayout of(BlueprintNode node, Font font) {
        List<Component> summary = NodeSummaries.lines(node.settings());
        List<PinRole> outputs = node.type().outputPins();
        double width = font.width(Component.translatable(node.type().translationKey())) + 16;
        for (int i = 0; i < summary.size(); i++) {
            width = Math.max(width, SUMMARY_LEFT + font.width(summary.get(i)) + rightReserve(i, outputs, font));
        }
        for (PinRole output : outputs) {
            if (output.hasLabel()) {
                width = Math.max(width, font.width(Component.translatable(output.labelKey())) + 70);
            }
        }
        width = Math.clamp(width, MIN_WIDTH, MAX_WIDTH);

        List<Component> fitted = new ArrayList<>(summary.size());
        for (int i = 0; i < summary.size(); i++) {
            int available = (int) width - SUMMARY_LEFT - rightReserve(i, outputs, font);
            fitted.add(fit(summary.get(i), available, font));
        }

        int pinRows = Math.max(outputs.size(), node.type().hasPin(PinRole.EXEC_IN) ? 1 : 0);
        double bodyHeight = Math.max(summary.size() * LINE_HEIGHT, pinRows * PIN_ROW_HEIGHT) + 9;
        double height = HEADER_HEIGHT + bodyHeight;

        List<PinSpot> pins = new ArrayList<>();
        if (node.type().hasPin(PinRole.EXEC_IN)) {
            pins.add(new PinSpot(PinRole.EXEC_IN, 0, HEADER_HEIGHT + 10));
        }
        for (int i = 0; i < outputs.size(); i++) {
            pins.add(new PinSpot(outputs.get(i), width, HEADER_HEIGHT + 10 + i * PIN_ROW_HEIGHT));
        }
        return new NodeLayout(width, height, HEADER_HEIGHT, fitted, pins);
    }

    private static int rightReserve(int line, List<PinRole> outputs, Font font) {
        double lineTop = 5 + line * LINE_HEIGHT;
        int widest = 0;
        for (int row = 0; row < outputs.size(); row++) {
            PinRole output = outputs.get(row);
            if (!output.hasLabel()) {
                continue;
            }
            double labelTop = 6 + row * PIN_ROW_HEIGHT;
            if (lineTop < labelTop + font.lineHeight && labelTop < lineTop + font.lineHeight) {
                widest = Math.max(widest, font.width(Component.translatable(output.labelKey())));
            }
        }
        return widest == 0 ? RIGHT_PADDING : 8 + widest + LABEL_GAP;
    }

    private static Component fit(Component line, int maxWidth, Font font) {
        if (font.width(line) <= maxWidth) {
            return line;
        }
        String cut = font.plainSubstrByWidth(line.getString(), Math.max(0, maxWidth - font.width(ELLIPSIS)));
        return Component.literal(cut.stripTrailing() + ELLIPSIS);
    }

    public boolean contains(BlueprintNode node, double canvasX, double canvasY) {
        return canvasX >= node.x() && canvasX <= node.x() + width
                && canvasY >= node.y() && canvasY <= node.y() + height;
    }
}
