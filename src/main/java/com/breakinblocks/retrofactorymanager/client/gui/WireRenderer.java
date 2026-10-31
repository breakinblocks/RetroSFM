package com.breakinblocks.retrofactorymanager.client.gui;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

final class WireRenderer {
    private static final double SEGMENT_LENGTH = 3.0D;
    private static final int MIN_SEGMENTS = 16;
    private static final int MAX_SEGMENTS = 128;

    private WireRenderer() {
    }

    static void draw(
            GuiGraphics graphics,
            double x0,
            double y0,
            double x1,
            double y1,
            double tangent,
            double thickness,
            int color
    ) {
        double c0x = x0 + tangent;
        double c1x = x1 - tangent;
        int segments = (int) Mth.clamp((Math.abs(x1 - x0) + Math.abs(y1 - y0)) / SEGMENT_LENGTH, MIN_SEGMENTS, MAX_SEGMENTS);
        double[] xs = new double[segments + 1];
        double[] ys = new double[segments + 1];
        double[] normalXs = new double[segments + 1];
        double[] normalYs = new double[segments + 1];
        double normalX = 0.0D;
        double normalY = 1.0D;
        for (int i = 0; i <= segments; i++) {
            double t = (double) i / segments;
            double omt = 1.0D - t;
            xs[i] = omt * omt * omt * x0 + 3 * omt * omt * t * c0x + 3 * omt * t * t * c1x + t * t * t * x1;
            ys[i] = omt * omt * omt * y0 + 3 * omt * omt * t * y0 + 3 * omt * t * t * y1 + t * t * t * y1;
            double dx = 3 * omt * omt * (c0x - x0) + 6 * omt * t * (c1x - c0x) + 3 * t * t * (x1 - c1x);
            double dy = 6 * omt * t * (y1 - y0);
            double length = Math.hypot(dx, dy);
            if (length > 1.0E-6D) {
                normalX = -dy / length;
                normalY = dx / length;
            }
            normalXs[i] = normalX;
            normalYs[i] = normalY;
        }
        double feather = 1.0D / Minecraft.getInstance().getWindow().getGuiScale();
        double core = Math.max(thickness / 2.0D - feather / 2.0D, feather / 2.0D);
        double edge = core + feather;
        int transparent = color & 0x00FFFFFF;
        Matrix4f pose = graphics.pose().last().pose();
        VertexConsumer consumer = graphics.bufferSource().getBuffer(RenderType.gui());
        for (int i = 0; i < segments; i++) {
            strip(consumer, pose, xs, ys, normalXs, normalYs, i, -edge, transparent, -core, color);
            strip(consumer, pose, xs, ys, normalXs, normalYs, i, -core, color, core, color);
            strip(consumer, pose, xs, ys, normalXs, normalYs, i, core, color, edge, transparent);
        }
        graphics.flush();
    }

    private static void strip(
            VertexConsumer consumer,
            Matrix4f pose,
            double[] xs,
            double[] ys,
            double[] normalXs,
            double[] normalYs,
            int i,
            double innerOffset,
            int innerColor,
            double outerOffset,
            int outerColor
    ) {
        int j = i + 1;
        float ax = (float) (xs[i] + normalXs[i] * innerOffset);
        float ay = (float) (ys[i] + normalYs[i] * innerOffset);
        float bx = (float) (xs[j] + normalXs[j] * innerOffset);
        float by = (float) (ys[j] + normalYs[j] * innerOffset);
        float cx = (float) (xs[j] + normalXs[j] * outerOffset);
        float cy = (float) (ys[j] + normalYs[j] * outerOffset);
        float dx = (float) (xs[i] + normalXs[i] * outerOffset);
        float dy = (float) (ys[i] + normalYs[i] * outerOffset);
        double area = (ax * by - bx * ay) + (bx * cy - cx * by) + (cx * dy - dx * cy) + (dx * ay - ax * dy);
        if (area < 0) {
            consumer.addVertex(pose, ax, ay, 0.0F).setColor(innerColor);
            consumer.addVertex(pose, bx, by, 0.0F).setColor(innerColor);
            consumer.addVertex(pose, cx, cy, 0.0F).setColor(outerColor);
            consumer.addVertex(pose, dx, dy, 0.0F).setColor(outerColor);
        } else {
            consumer.addVertex(pose, dx, dy, 0.0F).setColor(outerColor);
            consumer.addVertex(pose, cx, cy, 0.0F).setColor(outerColor);
            consumer.addVertex(pose, bx, by, 0.0F).setColor(innerColor);
            consumer.addVertex(pose, ax, ay, 0.0F).setColor(innerColor);
        }
    }
}
