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

    // Moonrise getters
    public static int getMoonriseHitWindowTicks() {
        return ServerConfig.MOONRISE_HIT_WINDOW_TICKS.get();
    }

    public static int getMoonriseCooldownTicks() {
        return ServerConfig.MOONRISE_COOLDOWN_TICKS.get();
    }

    public static double getMoonriseDamagePercent() {
        return ServerConfig.MOONRISE_DAMAGE_PERCENT.get();
    }

    public static int getMoonriseEffectDurationTicks() {
        return ServerConfig.MOONRISE_EFFECT_DURATION_TICKS.get();
    }

    // Death's Dance getters
    public static double getDeathsDanceReductionPerLevel() {
        return ServerConfig.DEATHS_DANCE_REDUCTION_PER_LEVEL.get();
    }

    public static double getDeathsDanceHealPercent() {
        return ServerConfig.DEATHS_DANCE_HEAL_PERCENT.get();
    }

    public static int getDeathsDanceDamageIntervalTicks() {
        return ServerConfig.DEATHS_DANCE_DAMAGE_INTERVAL_TICKS.get();
    }

    // Lifeline getters
    public static double getLifelineHealthThreshold() {
        return ServerConfig.LIFELINE_HEALTH_THRESHOLD.get();
    }

    public static int getLifelineCooldownTicks() {
        return ServerConfig.LIFELINE_COOLDOWN_TICKS.get();
    }

    public static double getLifelineShieldMultiplier() {
        return ServerConfig.LIFELINE_SHIELD_MULTIPLIER.get();
    }

    public static int getLifelineShieldDurationTicks() {
        return ServerConfig.LIFELINE_SHIELD_DURATION_TICKS.get();
    }

    // Nightstalker getters
    public static double getNightstalkerDamagePerLevel() {
        return ServerConfig.NIGHTSTALKER_DAMAGE_PER_LEVEL.get();
    }

    public static int getNightstalkerInvisibilityDuration() {
        return ServerConfig.NIGHTSTALKER_INVISIBILITY_DURATION.get();
    }

    public static int getNightstalkerKillCreditWindow() {
        return ServerConfig.NIGHTSTALKER_KILL_CREDIT_WINDOW.get();
    }

    // Listen to config loading/reloading events
    @SubscribeEvent
    public static void onModConfigEvent(ModConfigEvent event) {
        if (event instanceof ModConfigEvent.Loading || event instanceof ModConfigEvent.Reloading) {
            if (event.getConfig().getSpec() == ClientConfig.CONFIG) {
                Main.LOGGER.debug("LifestealMod Client Config reloaded/loaded.");
            } else if (event.getConfig().getSpec() == ServerConfig.CONFIG) {
                try {
                    Main.LOGGER.debug(
                        "LifestealMod Server Config reloaded/loaded. Values:\n" +
                        "  [Lifesteal] Percent: {}%, Damage Increase: {}%\n" +
                        "  [Cleave] Percent: {}%, Max Stacks: {}\n" +
                        "  [Moonrise] Hit Window: {}t, Cooldown: {}t, Damage: {}%, Effect Duration: {}t\n" +
                        "  [Death's Dance] Reduction/Lvl: {}%, Heal: {}%, Damage Interval: {}t\n" +
                        "  [Lifeline] Health Threshold: {}%, Cooldown: {}t, Shield Mult: {}, Shield Duration: {}t\n" +
                        "  [Nightstalker] Damage/Lvl: {}%, Invulnerability: {}t, Kill Credit: {}t",
                        ServerConfig.LIFESTEAL_PERCENT.get() * 100,
                        ServerConfig.DAMAGE_INCREASE_PERCENT.get() * 100,
                        ServerConfig.CLEAVE_PERCENT.get() * 100,
                        ServerConfig.CLEAVE_MAX_STACKS.get(),
                        ServerConfig.MOONRISE_HIT_WINDOW_TICKS.get(),
                        ServerConfig.MOONRISE_COOLDOWN_TICKS.get(),
                        ServerConfig.MOONRISE_DAMAGE_PERCENT.get() * 100,
                        ServerConfig.MOONRISE_EFFECT_DURATION_TICKS.get(),
                        ServerConfig.DEATHS_DANCE_REDUCTION_PER_LEVEL.get() * 100,
                        ServerConfig.DEATHS_DANCE_HEAL_PERCENT.get() * 100,
                        ServerConfig.DEATHS_DANCE_DAMAGE_INTERVAL_TICKS.get(),
                        ServerConfig.LIFELINE_HEALTH_THRESHOLD.get() * 100,
                        ServerConfig.LIFELINE_COOLDOWN_TICKS.get(),
                        ServerConfig.LIFELINE_SHIELD_MULTIPLIER.get(),
                        ServerConfig.LIFELINE_SHIELD_DURATION_TICKS.get(),
                        ServerConfig.NIGHTSTALKER_DAMAGE_PER_LEVEL.get() * 100,
                        ServerConfig.NIGHTSTALKER_INVISIBILITY_DURATION.get(),
                        ServerConfig.NIGHTSTALKER_KILL_CREDIT_WINDOW.get()
                    );
                } catch (Exception e) {
                    Main.LOGGER.error("Failed to log server config values after event", e);
                }
            }
        }
    }
}
