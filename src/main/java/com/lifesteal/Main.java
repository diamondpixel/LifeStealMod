package com.lifesteal;

import com.lifesteal.configs.ClientConfig;
import com.lifesteal.configs.Config;
import com.lifesteal.configs.ServerConfig;
import com.mojang.logging.LogUtils;
import net.minecraft.world.item.enchantment.Enchantment;
import com.lifesteal.enchantments.LifestealEnchantment;
import com.lifesteal.enchantments.CleaveEnchantment;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod(Main.MODID)
public class Main {
    public static final String MODID = "lifesteal";
    public static final org.slf4j.Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister<Enchantment> ENCHANTMENTS =
            DeferredRegister.create(ForgeRegistries.ENCHANTMENTS, MODID);

    public static final RegistryObject<Enchantment> LIFESTEAL_ENCHANTMENT =
            ENCHANTMENTS.register("lifesteal", LifestealEnchantment::new);

    public static final RegistryObject<Enchantment> CLEAVE_ENCHANTMENT =
            ENCHANTMENTS.register("cleave", CleaveEnchantment::new);

    private static FMLJavaModLoadingContext modLoadingContext;
    private static IEventBus modEventBus;
    public static Dist modSide;

    public Main(FMLJavaModLoadingContext context)
    {
        modLoadingContext = context;
        modEventBus = modLoadingContext.getModEventBus();

        // If we are on the client (which includes singleplayer with an integrated server),
        // register both client and server configs.
        if (FMLEnvironment.dist == net.minecraftforge.api.distmarker.Dist.CLIENT) {
            registerConfigs(true);
            modSide = net.minecraftforge.api.distmarker.Dist.CLIENT;
        } else { // Dedicated server
            registerConfigs(false);
            modSide = net.minecraftforge.api.distmarker.Dist.DEDICATED_SERVER;
        }

        modEventBus.addListener(Config::onModConfigEvent);
        ENCHANTMENTS.register(modEventBus);
        MinecraftForge.EVENT_BUS.register(this);

        logInitMessage();
    }

    /**
     * Registers configs.
     * @param registerClient if true, registers the client config as well (used in singleplayer).
     */
    private void registerConfigs(boolean registerClient)
    {
        if (registerClient) {
            modLoadingContext.registerConfig(ModConfig.Type.CLIENT, ClientConfig.CONFIG, "lifesteal-client.toml");
        }
        modLoadingContext.registerConfig(ModConfig.Type.SERVER, ServerConfig.CONFIG, "lifesteal-server.toml");
    }

    private void logInitMessage()
    {
        LOGGER.info("\033[38;5;196m╔═════════════════════════════════════════════════════════════════════╗");
        LOGGER.info("\033[38;5;196m║\033[38;5;201m ██╗      ██╗███████╗███████╗██████╗████████╗███████╗ █████╗ ██╗ \u200E \u200E \u200E \u200E \033[38;5;196m║");
        LOGGER.info("\033[38;5;196m║\033[38;5;207m ██║      ██║██╔════╝██╔════╝██╔═══╝╚══██╔══╝██╔════╝██╔══██╗██║ \u200E \u200E \u200E \u200E \033[38;5;196m║");
        LOGGER.info("\033[38;5;196m║\033[38;5;213m ██║      ██║█████╗  █████╗  █████╗    ██║   █████╗  ███████║██║ \u200E \u200E \u200E \u200E \033[38;5;196m║");
        LOGGER.info("\033[38;5;196m║\033[38;5;219m ██║      ██║██╔══╝  ██╔══╝    ██╔╝    ██║   ██╔══╝  ██╔══██║██║ \u200E \u200E \u200E \u200E \033[38;5;196m║");
        LOGGER.info("\033[38;5;196m║\033[38;5;225m ███████╗ ██║██║     ███████╗███████╗  ██║   ███████╗██║  ██║███████╗\033[38;5;196m║");
        LOGGER.info("\033[38;5;196m║\033[38;5;231m ╚══════╝ ╚═╝╚═╝     ╚══════╝╚══════╝  ╚═╝   ╚══════╝╚══════╝╚══════╝\033[38;5;196m║");
        LOGGER.info("\033[38;5;196m╠═════════════════════════════════════════════════════════════════════╣");
        LOGGER.info("\033[38;5;196m║\033[1;37m      Version 1.0.4 | Forge 1.20.1 | By Liparakis!      " +
                "\u200E \u200E \u200E \u200E \u200E \u200E \u200E \u200E \u200E \u200E \u200E \u200E \u200E \033[38;5;196m║");
        LOGGER.info("\033[38;5;196m╚═════════════════════════════════════════════════════════════════════╝\033[0m");
    }

}