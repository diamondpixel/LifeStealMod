package com.lifesteal.handlers;

import com.lifesteal.Main;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.*;

/**
 * Handles the RockSolid enchantment effect.
 * 
 * Each armor piece with RockSolid provides:
 * - Minimum armor floor equal to the number of pieces with the enchantment
 * - Plays sound effect when hit (10s cooldown)
 */
@Mod.EventBusSubscriber
public class RockSolidHandler {

    private static final Map<UUID, Long> SOUND_COOLDOWNS = new HashMap<>();
    private static final Map<UUID, Double> ORIGINAL_ARMOR = new HashMap<>();
    private static final long SOUND_COOLDOWN_TICKS = 200; // 10 seconds = 200 ticks
    private static final SoundEvent ROCK_SOLID_SOUND = SoundEvent.createVariableRangeEvent(
            new ResourceLocation(Main.MODID, "rocksolid")
    );

    /**
     * HIGH PRIORITY - Play sound and track armor when player is hit
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onPlayerHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getEntity();
        
        // Count armor pieces with RockSolid enchantment
        int rockSolidCount = countRockSolidPieces(player);
        
        if (rockSolidCount <= 0) {
            return;
        }
        
        // Play sound if off cooldown
        playSoundIfReady(player);
        
        // Store original armor if not already tracked
        UUID playerUUID = player.getUUID();
        if (!ORIGINAL_ARMOR.containsKey(playerUUID)) {
            ORIGINAL_ARMOR.put(playerUUID, (double) player.getArmorValue());
        }
    }

    /**
     * Counts how many armor pieces have the RockSolid enchantment
     */
    private static int countRockSolidPieces(Player player) {
        int count = 0;
        
        for (EquipmentSlot slot : new EquipmentSlot[]{
                EquipmentSlot.HEAD, 
                EquipmentSlot.CHEST, 
                EquipmentSlot.LEGS, 
                EquipmentSlot.FEET
        }) {
            ItemStack armorPiece = player.getItemBySlot(slot);
            int enchantLevel = EnchantmentHelper.getItemEnchantmentLevel(
                    Main.ROCK_SOLID_ENCHANTMENT.get(), 
                    armorPiece
            );
            if (enchantLevel > 0) {
                count++;
            }
        }
        
        return count;
    }

    /**
     * Gets the minimum armor floor (1 armor point per piece with enchantment)
     */
    public static int getMinimumArmorFloor(Player player) {
        return countRockSolidPieces(player);
    }

    /**
     * Server tick handler - Enforce minimum armor floor every tick
     */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        
        net.minecraft.server.MinecraftServer server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }
        
        long currentTime = server.getTickCount();
        
        // Check all online players
        for (Player player : server.getPlayerList().getPlayers()) {
            int minArmorFloor = getMinimumArmorFloor(player);
            
            if (minArmorFloor > 0) {
                double currentArmor = player.getArmorValue();
                
                // If armor has been shredded below the minimum floor, enforce it
                if (currentArmor < minArmorFloor) {
                    player.getAttribute(Attributes.ARMOR).setBaseValue(minArmorFloor);
                }
            }
        }
        
        // Cleanup old data periodically (every 5 seconds)
        if (currentTime % 100 == 0) {
            SOUND_COOLDOWNS.entrySet().removeIf(entry -> 
                    (currentTime - entry.getValue()) > SOUND_COOLDOWN_TICKS + 1200
            );
            
            // Clean up armor tracking for offline players
            Set<UUID> onlinePlayerUUIDs = new HashSet<>();
            for (Player player : server.getPlayerList().getPlayers()) {
                onlinePlayerUUIDs.add(player.getUUID());
            }
            ORIGINAL_ARMOR.keySet().retainAll(onlinePlayerUUIDs);
        }
    }

    /**
     * Plays the RockSolid sound effect if cooldown has expired
     */
    private static void playSoundIfReady(Player player) {
        UUID playerUUID = player.getUUID();
        long currentTime = player.level().getGameTime();
        
        Long lastSoundTime = SOUND_COOLDOWNS.get(playerUUID);
        
        if (lastSoundTime == null || (currentTime - lastSoundTime) >= SOUND_COOLDOWN_TICKS) {
            // Play sound
            player.level().playSound(
                    null, 
                    player.getX(), 
                    player.getY(), 
                    player.getZ(), 
                    ROCK_SOLID_SOUND, 
                    SoundSource.PLAYERS, 
                    1.0f, 
                    1.0f
            );
            
            // Update cooldown
            SOUND_COOLDOWNS.put(playerUUID, currentTime);
        }
    }
}
