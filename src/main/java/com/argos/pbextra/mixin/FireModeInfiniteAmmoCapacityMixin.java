package com.argos.pbextra.mixin;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.vicmatskiv.pointblank.feature.FireModeFeature;
import com.vicmatskiv.pointblank.util.JsonUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;



@Mixin(value = FireModeFeature.Builder.class, remap = false)
public abstract class FireModeInfiniteAmmoCapacityMixin {

    @Inject(method = "withJsonObject", at = @At("HEAD"), remap = false)
    private void pointblankextra$allowInfiniteFireModeAmmoCapacity(
            JsonObject obj, CallbackInfoReturnable<FireModeFeature.Builder> cir) {
        for (JsonObject fireMode : JsonUtil.getJsonObjects(obj, "fireModes")) {
            JsonElement maxAmmoCapacity = fireMode.get("maxAmmoCapacity");
            if (maxAmmoCapacity == null || !maxAmmoCapacity.isJsonPrimitive()) {
                continue;
            }

            JsonPrimitive primitive = maxAmmoCapacity.getAsJsonPrimitive();
            if (!primitive.isString() || !"infinite".equalsIgnoreCase(primitive.getAsString().trim())) {
                continue;
            }

            // The same replacement Point Blank applies to the weapon-wide setting.
            fireMode.addProperty("maxAmmoCapacity", Integer.MAX_VALUE);
        }
    }
}
