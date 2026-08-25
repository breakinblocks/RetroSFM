package com.breakinblocks.retrofactorymanager.client.gui;

import com.breakinblocks.retrofactorymanager.common.RFMTranslations;
import com.breakinblocks.retrofactorymanager.data.IoSide;
import com.breakinblocks.retrofactorymanager.data.LimitEntry;
import com.breakinblocks.retrofactorymanager.data.RoundRobinMode;
import com.breakinblocks.retrofactorymanager.data.NodeSettings;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public final class NodeSummaries {
    private NodeSummaries() {
    }

    public static List<Component> lines(NodeSettings settings) {
        return switch (settings) {
            case NodeSettings.Timer timer -> List.of(timerLine(timer));
            case NodeSettings.RedstonePulse ignored -> List.of(RFMTranslations.SUMMARY_REDSTONE_PULSE.component());
            case NodeSettings.Io io -> ioLines(io);
            case NodeSettings.If ifSettings -> ifLines(ifSettings);
            case NodeSettings.Forget forget -> List.of(forget.labels().isEmpty()
                    ? RFMTranslations.SUMMARY_ALL_LABELS.component()
                    : Component.literal(String.join(", ", forget.labels())));
            case NodeSettings.Comment comment -> comment.text().isBlank()
                    ? List.of(RFMTranslations.SUMMARY_COMMENT_EMPTY.component())
                    : comment.text().lines().<Component>map(Component::literal).toList();
        };
    }

    private static Component timerLine(NodeSettings.Timer timer) {
        String offset = timer.offset() > 0 ? " + " + timer.offset() : "";
        Component unit = (timer.seconds() ? RFMTranslations.SUMMARY_SECONDS : RFMTranslations.SUMMARY_TICKS).component();
        return (timer.global() ? RFMTranslations.SUMMARY_TIMER_GLOBAL : RFMTranslations.SUMMARY_TIMER)
                .component(String.valueOf(timer.amount()), offset, unit);
    }

    private static List<Component> ioLines(NodeSettings.Io io) {
        boolean output = io instanceof NodeSettings.Output;
        List<Component> lines = new ArrayList<>();
        Component labels = io.labels().isEmpty()
                ? RFMTranslations.SUMMARY_UNSET.component()
                : Component.literal(String.join(", ", io.labels()));
        RFMTranslations.Entry verb = output
                ? (io.each() ? RFMTranslations.SUMMARY_TO_EACH : RFMTranslations.SUMMARY_TO)
                : (io.each() ? RFMTranslations.SUMMARY_FROM_EACH : RFMTranslations.SUMMARY_FROM);
        lines.add(verb.component(labels));
        if (output && ((NodeSettings.Output) io).emptySlotsOnly()) {
            lines.add(RFMTranslations.SUMMARY_EMPTY_SLOTS.component());
        }
        for (LimitEntry entry : io.activeLimits()) {
            String summary = entry.summary();
            if (entry.resources().size() > 2) {
                String head = entry.resources().getFirst();
                summary = summary.replace(
                        String.join(" OR ", entry.resources()),
                        head + " OR +" + (entry.resources().size() - 1)
                );
            }
            lines.add(Component.literal(summary));
        }
        if (!io.except().isEmpty()) {
            lines.add(RFMTranslations.SUMMARY_EXCEPT.component(String.join(", ", io.except())));
        }
        if (io.roundRobin() != RoundRobinMode.NONE) {
            lines.add(RFMTranslations.ROUND_ROBIN.component(
                    Component.translatable(io.roundRobin().translationKey())));
        }
        if (io.eachSide()) {
            lines.add(RFMTranslations.SUMMARY_EACH_SIDE.component());
        } else if (!io.sides().isEmpty()) {
            String joined = String.join(", ", io.sides().stream()
                    .map(side -> Component.translatable(side.translationKey()).getString())
                    .toList());
            lines.add(RFMTranslations.SUMMARY_SIDES.component(joined));
        }
        if (!io.slots().isBlank()) {
            lines.add(RFMTranslations.SUMMARY_SLOTS.component(io.slots()));
        }
        return lines;
    }

    private static List<Component> ifLines(NodeSettings.If ifSettings) {
        if (ifSettings.useText()) {
            return List.of(ifSettings.condition().isBlank()
                    ? RFMTranslations.SUMMARY_NO_CONDITION.component()
                    : Component.literal(ifSettings.condition()));
        }
        if (ifSettings.rows().isEmpty()) {
            return List.of(Component.literal("TRUE"));
        }
        return List.of(Component.literal(ifSettings.rows().stream()
                .map(row -> (row.negate() ? "NOT " : "") + row.toSfml())
                .reduce((a, b) -> a + (ifSettings.joinOr() ? " OR " : " AND ") + b)
                .orElse("TRUE")));
    }

    public static Component sideShort(IoSide side) {
        return Component.translatable(side.translationKey());
    }
}
