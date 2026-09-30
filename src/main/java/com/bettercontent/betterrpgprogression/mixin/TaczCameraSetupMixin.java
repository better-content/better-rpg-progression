package com.bettercontent.betterrpgprogression.mixin;

import com.bettercontent.betterrpgprogression.common.attribute.ControlScaling;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Pseudo
@Mixin(targets = "com.tacz.guns.client.event.CameraSetupEvent", remap = false)
abstract class TaczCameraSetupMixin {
    @ModifyArg(
        method = "initialCameraRecoil",
        at = @At(
            value = "INVOKE",
            target = "Lcom/tacz/guns/resource/pojo/data/gun/GunRecoil;genPitchSplineFunction(F)Lorg/apache/commons/math3/analysis/polynomials/PolynomialSplineFunction;",
            remap = false
        ),
        index = 0,
        require = 0
    )
    private static float better_rpg_progression$reducePitchRecoil(float recoil) {
        return better_rpg_progression$reduceRecoil(recoil);
    }

    @ModifyArg(
        method = "initialCameraRecoil",
        at = @At(
            value = "INVOKE",
            target = "Lcom/tacz/guns/resource/pojo/data/gun/GunRecoil;genYawSplineFunction(F)Lorg/apache/commons/math3/analysis/polynomials/PolynomialSplineFunction;",
            remap = false
        ),
        index = 0,
        require = 0
    )
    private static float better_rpg_progression$reduceYawRecoil(float recoil) {
        return better_rpg_progression$reduceRecoil(recoil);
    }

    private static float better_rpg_progression$reduceRecoil(float recoil) {
        LocalPlayer player = Minecraft.getInstance().player;
        return player == null ? recoil : ControlScaling.scaleRecoil(recoil, player);
    }
}
