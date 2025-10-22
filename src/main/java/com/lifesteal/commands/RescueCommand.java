package com.lifesteal.commands;

import com.lifesteal.handlers.DeathsDanceHandler;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.Collection;

public class RescueCommand {
    
    /**
     * Register the "rescue" command.
     * Usage:
     *   /rescue - Rescues yourself
     *   /rescue <player> - Rescues the specified player (requires OP)
     *
     * This command clears all Deaths Dance effects and fully heals the player.
     * Useful as a safety fallback if Death's Dance causes a bugged state.
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // Self-rescue (no permission required)
        dispatcher.register(
                Commands.literal("rescue")
                        .executes(context -> executeSelf(context))
                        .then(Commands.argument("player", EntityArgument.players())
                                .requires(source -> source.hasPermission(2))
                                .executes(context -> executeOther(context))
                        )
        );
    }

    /**
     * Executes rescue on the command sender
     */
    private static int executeSelf(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("Only players can use this command."));
            return 0;
        }
        
        rescuePlayer(player);
        source.sendSuccess(
                () -> Component.literal("§aRescued! All Deaths Dance effects cleared and health restored."), 
                false
        );
        
        return 1;
    }

    /**
     * Executes rescue on another player (requires OP)
     */
    private static int executeOther(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        try {
            Collection<ServerPlayer> players = EntityArgument.getPlayers(context, "player");
            
            int count = 0;
            for (ServerPlayer player : players) {
                rescuePlayer(player);
                count++;
            }
            
            final int finalCount = count;
            source.sendSuccess(
                    () -> Component.literal("§aRescued " + finalCount + " player(s)!"), 
                    true
            );
            
            return count;
        } catch (Exception e) {
            source.sendFailure(Component.literal("Failed to rescue player: " + e.getMessage()));
            return 0;
        }
    }

    /**
     * Performs the rescue operation on a player
     */
    private static void rescuePlayer(Player player) {
        // CRITICAL: Clear Deaths Dance data MULTIPLE times to ensure it's gone
        // This handles any race conditions or re-initialization issues
        DeathsDanceHandler.clearPlayerData(player.getUUID());
        
        // Force remove any dying/dead state FIRST
        if (player.isDeadOrDying()) {
            player.setHealth(1.0f); // Set minimal health first
        }
        
        // Clear all negative effects
        player.removeAllEffects();
        player.clearFire();
        
        // Now heal to full health
        player.setHealth(player.getMaxHealth());
        
        // Clear Deaths Dance data again after healing to catch any new entries
        DeathsDanceHandler.clearPlayerData(player.getUUID());
        
        // Give absorption hearts as a buffer (2 extra hearts)
        player.setAbsorptionAmount(4.0f);
    }
}
