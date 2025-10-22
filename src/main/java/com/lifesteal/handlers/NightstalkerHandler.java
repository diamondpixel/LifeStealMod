package com.lifesteal.handlers;

import com.lifesteal.Main;
import com.lifesteal.configs.Config;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.*;

/**
 * Handles the Nightstalker enchantment effect.
 * 
 * Passive 1: Deals increased damage based on target's missing health
 * Passive 2: Getting a takedown grants invulnerability for 1.5 seconds
 */
@Mod.EventBusSubscriber
public class NightstalkerHandler {

    private static final Map<UUID, PlayerNightstalkerData> NIGHTSTALKER_DATA = new HashMap<>();

    private static class PlayerNightstalkerData {
        Map<UUID, Long> damagedEntities; // Tracks entities damaged by this player (UUID -> game time)
        int invulnerabilityTicksRemaining; // Ticks of invulnerability remaining
        
        PlayerNightstalkerData() {
            this.damagedEntities = new HashMap<>();
            this.invulnerabilityTicksRemaining = 0;
        }
    }

    /**
     * Apply bonus damage based on target's missing health
     */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onPlayerAttack(LivingHurtEvent event) {
        if (!(event.getSource().getEntity() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getSource().getEntity();
        LivingEntity target = event.getEntity();
        ItemStack weapon = player.getMainHandItem();
        
        int nightstalkerLevel = EnchantmentHelper.getItemEnchantmentLevel(Main.NIGHTSTALKER_ENCHANTMENT.get(), weapon);
        if (nightstalkerLevel <= 0) {
            return;
        }
        
        handleNightstalker(event, player, target, nightstalkerLevel);
    }

    /**
     * Nightstalker: Bonus damage based on missing health
     */
    private static void handleNightstalker(LivingHurtEvent event, Player player, LivingEntity target, int nightstalkerLevel) {
        UUID playerUUID = player.getUUID();
        UUID targetUUID = target.getUUID();
        long currentTime = player.level().getGameTime();
        
        // Track this damage for kill credit
        PlayerNightstalkerData data = NIGHTSTALKER_DATA.computeIfAbsent(playerUUID, k -> new PlayerNightstalkerData());
        data.damagedEntities.put(targetUUID, currentTime);
        
        // Calculate bonus damage based on missing health
        float targetCurrentHealth = target.getHealth();
        float targetMaxHealth = target.getMaxHealth();
        float missingHealthPercent = 1.0f - (targetCurrentHealth / targetMaxHealth);
        
        // Damage scaling based on level and missing health percentage
        float maxBonusPercent = (float) Config.getNightstalkerDamagePerLevel() * nightstalkerLevel;
        float bonusDamagePercent = missingHealthPercent * maxBonusPercent;
        
        float originalDamage = event.getAmount();
        float bonusDamage = originalDamage * bonusDamagePercent;
        
        event.setAmount(originalDamage + bonusDamage);
    }

    /**
     * Grant invulnerability when getting a takedown
     */
    @SubscribeEvent
    public static void onEntityDeath(LivingDeathEvent event) {
        LivingEntity deadEntity = event.getEntity();
        UUID deadEntityUUID = deadEntity.getUUID();
        long currentTime = deadEntity.level().getGameTime();
        
        // Check all players with Nightstalker data
        for (Map.Entry<UUID, PlayerNightstalkerData> entry : NIGHTSTALKER_DATA.entrySet()) {
            UUID playerUUID = entry.getKey();
            PlayerNightstalkerData data = entry.getValue();
            
            // Check if this player damaged the entity recently
            Long damageTime = data.damagedEntities.get(deadEntityUUID);
            if (damageTime != null && (currentTime - damageTime) <= Config.getNightstalkerKillCreditWindow()) {
                // Player gets kill credit! Grant invulnerability
                
                // Find the player entity
                Player player = deadEntity.level().getPlayerByUUID(playerUUID);
                if (player != null) {
                    ItemStack weapon = player.getMainHandItem();
                    int nightstalkerLevel = EnchantmentHelper.getItemEnchantmentLevel(Main.NIGHTSTALKER_ENCHANTMENT.get(), weapon);
                    
                    if (nightstalkerLevel > 0) {
                        triggerInvulnerability(player, data);
                    }
                }
                
                // Clean up the tracked entity
                data.damagedEntities.remove(deadEntityUUID);
            }
        }
    }

    /**
     * Grant invulnerability effect (Resistance 255 for complete damage immunity)
     */
    private static void triggerInvulnerability(Player player, PlayerNightstalkerData data) {
        player.addEffect(
                new MobEffectInstance(
                        MobEffects.DAMAGE_RESISTANCE,
                        Config.getNightstalkerInvisibilityDuration(),
                        255,
                        false,
                        true)
        );
        // Track invulnerability duration
        data.invulnerabilityTicksRemaining = Config.getNightstalkerInvisibilityDuration();
    }


    /**
     * Server tick handler - Clean up old data
     */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        
        Iterator<Map.Entry<UUID, PlayerNightstalkerData>> iter = NIGHTSTALKER_DATA.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<UUID, PlayerNightstalkerData> entry = iter.next();
            UUID playerUUID = entry.getKey();
            PlayerNightstalkerData data = entry.getValue();
            
            // Decrement invulnerability timer
            if (data.invulnerabilityTicksRemaining > 0) {
                data.invulnerabilityTicksRemaining--;
            }
            
            // Clean up old tracked entities
            net.minecraft.server.MinecraftServer server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                Player player = server.getPlayerList().getPlayer(playerUUID);
                if (player != null) {
                    long currentTime = player.level().getGameTime();
                    data.damagedEntities.entrySet().removeIf(e -> 
                        (currentTime - e.getValue()) > Config.getNightstalkerKillCreditWindow() + 100
                    );
                }
            }
            
            // Remove player data if nothing is active
            if (data.invulnerabilityTicksRemaining <= 0 && data.damagedEntities.isEmpty()) {
                iter.remove();
            }
        }
    }
}
