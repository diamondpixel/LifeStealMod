package com.lifesteal.handlers;

import com.lifesteal.Main;
import com.lifesteal.commands.RescueCommand;
import com.lifesteal.commands.RetrieveArmorCommand;
import com.lifesteal.commands.SetArmorCommand;
import com.lifesteal.configs.Config;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Main.MODID)
public class EventHandler {

    @SubscribeEvent
    public static void onCommandRegister(RegisterCommandsEvent event) {
        RetrieveArmorCommand.register(event.getDispatcher());
        SetArmorCommand.register(event.getDispatcher());
        RescueCommand.register(event.getDispatcher());
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        // If the damage source is a player, apply offensive effects.
        if (event.getSource().getEntity() instanceof Player player) {
            handleLifesteal(event, player);
        }

        // If the entity hurt is a player, apply defensive (damage vulnerability) effects.
        if (event.getEntity() instanceof Player player) {
            handleDamageVulnerability(event, player);
        }
    }

    /**
     * Lifesteal: heals the attacker based on damage dealt.
     */
    private static void handleLifesteal(LivingHurtEvent event, Player player) {
        ItemStack weapon = player.getMainHandItem();
        int level = EnchantmentHelper.getItemEnchantmentLevel(Main.LIFESTEAL_ENCHANTMENT.get(), weapon);
        if (level > 0) {
            // Multiply the event damage by (configured lifesteal percent * enchantment level)
            float healAmount = event.getAmount() * ((float) Config.getLifestealPercent() * level);
            player.heal(healAmount);
        }
    }

    /**
     * Damage Vulnerability: increases damage taken by players with the enchantment.
     */
    private static void handleDamageVulnerability(LivingHurtEvent event, Player player) {
        ItemStack weapon = player.getMainHandItem();
        int level = EnchantmentHelper.getItemEnchantmentLevel(Main.LIFESTEAL_ENCHANTMENT.get(), weapon);
        if (level > 0) {
            float multiplier = 1 + ((float) Config.getDamageIncreasePercent() * level);
            event.setAmount(event.getAmount() * multiplier);
        }
    }
}