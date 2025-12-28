package com.example.chainmod;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class ChainMod implements ModInitializer {
    public static final String MOD_ID = "chainmod";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    // Store active chains: Map<playerId, List<ChainData>>
    private static final Map<UUID, List<ChainData>> activeChains = new HashMap<>();

    @Override
    public void onInitialize() {
        LOGGER.info("Chain Mod initialized!");

        // Register tick event to update chains
        ServerTickEvents.END_WORLD_TICK.register(this::onWorldTick);
    }

    private void onWorldTick(ServerWorld world) {
        // Update all active chains
        for (Map.Entry<UUID, List<ChainData>> entry : activeChains.entrySet()) {
            Entity entity = world.getEntity(entry.getKey());
            if (entity instanceof PlayerEntity player) {
                for (ChainData chain : entry.getValue()) {
                    chain.update(player);
                }
            }
        }
    }

    // Method to create a chain between two points
    public static void createChain(PlayerEntity player, Vec3d startPos, Vec3d endPos) {
        ChainData chain = new ChainData(startPos, endPos, 16); // 16 segments
        activeChains.computeIfAbsent(player.getUuid(), k -> new ArrayList<>()).add(chain);
    }

    // Method to create a chain attached to player
    public static void createChainToPlayer(PlayerEntity player, Vec3d anchorPos) {
        ChainData chain = new ChainData(anchorPos, player.getPos(), 16);
        chain.setFollowPlayer(true);
        activeChains.computeIfAbsent(player.getUuid(), k -> new ArrayList<>()).add(chain);
    }

    public static void removeAllChains(PlayerEntity player) {
        activeChains.remove(player.getUuid());
    }

    public static List<ChainData> getChains(PlayerEntity player) {
        return activeChains.getOrDefault(player.getUuid(), new ArrayList<>());
    }
}