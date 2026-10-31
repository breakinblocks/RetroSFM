package com.breakinblocks.retrofactorymanager.client.gui;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fc;

final class WireRenderer {
    private static final double SEGMENT_LENGTH = 3.0D;
    private static final int MIN_SEGMENTS = 16;
    private static final int MAX_SEGMENTS = 128;
    private static final int STRIPS_PER_SEGMENT = 3;
    private static final int VERTICES_PER_QUAD = 4;

    private WireRenderer() {
    }

    static void draw(
            GuiGraphicsExtractor graphics,
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
        Geometry geometry = new Geometry(segments * STRIPS_PER_SEGMENT * VERTICES_PER_QUAD);
        for (int i = 0; i < segments; i++) {
            geometry.strip(xs, ys, normalXs, normalYs, i, -edge, transparent, -core, color);
            geometry.strip(xs, ys, normalXs, normalYs, i, -core, color, core, color);
            geometry.strip(xs, ys, normalXs, normalYs, i, core, color, edge, transparent);
        }
        Matrix3x2f pose = new Matrix3x2f(graphics.pose());
        ScreenRectangle scissor = graphics.peekScissorStack();
        ScreenRectangle bounds = geometry.bounds().transformMaxBounds(pose);
        graphics.submitGuiElementRenderState(new WireRenderState(
                pose,
                geometry.xs,
                geometry.ys,
                geometry.colors,
                scissor,
                scissor != null ? scissor.intersection(bounds) : bounds
        ));
    }

    private static final class Geometry {
        private final float[] xs;
        private final float[] ys;
        private final int[] colors;
        private int size;
        private float minX = Float.POSITIVE_INFINITY;
        private float minY = Float.POSITIVE_INFINITY;
        private float maxX = Float.NEGATIVE_INFINITY;
        private float maxY = Float.NEGATIVE_INFINITY;

        private Geometry(int capacity) {
            xs = new float[capacity];
            ys = new float[capacity];
            colors = new int[capacity];
        }

        private void strip(
                double[] pointXs,
                double[] pointYs,
                double[] normalXs,
                double[] normalYs,
                int i,
                double innerOffset,
                int innerColor,
                double outerOffset,
                int outerColor
        ) {
            int j = i + 1;
            float ax = (float) (pointXs[i] + normalXs[i] * innerOffset);
            float ay = (float) (pointYs[i] + normalYs[i] * innerOffset);
            float bx = (float) (pointXs[j] + normalXs[j] * innerOffset);
            float by = (float) (pointYs[j] + normalYs[j] * innerOffset);
            float cx = (float) (pointXs[j] + normalXs[j] * outerOffset);
            float cy = (float) (pointYs[j] + normalYs[j] * outerOffset);
            float dx = (float) (pointXs[i] + normalXs[i] * outerOffset);
            float dy = (float) (pointYs[i] + normalYs[i] * outerOffset);
            double area = (ax * by - bx * ay) + (bx * cy - cx * by) + (cx * dy - dx * cy) + (dx * ay - ax * dy);
            if (area < 0) {
                add(ax, ay, innerColor);
                add(bx, by, innerColor);
                add(cx, cy, outerColor);
                add(dx, dy, outerColor);
            } else {
                add(dx, dy, outerColor);
                add(cx, cy, outerColor);
                add(bx, by, innerColor);
                add(ax, ay, innerColor);
            }
        }

        private void add(
                float x,
                float y,
                int color
        ) {
            xs[size] = x;
            ys[size] = y;
            colors[size] = color;
            size++;
            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
            maxX = Math.max(maxX, x);
            maxY = Math.max(maxY, y);
        }

        private ScreenRectangle bounds() {
            int left = Mth.floor(minX);
            int top = Mth.floor(minY);
            return new ScreenRectangle(left, top, Mth.ceil(maxX) - left, Mth.ceil(maxY) - top);
        }
    }

    private record WireRenderState(
            Matrix3x2fc pose,
            float[] xs,
            float[] ys,
            int[] colors,
            @Nullable ScreenRectangle scissorArea,
            @Nullable ScreenRectangle bounds
    ) implements GuiElementRenderState {
        @Override
        public void buildVertices(VertexConsumer vertexConsumer) {
            for (int i = 0; i < xs.length; i++) {
                vertexConsumer.addVertexWith2DPose(pose, xs[i], ys[i]).setColor(colors[i]);
            }
        }

        @Override
        public RenderPipeline pipeline() {
            return RenderPipelines.GUI;
        }

        @Override
        public TextureSetup textureSetup() {
            return TextureSetup.noTexture();
        }
    }
}
