package net.saturn.chainmod;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

public class ChainCommand {

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            registerCommands(dispatcher);
        });
    }

    private static void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("chain")
                .then(CommandManager.literal("create")
                        .executes(ChainCommand::createChain))
                .then(CommandManager.literal("remove")
                        .executes(ChainCommand::removeChains))
                .then(CommandManager.literal("anchor")
                        .executes(ChainCommand::createAnchoredChain))
        );
    }

    private static int createChain(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();

        if (source.getEntity() instanceof ServerPlayerEntity player) {
            Vec3d playerPos = player.getEntityPos();
            Vec3d startPos = playerPos.add(2, 3, 0); // Chain attached above and to the side
            Vec3d endPos = playerPos.add(0, 1, 0); // Attached to player center

            ChainMod.createChain(player, startPos, endPos);
            player.sendMessage(Text.literal("Chain created!"), false);
            return 1;
        }

        return 0;
    }

    private static int createAnchoredChain(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();

        if (source.getEntity() instanceof ServerPlayerEntity player) {
            Vec3d anchorPos = player.getEntityPos().add(5, 5, 0); // Fixed point in space

            ChainMod.createChainToPlayer(player, anchorPos);
            player.sendMessage(Text.literal("Anchored chain created! Move around to see it follow you."), false);
            return 1;
        }

        return 0;
    }

    private static int removeChains(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();

        if (source.getEntity() instanceof ServerPlayerEntity player) {
            ChainMod.removeAllChains(player);
            player.sendMessage(Text.literal("All chains removed!"), false);
            return 1;
        }

        return 0;
    }
}