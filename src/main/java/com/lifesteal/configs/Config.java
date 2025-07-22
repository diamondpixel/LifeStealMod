package com.lifesteal.configs;

import com.lifesteal.Main;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

@Mod.EventBusSubscriber(modid = Main.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config {

    public static double getLifestealPercent() {
        return ServerConfig.LIFESTEAL_PERCENT.get();
    }

    public static double getDamageIncreasePercent() {
        return ServerConfig.DAMAGE_INCREASE_PERCENT.get();
    }

    public static double getCleavePercent() {
        return ServerConfig.CLEAVE_PERCENT.get();
    }

    public static int getCleaveMaxStacks() {
        return ServerConfig.CLEAVE_MAX_STACKS.get();
    }

    // Listen to config loading/reloading events
    @SubscribeEvent
    public static void onModConfigEvent(ModConfigEvent event) {
        if (event instanceof ModConfigEvent.Loading || event instanceof ModConfigEvent.Reloading) {
            if (event.getConfig().getSpec() == ClientConfig.CONFIG) {
                Main.LOGGER.debug("LifestealMod Client Config reloaded/loaded.");
            } else if (event.getConfig().getSpec() == ServerConfig.CONFIG) {
                try {
                    double lifesteal = ServerConfig.LIFESTEAL_PERCENT.get();
                    double damageIncrease = ServerConfig.DAMAGE_INCREASE_PERCENT.get();
                    double cleavePercent = ServerConfig.CLEAVE_PERCENT.get();
                    int cleaveMaxStacks = ServerConfig.CLEAVE_MAX_STACKS.get();
                    Main.LOGGER.debug(
                        "LifestealMod Server Config reloaded/loaded. Values:\n" +
                        "  Lifesteal Percent: {}%\n" +
                        "  Damage Increase Percent: {}%\n" +
                        "  Cleave Percent: {}%\n" +
                        "  Cleave Max Stacks: {}",
                        lifesteal * 100,
                        damageIncrease * 100,
                        cleavePercent * 100,
                        cleaveMaxStacks
                    );
                } catch (Exception e) {
                    Main.LOGGER.error("Failed to log server config values after event", e);
                }
            }
        }
    }
}
