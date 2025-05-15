package com.lifesteal.handlers;

import com.lifesteal.Main;
import com.lifesteal.Utility;
import com.lifesteal.configs.Config;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * This class implements a ticking cleave effect. When a player with the CLEAVE enchantment
 * attacks a target, a cleave “stack” is added. Each stack shreds a percentage of the target’s
 * original armor (using getCleavePercent() as the armor shred percentage per stack). The first
 * stack decays after 2 seconds (40 ticks) after the last hit; thereafter, each remaining stack
 * decays every 0.3 seconds (6 ticks). If a new hit would reduce the target’s effective armor below 0,
 * extra damage is applied based on the overshoot.
 */
@Mod.EventBusSubscriber
public class CleaveHandler {

    // Stores per-target cleave data.
    private static final Map<LivingEntity, CleaveData> CLEAVE_DATA = new HashMap<>();

    private static class CleaveData {
        int stacks;                   // Current number of cleave stacks on this target.
        int ticksUntilNextRemoval;    // Countdown (in ticks) until a stack is removed.
        double originalArmor;         // The target’s armor value before any cleave reduction.
    }

    /**
     * Called when a LivingHurtEvent is fired.
     * If the attacker is a player with a CLEAVE-enchanted weapon, update the target’s cleave data.
     */
    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getSource().getEntity() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getSource().getEntity();
        handleCleave(event, player);
    }

    /**
     * Cleave Effect:
     * <ul>
     *   <li>Each consecutive attack by a player with the CLEAVE enchantment increases the target’s cleave stack
     *       (up to a maximum defined in the config).</li>
     *   <li>Each cleave stack shreds a percentage of the target’s original armor:
     *       effectiveArmor = originalArmor * (1 - (cleavePercent * stacks)).</li>
     *   <li>If the effective armor would go negative, the overshoot is converted into extra damage (multiplied
     *       by an extra factor, here 0.5 per point of overshoot).</li>
     *   <li>The target’s armor is updated via setEntityArmor().</li>
     * </ul>
     */
    private static void handleCleave(LivingHurtEvent event, Player player) {
        ItemStack weapon = player.getMainHandItem();
        LivingEntity target = event.getEntity();

        // Check if the weapon has the CLEAVE enchantment.
        int cleaveLevel = EnchantmentHelper.getItemEnchantmentLevel(Main.CLEAVE_ENCHANTMENT.get(), weapon);
        if (cleaveLevel > 0) {
            int maxStacks = Config.getCleaveMaxStacks();
            // getCleavePercent() returns the armor shred percentage per stack.
            // For example, 0.1 means each stack reduces the original armor by 10%.
            double armorShredPercent = Config.getCleavePercent();

            // Retrieve (or create) the cleave data for this target.
            CleaveData data = CLEAVE_DATA.get(target);
            if (data == null) {
                data = new CleaveData();
                data.stacks = 0;
                data.ticksUntilNextRemoval = 40; // 2 seconds (40 ticks) before the first stack removal.
                // Save the target’s current (original) armor.
                data.originalArmor = target.getAttribute(Attributes.ARMOR).getBaseValue();
                CLEAVE_DATA.put(target, data);
            } else {
                // Reset the decay timer on each hit.
                data.ticksUntilNextRemoval = 40;
            }
            if (data.stacks < maxStacks) {
                data.stacks++;
            }

            // Calculate the effective armor after applying the cleave stacks.
            double effectiveArmor = data.originalArmor * (1 - armorShredPercent * data.stacks);
            float damageMultiplier = 1.0f;

            // If effective armor would drop below 0, convert the overshoot into extra damage.
            if (effectiveArmor < 0) {
                double overshoot = Math.abs(effectiveArmor);
                damageMultiplier += (float)(overshoot * 0.5);  // 0.5 is an arbitrary factor.
                effectiveArmor = 0;
            }

            // Update the target’s armor using our helper function.
            Utility.setEntityArmor(target, effectiveArmor);

            // Apply the (possibly extra) damage.
            event.setAmount(event.getAmount() * damageMultiplier);
        }
    }

    /**
     * Server tick event handler.
     * Decrements each target’s timer; when it reaches 0, one cleave stack is removed.
     * After the first removal, subsequent stacks decay every 6 ticks (≈0.3 seconds).
     * When no stacks remain, the target’s armor is reset to its original value.
     */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        // Process only at the end of the tick.
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Iterator<Map.Entry<LivingEntity, CleaveData>> iter = CLEAVE_DATA.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<LivingEntity, CleaveData> entry = iter.next();
            LivingEntity target = entry.getKey();
            CleaveData data = entry.getValue();

            data.ticksUntilNextRemoval--;
            if (data.ticksUntilNextRemoval <= 0) {
                // Remove one cleave stack.
                data.stacks--;
                if (data.stacks > 0) {
                    // After the first removal, subsequent removals occur every 6 ticks (≈0.3 seconds).
                    data.ticksUntilNextRemoval = 6;
                    double armorShredPercent = Config.getCleavePercent();
                    double effectiveArmor = data.originalArmor * (1 - armorShredPercent * data.stacks);
                    if (effectiveArmor < 0) {
                        effectiveArmor = 0;
                    }
                    Utility.setEntityArmor(target, effectiveArmor);
                } else {
                    // No stacks remain; reset the target’s armor.
                    Utility.setEntityArmor(target, data.originalArmor);
                    iter.remove();
                }
            }
        }
    }
}