package com.lifesteal.commands;

import com.lifesteal.Main;
import com.lifesteal.Utility;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Main.MODID)
public class RetrieveArmorCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("retrieve_armor")
                        .requires(source -> source.hasPermission(4))
                        .then(Commands.argument("entity", EntityArgument.entity())
                                .suggests(Utility::suggestLookedAtEntity)
                                .executes(context -> {
                                    LivingEntity entity = (LivingEntity) EntityArgument.getEntity(context, "entity");
                                    int armor = entity.getArmorValue();
                                    context.getSource().sendSuccess(
                                            () -> Component.literal("Armor: " + armor),
                                            false
                                    );
                                    return 1;
                                })
                        )
        );
    }
}
