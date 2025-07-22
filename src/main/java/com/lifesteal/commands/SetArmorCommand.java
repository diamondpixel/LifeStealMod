package com.lifesteal.commands;

import com.lifesteal.Utility;
import com.lifesteal.Main;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.UUID;

public class SetArmorCommand
{
    /**
     * Register the "set_armor" command.
     * The command syntax is:
     *   /setarmor <target> <value>
     *
     * The <target> argument is a string (expected to be a UUID) with suggestions from our helper.
     * The <value> argument is a double (the new armor base value).
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("set_armor")
                        .requires(source -> source.hasPermission(4))
                        .then(Commands.argument("target", StringArgumentType.word())
                                .suggests(Utility::suggestLookedAtEntity)
                                .then(Commands.argument("value", DoubleArgumentType.doubleArg(0))
                                        .executes(context -> execute(context))
                                )
                        )
        );
    }

    /**
     * Executes the command:
     * - Looks up the target entity by its UUID string.
     * - Verifies that it is a LivingEntity.
     * - Calls the helper function to set its armor value.
     */
    private static int execute(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String targetUUIDStr = StringArgumentType.getString(context, "target");
        double armorValue = DoubleArgumentType.getDouble(context, "value");

        UUID targetUUID;
        try {
            targetUUID = UUID.fromString(targetUUIDStr);
        } catch (IllegalArgumentException e) {
            source.sendFailure(Component.literal("Invalid UUID: " + targetUUIDStr));
            return 0;
        }

        // Retrieve the target entity from the world.
        Entity targetEntity = source.getLevel().getEntity(targetUUID);
        if (targetEntity == null) {
            source.sendFailure(Component.literal("No entity found with UUID: " + targetUUIDStr));
            return 0;
        }
        if (!(targetEntity instanceof LivingEntity)) {
            source.sendFailure(Component.literal("Entity is not a living entity."));
            return 0;
        }
        LivingEntity living = (LivingEntity) targetEntity;

        // Use the helper function to set the entity's armor value.
        if (!Utility.setEntityArmor(living, armorValue)) {
            source.sendFailure(Component.literal("Failed to set armor. Entity may not have an armor attribute."));
            return 0;
        }

        // Set the new base armor value.
        living.getAttribute(Attributes.ARMOR).setBaseValue(armorValue);
        source.sendSuccess(
                () -> Component.literal("Set armor of " + living.getDisplayName().getString() + " to " + armorValue), true
        );
        return 1;
    }
}
