package com.lifesteal.handlers;

import com.lifesteal.Main;
import com.lifesteal.configs.Config;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.*;

/**
 * Handles the Death's Dance enchantment effect.
 * 
 * Ignore Pain: Reduces a percentage of damage taken and stores it to deal as true damage over 3 seconds.
 * Defy: If an enemy dies within 3 seconds of being damaged by the player, clears stored damage and heals.
 */
@Mod.EventBusSubscriber
public class DeathsDanceHandler {

    private static final Map<UUID, PlayerDanceData> DANCE_DATA = new HashMap<>();

    /**
     * Public method to clear all Deaths Dance data for a player.
     * Used by the rescue command and other cleanup operations.
     * Completely removes the player from the tracking system.
     */
    public static void clearPlayerData(UUID playerUUID) {
        // Completely remove the player from the map to ensure clean slate
        DANCE_DATA.remove(playerUUID);
    }

    private static class PlayerDanceData {
        float storedDamage; // Total damage stored to be applied later
        int damageTickCounter; // Counter for applying delayed damage
        float healingRemaining; // Healing to apply over time
        int healingTickCounter; // Counter for applying healing
        Map<UUID, Long> damagedEntities; // Tracks entities damaged by this player (UUID -> game time)
        
        PlayerDanceData() {
            this.storedDamage = 0;
            this.damageTickCounter = 0;
            this.healingRemaining = 0;
            this.healingTickCounter = 0;
            this.damagedEntities = new HashMap<>();
        }
    }

    /**
     * HIGH PRIORITY - Process damage reduction and storage BEFORE other damage modifications
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onPlayerHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getEntity();
        ItemStack weapon = player.getMainHandItem();
        
        int danceLevel = EnchantmentHelper.getItemEnchantmentLevel(Main.DEATHS_DANCE_ENCHANTMENT.get(), weapon);
        if (danceLevel <= 0) {
            return;
        }
        
        handleIgnorePain(event, player, danceLevel);
    }

    /**
     * Track when player damages an entity
     */
    @SubscribeEvent
    public static void onPlayerAttack(LivingHurtEvent event) {
        if (!(event.getSource().getEntity() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getSource().getEntity();
        ItemStack weapon = player.getMainHandItem();
        
        int danceLevel = EnchantmentHelper.getItemEnchantmentLevel(Main.DEATHS_DANCE_ENCHANTMENT.get(), weapon);
        if (danceLevel <= 0) {
            return;
        }
        
        LivingEntity target = event.getEntity();
        UUID playerUUID = player.getUUID();
        UUID targetUUID = target.getUUID();
        long currentTime = player.level().getGameTime();
        
        // Track this damage for kill credit
        PlayerDanceData data = DANCE_DATA.computeIfAbsent(playerUUID, k -> new PlayerDanceData());
        data.damagedEntities.put(targetUUID, currentTime);
    }

    /**
     * Ignore Pain: Reduce incoming damage and store it for later
     */
    private static void handleIgnorePain(LivingHurtEvent event, Player player, int danceLevel) {
        // Skip if player is dying (but allow at low HP - that's when it's most useful!)
        if (player.isDeadOrDying()) {
            return;
        }
        float incomingDamage = event.getAmount();
        
        // Calculate damage reduction percentage (scales with level)
        float reductionPercent = (float) (danceLevel * Config.getDeathsDanceReductionPerLevel());
        
        float reducedDamage = incomingDamage * reductionPercent;
        float immediateDamage = incomingDamage - reducedDamage;
        
        // Apply immediate reduced damage - this helps survival at low HP!
        event.setAmount(immediateDamage);
        
        // Store the reduced damage to be applied over time
        UUID playerUUID = player.getUUID();
        PlayerDanceData data = DANCE_DATA.computeIfAbsent(playerUUID, k -> new PlayerDanceData());
        data.storedDamage += reducedDamage;
        
        // Reset damage application counter
        data.damageTickCounter = 0;
    }

    /**
     * Clear stored damage when player respawns to prevent post-respawn damage
     */
    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        Player player = event.getEntity();
        UUID playerUUID = player.getUUID();
        PlayerDanceData data = DANCE_DATA.get(playerUUID);
        
        if (data != null) {
            // Clear all stored damage and effects on respawn
            data.storedDamage = 0;
            data.damageTickCounter = 0;
            data.healingRemaining = 0;
            data.healingTickCounter = 0;
        }
    }

    /**
     * Defy: When an enemy dies, check if player damaged them recently
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onEntityDeath(LivingDeathEvent event) {
        LivingEntity deadEntity = event.getEntity();
        UUID deadEntityUUID = deadEntity.getUUID();
        long currentTime = deadEntity.level().getGameTime();
        
        // If a player dies, cleanse their stored damage and prevent interference with other mods
        if (deadEntity instanceof Player) {
            PlayerDanceData data = DANCE_DATA.get(deadEntityUUID);
            
            // Always clear stored damage on death, regardless of whether they're holding the weapon
            // This handles /kill command, respawning, and prevents post-death damage application
            if (data != null) {
                data.storedDamage = 0;
                data.damageTickCounter = 0;
                data.healingRemaining = 0;
                data.healingTickCounter = 0;
            }
        }
        
        // Check all players with Death's Dance data
        for (Map.Entry<UUID, PlayerDanceData> entry : DANCE_DATA.entrySet()) {
            UUID playerUUID = entry.getKey();
            PlayerDanceData data = entry.getValue();
            
            // Check if this player damaged the entity recently
            Long damageTime = data.damagedEntities.get(deadEntityUUID);
            if (damageTime != null && (currentTime - damageTime) <= Config.getDeathsDanceDamageIntervalTicks() * 3) {
                // Player gets kill credit! Trigger Defy
                
                // Find the player entity
                Player player = deadEntity.level().getPlayerByUUID(playerUUID);
                if (player != null) {
                    ItemStack weapon = player.getMainHandItem();
                    int danceLevel = EnchantmentHelper.getItemEnchantmentLevel(Main.DEATHS_DANCE_ENCHANTMENT.get(), weapon);
                    
                    if (danceLevel > 0) {
                        handleDefy(weapon, data, danceLevel);
                    }
                }
                
                // Clean up the tracked entity
                data.damagedEntities.remove(deadEntityUUID);
            }
        }
    }

    /**
     * Defy: Clear stored damage and heal based on weapon AD
     */
    private static void handleDefy(ItemStack weapon, PlayerDanceData data, int danceLevel) {
        // Clear all stored damage
        data.storedDamage = 0;
        data.damageTickCounter = 0;
        
        // Calculate healing based on weapon AD
        float weaponDamage = calculateWeaponDamage(weapon);
        float healAmount = weaponDamage * (float) Config.getDeathsDanceHealPercent() * danceLevel; // Scale with level
        
        // Store healing to apply over 2 seconds (40 ticks)
        data.healingRemaining = healAmount;
        data.healingTickCounter = 0;
    }

    /**
     * Calculates weapon damage including sharpness
     */
    private static float calculateWeaponDamage(ItemStack weapon) {
        float baseDamage = 4.0f;
        
        if (weapon.getItem() instanceof net.minecraft.world.item.SwordItem swordItem) {
            baseDamage = (float) swordItem.getDamage();
        } else if (weapon.getItem() instanceof net.minecraft.world.item.AxeItem axeItem) {
            baseDamage = (float) axeItem.getAttackDamage();
        }
        
        // Add sharpness bonus
        int sharpnessLevel = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SHARPNESS, weapon);
        if (sharpnessLevel > 0) {
            baseDamage += (0.5f * sharpnessLevel + 0.5f);
        }
        
        return baseDamage;
    }

    /**
     * Server tick handler - Apply stored damage and healing over time
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
        
        Iterator<Map.Entry<UUID, PlayerDanceData>> iter = DANCE_DATA.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<UUID, PlayerDanceData> entry = iter.next();
            UUID playerUUID = entry.getKey();
            PlayerDanceData data = entry.getValue();
            
            // Find the player across all dimensions
            Player player = server.getPlayerList().getPlayer(playerUUID);
            if (player == null) {
                continue; // Player not online
            }
            
            long currentTime = player.level().getGameTime();
            
            // Apply stored damage over time
            if (data.storedDamage > 0) {
                // Critical safety check: If player is at critical health or dying, clear all stored damage
                // This prevents conflicts with death mechanics, /kill command, and resurrection mods
                if (player.getHealth() <= 1.0f || player.isDeadOrDying()) {
                    data.storedDamage = 0;
                    data.damageTickCounter = 0;
                } else {
                    data.damageTickCounter++;
                    
                    // Apply damage every second (20 ticks) for 3 seconds total
                    if (data.damageTickCounter >= Config.getDeathsDanceDamageIntervalTicks()) {
                        float damageToApply = data.storedDamage / 3.0f;
                        
                        // Additional safety: Don't apply damage if it would kill the player
                        // Leave at least 0.5 hearts to prevent accidental death
                        if (damageToApply >= player.getHealth() - 1.0f) {
                            // Damage would be lethal, clear it instead
                            data.storedDamage = 0;
                            data.damageTickCounter = 0;
                        } else {
                            // Safe to apply damage
                            player.hurt(player.damageSources().magic(), damageToApply);
                            
                            data.storedDamage -= damageToApply;
                            data.damageTickCounter = 0;
                            
                            // Clean up if damage is exhausted
                            if (data.storedDamage <= 0.1f) {
                                data.storedDamage = 0;
                                data.damageTickCounter = 0;
                            }
                        }
                    }
                }
            }
            
            // Apply healing over time
            if (data.healingRemaining > 0) {
                data.healingTickCounter++;
                
                // Apply healing every tick over 2 seconds (40 ticks total)
                if (data.healingTickCounter % 2 == 0) { // Apply every 2 ticks for smoother healing
                    float healToApply = data.healingRemaining / 20.0f;
                    
                    player.heal(healToApply);
                    
                    data.healingRemaining -= healToApply;
                    
                    if (data.healingRemaining <= 0.1f || data.healingTickCounter >= 40) {
                        data.healingRemaining = 0;
                        data.healingTickCounter = 0;
                    }
                }
            }
            
            // Clean up old tracked entities (older than kill credit window)
            data.damagedEntities.entrySet().removeIf(e -> 
                (currentTime - e.getValue()) > Config.getDeathsDanceDamageIntervalTicks() * 3 + 100
            );
            
            // Remove player data if nothing is active
            if (data.storedDamage == 0 && data.healingRemaining == 0 && data.damagedEntities.isEmpty()) {
                iter.remove();
            }
        }
    }
}
