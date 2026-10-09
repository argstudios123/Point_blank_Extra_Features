package com.argos.pbextra.mixin;

import com.argos.pbextra.data.MeleeConfig;
import com.argos.pbextra.data.WeaponExtraDataManager;
import com.argos.pbextra.melee.MeleeAnimationController;
import com.argos.pbextra.melee.MeleeConstants;
import com.vicmatskiv.pointblank.item.GunItem;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;


@Mixin(value = GunItem.class, remap = false)
public abstract class GunItemMeleeControllerMixin {

    @Inject(method = "registerControllers", at = @At("RETURN"), remap = false)
    private void pointblankextra$addMeleeController(
            AnimatableManager.ControllerRegistrar registrar, CallbackInfo ci) {

        GunItem gun = (GunItem) (Object) this;

        var registryKey = ForgeRegistries.ITEMS.getKey(gun);
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
