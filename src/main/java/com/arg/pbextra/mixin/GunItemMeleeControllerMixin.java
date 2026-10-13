package com.arg.pbextra.mixin;

import com.arg.pbextra.data.MeleeConfig;
import com.arg.pbextra.data.WeaponExtraDataManager;
import com.arg.pbextra.melee.MeleeAnimationController;
import com.arg.pbextra.melee.MeleeConstants;
import com.vicmatskiv.pointblank.item.GunItem;
import net.minecraft.core.registries.BuiltInRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;


@Mixin(value = GunItem.class, remap = false)
public abstract class GunItemMeleeControllerMixin {

    @Inject(method = "registerControllers", at = @At("RETURN"), remap = false)
    private void pointblankextra$addMeleeController(
            AnimatableManager.ControllerRegistrar registrar, CallbackInfo ci) {

        GunItem gun = (GunItem) (Object) this;

        var registryKey = BuiltInRegistries.ITEM.getKey(gun);
        if (registryKey == null) {
            return;
        }

        MeleeConfig config = WeaponExtraDataManager.getData(registryKey).getMelee();
        if (!config.isEnabled()) {
            return;
        }

        AnimationController<GunItem> controller = new MeleeAnimationController(
                gun,
                MeleeConstants.MELEE_CONTROLLER
        );

        registrar.add(controller);
    }
}
