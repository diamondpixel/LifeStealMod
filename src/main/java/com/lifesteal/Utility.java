package com.lifesteal;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class Utility {
    /**
     * Helper method used as a suggestion provider.
     * It checks what living entity the command sender is looking at within 5 blocks
     * and suggests its UUID as the command argument.
     */
    public static CompletableFuture<Suggestions> suggestLookedAtEntity(
            CommandContext<CommandSourceStack> context,
            SuggestionsBuilder builder
    ) {
        Entity viewer = context.getSource().getEntity();
        if (viewer != null) {
            // Get the viewer's eye position and look direction.
            Vec3 start = viewer.getEyePosition(1.0F);
            Vec3 end = start.add(viewer.getLookAngle().scale(5));

            // Use ProjectileUtil to get an entity hit result along the line of sight.
            EntityHitResult hitResult = ProjectileUtil.getEntityHitResult(
                    viewer.level(),
                    viewer,
                    start,
                    end,
                    new AABB(start, end),
                    e -> e instanceof LivingEntity
            );
            if (hitResult != null) {
                hitResult.getEntity();
                Entity target = hitResult.getEntity();
                // Suggest the target entity’s UUID.
                builder.suggest(target.getUUID().toString());
            }
        }
        return builder.buildFuture();
    }

    /**
     * Helper function that sets the base armor value for a living entity.
     *
     * @param living     The target living entity.
     * @param armorValue The new armor value to set.
     * @return true if the armor value was set successfully; false otherwise.
     */
    public static boolean setEntityArmor(LivingEntity living, double armorValue) {
        if (living.getAttribute(Attributes.ARMOR) == null) {
            return false;
        }
        Objects.requireNonNull(living.getAttribute(Attributes.ARMOR)).setBaseValue(armorValue);
        return true;
    }
}
