package com.lifesteal.configs;

import com.lifesteal.Main;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

@Mod.EventBusSubscriber(modid = Main.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config {

    public static double getLifestealPercent() {
        // Return the value from the appropriate config based on the side
        return (Main.modSide == Dist.CLIENT)
                ? ClientConfig.LIFESTEAL_PERCENT.get()
                : ServerConfig.LIFESTEAL_PERCENT.get();
    }

    public static double getDamageIncreasePercent() {
        return (Main.modSide == Dist.CLIENT)
                ? ClientConfig.DAMAGE_INCREASE_PERCENT.get()
                : ServerConfig.DAMAGE_INCREASE_PERCENT.get();
    }

    public static double getCleavePercent() {
        return (Main.modSide == Dist.CLIENT)
                ? ClientConfig.CLEAVE_PERCENT.get()
                : ServerConfig.CLEAVE_PERCENT.get();
    }

    public static int getCleaveMaxStacks() {
        return (Main.modSide == Dist.CLIENT)
                ? ClientConfig.CLEAVE_MAX_STACKS.get()
                : ServerConfig.CLEAVE_MAX_STACKS.get();
    }

    // Listen to config loading/reloading events
    @SubscribeEvent
    public static void onModConfigEvent(ModConfigEvent event) {
        if (event instanceof ModConfigEvent.Loading || event instanceof ModConfigEvent.Reloading) {
            try {
                double lifesteal = getLifestealPercent();
                double damageIncrease = getDamageIncreasePercent();
                Main.LOGGER.debug("Updated config values - Main: {}%, Damage Increase: {}%",
                        lifesteal * 100, damageIncrease * 100);
            } catch (Exception e) {
                Main.LOGGER.error("Failed to update config values", e);
            }
        }
    }
}
