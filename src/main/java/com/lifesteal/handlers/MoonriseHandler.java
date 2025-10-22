package com.lifesteal.handlers;

import com.lifesteal.Main;
import com.lifesteal.configs.Config;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Handles the Moonrise enchantment effect.
 * When a player hits the same target twice within 1.5 seconds (30 ticks):
 * - Deals bonus damage based on weapon AD (base damage + sharpness)
 * - Grants 30% movement speed for 2 seconds (40 ticks)
 * - Grants absorption hearts (shield) for 2 seconds (40 ticks)
 * - Has a cooldown of 6 seconds (120 ticks)
 */
@Mod.EventBusSubscriber
public class MoonriseHandler {

    private static final Map<UUID, PlayerMoonriseData> MOONRISE_DATA = new HashMap<>();

    private static class PlayerMoonriseData {
        UUID lastTargetUUID;
        long lastHitTime; // In ticks
        int cooldownRemaining; // In ticks
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getSource().getEntity() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getSource().getEntity();
        LivingEntity target = event.getEntity();
        ItemStack weapon = player.getMainHandItem();
        
        int eclipseLevel = EnchantmentHelper.getItemEnchantmentLevel(Main.MOONRISE_ENCHANTMENT.get(), weapon);
        if (eclipseLevel <= 0) {
            return;
        }
        
        handleMoonrise(event, player, target, weapon, eclipseLevel);
    }

    private static void handleMoonrise(LivingHurtEvent event, Player player, LivingEntity target, ItemStack weapon, int eclipseLevel) {
        UUID playerUUID = player.getUUID();
        UUID targetUUID = target.getUUID();
        long currentTick = player.level().getGameTime();
        
        // Get or create player's moonrise data
        PlayerMoonriseData data = MOONRISE_DATA.computeIfAbsent(playerUUID, k -> {
            PlayerMoonriseData newData = new PlayerMoonriseData();
            newData.cooldownRemaining = 0;
            return newData;
        });
        
        // Check if on cooldown
        if (data.cooldownRemaining > 0) {
            return;
        }
        
        // Check if this is a second hit on the same target within the time window
        boolean isSameTarget = targetUUID.equals(data.lastTargetUUID);
        boolean withinTimeWindow = (currentTick - data.lastHitTime) <= Config.getMoonriseHitWindowTicks();
        
        if (isSameTarget && withinTimeWindow) {
            // TRIGGER MOONRISE EFFECT!
            
            // Calculate bonus damage based on target's max health (8% per level)
            float targetMaxHealth = target.getMaxHealth();
            float bonusDamage = targetMaxHealth * ((float) Config.getMoonriseDamagePercent() * eclipseLevel);
            
            event.setAmount(event.getAmount() + bonusDamage);
            
            // Grant 30% movement speed for 2 seconds
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, Config.getMoonriseEffectDurationTicks(), 0, false, true));
            
            // Grant shield: 300% weapon AD
            float weaponDamage = calculateWeaponDamage(weapon);
            float shieldAmount = weaponDamage * 3.0f;
            
            // Convert shield amount to absorption hearts (2 HP = 1 heart), scale absorption level accordingly
            // Custom rounding: if decimal < 0.4, floor; otherwise ceil
            float shieldHearts = shieldAmount / 4.0f;
            float decimal = shieldHearts - (int) shieldHearts;
            int absorptionLevel = (decimal < 0.4f) ? (int) shieldHearts : (int) Math.ceil(shieldHearts);
            absorptionLevel = Math.max(0, absorptionLevel - 1); // Absorption level 0 = 1 heart, so subtract 1
            player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, Config.getMoonriseEffectDurationTicks(), absorptionLevel, false, true));
            
            // Start cooldown
            data.cooldownRemaining = Config.getMoonriseCooldownTicks();
            
            // Reset tracking
            data.lastTargetUUID = null;
            data.lastHitTime = 0;
            
        } else {
            // This is the first hit or a different target - start tracking
            data.lastTargetUUID = targetUUID;
            data.lastHitTime = currentTick;
        }
    }

    /**
     * Calculates the total weapon damage including base damage and sharpness enchantment.
     */
    private static float calculateWeaponDamage(ItemStack weapon) {
        float baseDamage = 1.0f; // Default base (player punch)
        
        // Get actual attack damage from item attributes
        var attackDamageAttribute = weapon.getAttributeModifiers(net.minecraft.world.entity.EquipmentSlot.MAINHAND)
            .get(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
        
        if (!attackDamageAttribute.isEmpty()) {
            // Sum up the base damage (1.0) plus weapon modifier
            baseDamage = 1.0f; // Player base punch
            for (var modifier : attackDamageAttribute) {
                baseDamage += (float) modifier.getAmount();
            }
        }
        
        // Add sharpness bonus (Sharpness adds 0.5 * level + 0.5 damage)
        int sharpnessLevel = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SHARPNESS, weapon);
        if (sharpnessLevel > 0) {
            baseDamage += (0.5f * sharpnessLevel + 0.5f);
        }
        
        return baseDamage;
    }

    /**
     * Server tick event handler to decrement cooldowns.
     */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        
        Iterator<Map.Entry<UUID, PlayerMoonriseData>> iter = MOONRISE_DATA.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<UUID, PlayerMoonriseData> entry = iter.next();
            PlayerMoonriseData data = entry.getValue();
            
            // Decrement cooldown
            if (data.cooldownRemaining > 0) {
                data.cooldownRemaining--;
            }
        }
    }
}
