package com.bettercontent.betterrpgprogression.mixin;

import com.bettercontent.betterrpgprogression.common.attribute.EfficiencyScaling;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "dev.ghen.thirst.content.thirst.PlayerThirst", remap = false)
abstract class ThirstPlayerMixin {
    @Unique
    private Player better_rpg_progression$player;

    @Inject(method = "tick", at = @At("HEAD"), require = 0)
    private void better_rpg_progression$rememberPlayer(Player player, CallbackInfo callback) {
        better_rpg_progression$player = player;
    }

    @ModifyVariable(
        method = "addExhaustion",
        at = @At("HEAD"),
        argsOnly = true,
        ordinal = 0,
        require = 0
    )
    private float better_rpg_progression$scaleThirstExhaustion(float amount) {
        return better_rpg_progression$player == null ? amount : EfficiencyScaling.scaleThirst(amount, better_rpg_progression$player);
    }

    @ModifyArg(
        method = "updateExhaustion",
        at = @At(
            value = "INVOKE",
            target = "Ldev/ghen/thirst/content/thirst/PlayerThirst;addExhaustion(Lnet/minecraft/world/entity/player/Player;F)V"
        ),
        index = 1,
        require = 0
    )
    private float better_rpg_progression$restoreHungerScale(float amount) {
        return better_rpg_progression$player == null ? amount : EfficiencyScaling.restoreMirroredHungerScale(amount, better_rpg_progression$player);
    }
}
