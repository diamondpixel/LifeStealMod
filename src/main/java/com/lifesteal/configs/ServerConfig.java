package com.lifesteal.configs;

import net.minecraftforge.common.ForgeConfigSpec;

public class ServerConfig {
    // Create a builder for the server config
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec CONFIG;

    // Define server config values
    public static final ForgeConfigSpec.DoubleValue LIFESTEAL_PERCENT;
    public static final ForgeConfigSpec.DoubleValue DAMAGE_INCREASE_PERCENT;
    public static final ForgeConfigSpec.DoubleValue CLEAVE_PERCENT;
    public static final ForgeConfigSpec.IntValue CLEAVE_MAX_STACKS;
    
    // Moonrise config
    public static final ForgeConfigSpec.IntValue MOONRISE_HIT_WINDOW_TICKS;
    public static final ForgeConfigSpec.IntValue MOONRISE_COOLDOWN_TICKS;
    public static final ForgeConfigSpec.DoubleValue MOONRISE_DAMAGE_PERCENT;
    public static final ForgeConfigSpec.IntValue MOONRISE_EFFECT_DURATION_TICKS;
    
    // Death's Dance config
    public static final ForgeConfigSpec.DoubleValue DEATHS_DANCE_REDUCTION_PER_LEVEL;
    public static final ForgeConfigSpec.DoubleValue DEATHS_DANCE_HEAL_PERCENT;
    public static final ForgeConfigSpec.IntValue DEATHS_DANCE_DAMAGE_INTERVAL_TICKS;
    
    // Lifeline config
    public static final ForgeConfigSpec.DoubleValue LIFELINE_HEALTH_THRESHOLD;
    public static final ForgeConfigSpec.IntValue LIFELINE_COOLDOWN_TICKS;
    public static final ForgeConfigSpec.DoubleValue LIFELINE_SHIELD_MULTIPLIER;
    public static final ForgeConfigSpec.IntValue LIFELINE_SHIELD_DURATION_TICKS;
    
    // Nightstalker config
    public static final ForgeConfigSpec.DoubleValue NIGHTSTALKER_DAMAGE_PER_LEVEL;
    public static final ForgeConfigSpec.IntValue NIGHTSTALKER_INVISIBILITY_DURATION;
    public static final ForgeConfigSpec.IntValue NIGHTSTALKER_KILL_CREDIT_WINDOW;

    static {
        // Optional: Group the settings under a "server" category.
        BUILDER.comment("Server configuration for Main").push("server");

        LIFESTEAL_PERCENT = BUILDER
                .comment("Percentage of damage healed per Main tier (0.1 = 10%)")
                .defineInRange("lifesteal_percent", 0.1, 0.0, 1.0);

        DAMAGE_INCREASE_PERCENT = BUILDER
                .comment("Additional damage taken per Main tier (0.1 = 10% more damage)")
                .defineInRange("damage_increase_percent", 0.1, 0.0, 1.0);

        CLEAVE_PERCENT = BUILDER
                .comment("Shred armor if armor, while armor > 0 / increase dmg if armor < 0 based on cleave stacks.")
                .defineInRange("cleave_percent", 0.1, 0.0, 1.0);

        CLEAVE_MAX_STACKS = BUILDER
                .comment("Configure max applicable cleave stacks.")
                .defineInRange("cleave_max_stacks", 6, 1, 6);

        // Moonrise enchantment settings
        BUILDER.comment("Moonrise Enchantment Settings").push("moonrise");
        
        MOONRISE_HIT_WINDOW_TICKS = BUILDER
                .comment("Time window in ticks to hit the same target twice (30 ticks = 1.5 seconds)")
                .defineInRange("hit_window_ticks", 30, 1, 200);
        
        MOONRISE_COOLDOWN_TICKS = BUILDER
                .comment("Cooldown in ticks before Moonrise can trigger again (120 ticks = 6 seconds)")
                .defineInRange("cooldown_ticks", 120, 0, 6000);
        
        MOONRISE_DAMAGE_PERCENT = BUILDER
                .comment("Bonus damage as percentage of target's maximum health per level (0.08 = 8% of target max HP)")
                .defineInRange("damage_percent", 0.08, 0.0, 1.0);
        
        MOONRISE_EFFECT_DURATION_TICKS = BUILDER
                .comment("Duration of movement speed and shield effects in ticks (40 ticks = 2 seconds)")
                .defineInRange("effect_duration_ticks", 40, 1, 200);
        
        BUILDER.pop();
        
        // Death's Dance enchantment settings
        BUILDER.comment("Death's Dance Enchantment Settings").push("deaths_dance");
        
        DEATHS_DANCE_REDUCTION_PER_LEVEL = BUILDER
                .comment("Damage reduction percentage per enchantment level (0.1 = 10% per level)")
                .defineInRange("reduction_per_level", 0.1, 0.0, 1.0);
        
        DEATHS_DANCE_HEAL_PERCENT = BUILDER
                .comment("Healing on takedown as percentage of weapon AD (0.75 = 75%)")
                .defineInRange("heal_percent", 0.75, 0.0, 10.0);
        
        DEATHS_DANCE_DAMAGE_INTERVAL_TICKS = BUILDER
                .comment("Interval between delayed damage applications in ticks (20 ticks = 1 second)")
                .defineInRange("damage_interval_ticks", 20, 1, 100);
        
        BUILDER.pop();
        
        // Lifeline enchantment settings
        BUILDER.comment("Lifeline Enchantment Settings").push("lifeline");
        
        LIFELINE_HEALTH_THRESHOLD = BUILDER
                .comment("Health percentage threshold to trigger shield (0.3 = 30%)")
                .defineInRange("health_threshold", 0.3, 0.0, 1.0);
        
        LIFELINE_COOLDOWN_TICKS = BUILDER
                .comment("Cooldown in ticks before Lifeline can trigger again (1800 ticks = 90 seconds)")
                .defineInRange("cooldown_ticks", 1800, 0, 12000);
        
        LIFELINE_SHIELD_MULTIPLIER = BUILDER
                .comment("Multiplier for shield strength calculation (higher = stronger shield)")
                .defineInRange("shield_multiplier", 0.6, 0.0, 5.0);
        
        LIFELINE_SHIELD_DURATION_TICKS = BUILDER
                .comment("Duration of shield effect in ticks (180 ticks = 9 seconds)")
                .defineInRange("shield_duration_ticks", 180, 1, 600);
        
        BUILDER.pop();
        
        // Nightstalker enchantment settings
        BUILDER.comment("Nightstalker Enchantment Settings").push("nightstalker");
        
        NIGHTSTALKER_DAMAGE_PER_LEVEL = BUILDER
                .comment("Max bonus damage per level based on missing health (0.225 = 22.5% per level, scales with missing health)")
                .defineInRange("damage_per_level", 0.225, 0.0, 1.0);
        
        NIGHTSTALKER_INVISIBILITY_DURATION = BUILDER
                .comment("Duration of invulnerability on takedown in ticks (30 ticks = 1.5 seconds)")
                .defineInRange("invisibility_duration", 30, 0, 200);
        
        NIGHTSTALKER_KILL_CREDIT_WINDOW = BUILDER
                .comment("Time window in ticks to get kill credit after damaging (60 ticks = 3 seconds)")
                .defineInRange("kill_credit_window", 60, 1, 600);
        
        BUILDER.pop();

        BUILDER.pop();
        CONFIG = BUILDER.build();
    }
}
