package net.saturn.chainmod.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import net.saturn.chainmod.ChainData;
import net.saturn.chainmod.ChainMod;
import org.joml.Matrix4f;

import java.util.List;

public class ChainModClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        // Register world render event for drawing chains
        WorldRenderEvents.AFTER_ENTITIES.register(this::renderChains);
    }

    private void renderChains(WorldRenderContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;

        MatrixStack matrices = new MatrixStack();
        Camera camera = context.gameRenderer().getCamera();
        Vec3d cameraPos = camera.getPos();

        // Get chains for all nearby players
        for (PlayerEntity player : client.world.getPlayers()) {
            List<ChainData> chains = ChainMod.getChains(player);

            for (ChainData chain : chains) {
                renderChain(matrices, cameraPos, chain, context.worldRenderer().getBufferBuilders().getEntityVertexConsumers());
            }
        }
    }

    private void renderChain(MatrixStack matrices, Vec3d cameraPos,
                             ChainData chain, VertexConsumerProvider consumers) {
        matrices.push();

        // Offset by camera position for proper world rendering
        matrices.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

        VertexConsumer buffer = consumers.getBuffer(RenderLayer.getLines());
        Matrix4f matrix = matrices.peek().getPositionMatrix();

        List<Vec3d> segments = chain.getSegments();

        // Draw chain as connected line segments
        for (int i = 0; i < segments.size() - 1; i++) {
            Vec3d current = segments.get(i);
            Vec3d next = segments.get(i + 1);

            // Draw main chain line (dark gray/iron color)
            buffer.vertex(matrix, (float) current.x, (float) current.y, (float) current.z)
                    .color(0.4f, 0.4f, 0.4f, 1.0f)
                    .normal(0, 1, 0);

            buffer.vertex(matrix, (float) next.x, (float) next.y, (float) next.z)
                    .color(0.4f, 0.4f, 0.4f, 1.0f)
                    .normal(0, 1, 0);

            // Draw chain links (thicker visualization)
            drawChainLink(buffer, matrix, current);
        }

        // Draw final link
        Vec3d lastSegment = segments.get(segments.size() - 1);
        drawChainLink(buffer, matrix, lastSegment);

        matrices.pop();
    }

    private void drawChainLink(VertexConsumer buffer, Matrix4f matrix, Vec3d pos) {
        float size = 0.08f;
        float x = (float) pos.x;
        float y = (float) pos.y;
        float z = (float) pos.z;

        // Draw a small cube outline at each chain segment
        // Bottom square
        addLine(buffer, matrix, x - size, y - size, z - size, x + size, y - size, z - size, 0.3f);
        addLine(buffer, matrix, x + size, y - size, z - size, x + size, y - size, z + size, 0.3f);
        addLine(buffer, matrix, x + size, y - size, z + size, x - size, y - size, z + size, 0.3f);
        addLine(buffer, matrix, x - size, y - size, z + size, x - size, y - size, z - size, 0.3f);

        // Top square
        addLine(buffer, matrix, x - size, y + size, z - size, x + size, y + size, z - size, 0.5f);
        addLine(buffer, matrix, x + size, y + size, z - size, x + size, y + size, z + size, 0.5f);
        addLine(buffer, matrix, x + size, y + size, z + size, x - size, y + size, z + size, 0.5f);
        addLine(buffer, matrix, x - size, y + size, z + size, x - size, y + size, z - size, 0.5f);

        // Vertical lines
        addLine(buffer, matrix, x - size, y - size, z - size, x - size, y + size, z - size, 0.4f);
        addLine(buffer, matrix, x + size, y - size, z - size, x + size, y + size, z - size, 0.4f);
        addLine(buffer, matrix, x + size, y - size, z + size, x + size, y + size, z + size, 0.4f);
        addLine(buffer, matrix, x - size, y - size, z + size, x - size, y + size, z + size, 0.4f);
    }

    private void addLine(VertexConsumer buffer, Matrix4f matrix,
                         float x1, float y1, float z1, float x2, float y2, float z2, float brightness) {
        buffer.vertex(matrix, x1, y1, z1)
                .color(brightness, brightness, brightness, 1.0f)
                .normal(0, 1, 0);
        buffer.vertex(matrix, x2, y2, z2)
                .color(brightness, brightness, brightness, 1.0f)
                .normal(0, 1, 0);
    }
}