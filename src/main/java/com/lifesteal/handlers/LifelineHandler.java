package com.lifesteal.handlers;

import com.lifesteal.Main;
import com.lifesteal.configs.Config;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Handles the Lifeline enchantment effect.
 * 
 * When damage would reduce the player below 30% health, grants a protective shield
 * that decays over 9 seconds. Has a 90 second cooldown.
 */
@Mod.EventBusSubscriber
public class LifelineHandler {

    private static final Map<UUID, PlayerLifelineData> LIFELINE_DATA = new HashMap<>();

    private static class PlayerLifelineData {
        int cooldownRemaining; // Ticks until lifeline can be used again
        int shieldTicksRemaining; // Ticks until shield effect ends
        
        PlayerLifelineData() {
            this.cooldownRemaining = 0;
            this.shieldTicksRemaining = 0;
        }
    }

    /**
     * HIGH PRIORITY - Check if lifeline should trigger before damage is applied
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onPlayerHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getEntity();
        ItemStack weapon = player.getMainHandItem();
        
        int lifelineLevel = EnchantmentHelper.getItemEnchantmentLevel(Main.LIFELINE_ENCHANTMENT.get(), weapon);
        if (lifelineLevel <= 0) {
            return;
        }
        
        handleLifeline(event, player, weapon, lifelineLevel);
    }

    /**
     * Lifeline: Grant shield when damage would drop player below 30% health
     */
    private static void handleLifeline(LivingHurtEvent event, Player player, ItemStack weapon, int lifelineLevel) {
        float currentHealth = player.getHealth();
        float maxHealth = player.getMaxHealth();
        float incomingDamage = event.getAmount();
        float healthAfterDamage = currentHealth - incomingDamage;
        float healthThreshold = maxHealth * (float) Config.getLifelineHealthThreshold();
        
        // Check if damage would drop player below 30% health
        if (healthAfterDamage < healthThreshold && currentHealth >= healthThreshold) {
            UUID playerUUID = player.getUUID();
            PlayerLifelineData data = LIFELINE_DATA.computeIfAbsent(playerUUID, k -> new PlayerLifelineData());
            
            // Check if lifeline is off cooldown
            if (data.cooldownRemaining <= 0) {
                // Trigger Lifeline!
                triggerLifeline(player, weapon, data, lifelineLevel);
            }
        }
    }

    /**
     * Activate the Lifeline shield
     */
    private static void triggerLifeline(Player player, ItemStack weapon, PlayerLifelineData data, int lifelineLevel) {
        // Calculate shield strength based on weapon AD and enchantment level
        // Since Minecraft doesn't have "bonus health", we use weapon damage as a base
        float weaponDamage = calculateWeaponDamage(weapon);
        
        // Shield amount scales with level: Level 1 = 4 hearts, Level 2 = 6 hearts, Level 3 = 8 hearts
        // Plus bonus from weapon damage
        int baseShieldHearts = 2 + (lifelineLevel * 2);
        float shieldAmount = baseShieldHearts * 2.0f + (weaponDamage * (float) Config.getLifelineShieldMultiplier() * lifelineLevel);
        
        // Apply absorption effect (shield)
        // Absorption level determines how many hearts
        int absorptionAmplifier = Math.min((int)(shieldAmount / 4.0f), 10); // Cap at reasonable level
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, Config.getLifelineShieldDurationTicks(), absorptionAmplifier, false, true));
        
        // Start cooldown
        data.cooldownRemaining = Config.getLifelineCooldownTicks();
        data.shieldTicksRemaining = Config.getLifelineShieldDurationTicks();
    }

    /**
     * Calculate weapon damage including sharpness
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
     * Server tick handler - Decrement cooldowns
     */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        
        Iterator<Map.Entry<UUID, PlayerLifelineData>> iter = LIFELINE_DATA.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<UUID, PlayerLifelineData> entry = iter.next();
            PlayerLifelineData data = entry.getValue();
            
            // Decrement cooldown
            if (data.cooldownRemaining > 0) {
                data.cooldownRemaining--;
            }
            
            // Decrement shield duration
            if (data.shieldTicksRemaining > 0) {
                data.shieldTicksRemaining--;
            }
            
            // Clean up if both timers are done
            if (data.cooldownRemaining <= 0 && data.shieldTicksRemaining <= 0) {
                iter.remove();
            }
        }
    }
}
