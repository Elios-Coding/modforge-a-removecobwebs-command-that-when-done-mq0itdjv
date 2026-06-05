package com.modforge.aremovecobwebscommandthatwhendone;

import com.mojang.brigadier.Command;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import static net.minecraft.server.command.CommandManager.literal;

public class ARemovecobwebsCommandThatWhenDoneMod implements ModInitializer {
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("a-removecobwebs-command-that-when-done-mq0itdjv");

    private static final int RADIUS_BLOCKS = 50;

    @Override
    public void onInitialize() {
        try {
            CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, env) -> {
                try {
                    dispatcher.register(literal("removecobwebs")
                            .requires(source -> {
                                try {
                                    // Modern Fabric/Yarn: ServerCommandSource#hasPermissionLevel(int)
                                    // (Some mappings use #hasPermissionLevel, not #hasPermission)
                                    return source.hasPermission(2);
                                } catch (Throwable t) {
                                    LOGGER.error("ModForge: permission check failed for /removecobwebs", t);
                                    return false;
                                }
                            })
                            .executes(ctx -> executeRemoveCobwebs(ctx.getSource())));
                } catch (Throwable t) {
                    LOGGER.error("ModForge: failed to register /removecobwebs", t);
                }
            });
        } catch (Throwable t) {
            LOGGER.error("ModForge: failed to initialize command registration", t);
        }
    }

    private static int executeRemoveCobwebs(ServerCommandSource source) {
        try {
            final ServerPlayerEntity player = source.getPlayer();
            final ServerWorld world = player.getWorld();
            final BlockPos center = player.getBlockPos();

            int removed = 0;

            final int r = RADIUS_BLOCKS;
            final int r2 = r * r;

            // Scan a cube around the player, but only act on positions within a sphere of radius 50.
            for (int dx = -r; dx <= r; dx++) {
                for (int dy = -r; dy <= r; dy++) {
                    for (int dz = -r; dz <= r; dz++) {
                        final int dist2 = dx * dx + dy * dy + dz * dz;
                        if (dist2 > r2) {
                            continue;
                        }

                        final BlockPos pos = center.add(dx, dy, dz);
                        final BlockState state = world.getBlockState(pos);
                        if (state.isOf(Blocks.COBWEB)) {
                            // Flags 3: notify neighbors + update clients.
                            final boolean changed = world.setBlockState(pos, Blocks.AIR.getDefaultState(), 3);
                            if (changed) {
                                removed++;
                            }
                        }
                    }
                }
            }

            final int removedFinal = removed;
            source.sendFeedback(() -> Text.literal("Removed " + removedFinal + " cobweb(s) within " + RADIUS_BLOCKS + " blocks."), true);
            return Command.SINGLE_SUCCESS;
        } catch (Throwable t) {
            LOGGER.error("ModForge: /removecobwebs failed", t);
            try {
                source.sendFeedback(() -> Text.literal("Failed to remove cobwebs (see server log)."), false);
            } catch (Throwable ignored) {
                // If sending feedback fails, we still logged the error.
            }
            return 0;
        }
    }
}
