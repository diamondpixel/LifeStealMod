package com.lifesteal.configs;

import net.minecraftforge.common.ForgeConfigSpec;

public class ClientConfig {
    // Create a builder for the client config
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec CONFIG;

    // Define client config values
    public static final ForgeConfigSpec.DoubleValue LIFESTEAL_PERCENT;
    public static final ForgeConfigSpec.DoubleValue DAMAGE_INCREASE_PERCENT;
    public static final ForgeConfigSpec.DoubleValue CLEAVE_PERCENT;
    public static final ForgeConfigSpec.IntValue CLEAVE_MAX_STACKS;

    static {
        // Optional: Group the settings under a "client" category.
        BUILDER.comment("Client configuration for Main").push("client");

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

        BUILDER.pop();
        CONFIG = BUILDER.build();
    }
}
