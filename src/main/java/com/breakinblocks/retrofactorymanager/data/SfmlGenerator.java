package com.breakinblocks.retrofactorymanager.data;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class SfmlGenerator {
    private static final String INDENT = "    ";
    private static final Set<String> KEYWORDS = Set.of(
            "IF", "THEN", "ELSE", "HAS", "OVERALL", "SOME", "ONE", "LONE", "TRUE", "FALSE",
            "NOT", "AND", "OR", "GT", "LT", "EQ", "LE", "GE",
            "FROM", "TO", "INPUT", "OUTPUT", "WHERE", "SLOTS", "SLOT", "RETAIN", "EACH",
            "EXCEPT", "FORGET", "EMPTY", "IN", "WITHOUT", "WITH", "TAG",
            "ROUND", "ROBIN", "BY", "LABEL", "BLOCK",
            "TOP", "BOTTOM", "NORTH", "EAST", "SOUTH", "WEST", "SIDE",
            "LEFT", "RIGHT", "FRONT", "BACK", "NULL",
            "TICKS", "TICK", "SECONDS", "SECOND", "GLOBAL", "G", "PLUS",
            "REDSTONE", "PULSE", "DO", "END", "NAME", "EVERY"
    );

    private SfmlGenerator() {
    }

    /// Generated text plus, for each 1-based line, the node that produced it.
    public record Generated(String text, Map<Integer, UUID> lineOwners) {
    }

    public static String generate(BlueprintGraph graph) {
        return generateDetailed(graph).text();
    }

    public static Generated generateDetailed(BlueprintGraph graph) {
        Emitter emitter = new Emitter();
        if (!graph.name().isBlank()) {
            emitter.line("NAME " + '"' + graph.name().replace("\"", "") + '"', null);
            emitter.blank();
        }
        List<BlueprintNode> triggers = graph.triggers();
        for (int i = 0; i < triggers.size(); i++) {
            BlueprintNode trigger = triggers.get(i);
            if (i > 0) {
                emitter.blank();
            }
            emitter.line(triggerHeader(trigger), trigger.id());
            emitChain(graph, graph.edgeFrom(trigger.id(), PinRole.EXEC_OUT), 1, emitter);
            emitter.line("END", trigger.id());
        }
        return emitter.finish();
    }

    /// Line-oriented sink that remembers which node emitted each line.
    private static final class Emitter {
        private final StringBuilder sb = new StringBuilder();
        private final Map<Integer, UUID> owners = new HashMap<>();
        private int line = 1;

        void line(String text, UUID owner) {
            if (owner != null) {
                owners.put(line, owner);
            }
            sb.append(text).append('\n');
            line++;
        }

        void blank() {
            sb.append('\n');
            line++;
        }

        Generated finish() {
            String text = sb.toString();
            String stripped = text.strip();
            int leadingBlankLines = 0;
            for (int i = 0; i < text.length() && Character.isWhitespace(text.charAt(i)); i++) {
                if (text.charAt(i) == '\n') {
                    leadingBlankLines++;
                }
            }
            if (leadingBlankLines == 0) {
                return new Generated(stripped, Map.copyOf(owners));
            }
            Map<Integer, UUID> shifted = new HashMap<>();
            int shift = leadingBlankLines;
            owners.forEach((key, value) -> shifted.put(key - shift, value));
            return new Generated(stripped, Map.copyOf(shifted));
        }
    }

    private static String triggerHeader(BlueprintNode trigger) {
        if (trigger.settings() instanceof NodeSettings.Timer timer) {
            StringBuilder sb = new StringBuilder("EVERY ").append(timer.amount());
            if (timer.global()) {
                sb.append(" GLOBAL");
            }
            if (timer.offset() > 0) {
                sb.append(" + ").append(timer.offset());
            }
            sb.append(timer.seconds() ? " SECONDS" : " TICKS");
            return sb.append(" DO").toString();
        }
        return "EVERY REDSTONE PULSE DO";
    }

    private static void emitChain(BlueprintGraph graph, Optional<BlueprintEdge> firstEdge, int depth, Emitter emitter) {
        Optional<BlueprintEdge> edge = firstEdge;
        while (edge.isPresent()) {
            BlueprintNode node = graph.node(edge.get().toNode());
            if (node == null) {
                return;
            }
            String indent = INDENT.repeat(depth);
            UUID owner = node.id();
            switch (node.settings()) {
                case NodeSettings.Input input -> {
                    StringBuilder sb = new StringBuilder(indent).append("INPUT");
                    appendLimits(sb, input);
                    sb.append(" FROM ")
                            .append(input.each() ? "EACH " : "")
                            .append(labelAccess(input.labels()));
                    appendRoundRobin(sb, input);
                    appendSides(sb, input);
                    appendSlots(sb, input);
                    emitter.line(sb.toString(), owner);
                }
                case NodeSettings.Output output -> {
                    StringBuilder sb = new StringBuilder(indent).append("OUTPUT");
                    appendLimits(sb, output);
                    sb.append(" TO ")
                            .append(output.emptySlotsOnly() ? "EMPTY SLOTS IN " : "")
                            .append(output.each() ? "EACH " : "")
                            .append(labelAccess(output.labels()));
                    appendRoundRobin(sb, output);
                    appendSides(sb, output);
                    appendSlots(sb, output);
                    emitter.line(sb.toString(), owner);
                }
                case NodeSettings.Forget forget -> {
                    StringBuilder sb = new StringBuilder(indent).append("FORGET");
                    if (!forget.labels().isEmpty()) {
                        sb.append(" ").append(labelAccess(forget.labels()));
                    }
                    emitter.line(sb.toString(), owner);
                }
                case NodeSettings.Comment comment -> comment.text().lines()
                        .forEach(line -> emitter.line(indent + "-- " + line, owner));
                case NodeSettings.If ifSettings -> {
                    emitter.line(indent + "IF " + ifSettings.conditionSfml() + " THEN", owner);
                    emitChain(graph, graph.edgeFrom(node.id(), PinRole.TRUE_OUT), depth + 1, emitter);
                    Optional<BlueprintEdge> falseEdge = graph.edgeFrom(node.id(), PinRole.FALSE_OUT);
                    if (falseEdge.isPresent()) {
                        emitter.line(indent + "ELSE", owner);
                        emitChain(graph, falseEdge, depth + 1, emitter);
                    }
                    emitter.line(indent + "END", owner);
                }
                default -> {
                }
            }
            edge = graph.edgeFrom(node.id(), node.type().continuationPin());
        }
    }

    private static void appendLimits(StringBuilder sb, NodeSettings.Io io) {
        List<String> clauses = io.activeLimits().stream().map(LimitEntry::toSfml).toList();
        if (!clauses.isEmpty()) {
            sb.append(" ").append(String.join(", ", clauses));
        }
        if (!io.except().isEmpty()) {
            sb.append(" EXCEPT ").append(String.join(", ", io.except()));
        }
    }

    private static void appendRoundRobin(StringBuilder sb, NodeSettings.Io io) {
        if (io.roundRobin() != RoundRobinMode.NONE) {
            sb.append(" ").append(io.roundRobin().sfml());
        }
    }

    private static void appendSides(StringBuilder sb, NodeSettings.Io io) {
        if (io.eachSide()) {
            sb.append(" EACH SIDE");
        } else if (!io.sides().isEmpty()) {
            sb.append(" ")
                    .append(String.join(", ", io.sides().stream().map(Enum::name).toList()))
                    .append(" SIDE");
        }
    }

    private static void appendSlots(StringBuilder sb, NodeSettings.Io io) {
        if (!io.slots().isBlank()) {
            sb.append(" SLOTS ").append(io.slots().trim());
        }
    }

    private static String labelAccess(List<String> labels) {
        if (labels.isEmpty()) {
            return "_unset";
        }
        return String.join(", ", labels.stream().map(SfmlGenerator::quoteLabel).toList());
    }

    static String quoteLabel(String label) {
        if (label.matches("[a-zA-Z_][a-zA-Z0-9_]*") && !KEYWORDS.contains(label.toUpperCase(Locale.ROOT))) {
            return label;
        }
        return '"' + label.replace("\\", "\\\\").replace("\"", "\\\"") + '"';
    }
}
