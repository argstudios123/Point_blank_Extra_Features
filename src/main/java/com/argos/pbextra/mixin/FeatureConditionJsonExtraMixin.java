package com.argos.pbextra.mixin;

import com.argos.pbextra.condition.ConditionCarrier;
import com.google.gson.JsonObject;
import com.vicmatskiv.pointblank.feature.ActiveMuzzleFeature;
import com.vicmatskiv.pointblank.feature.FireModeFeature;
import com.vicmatskiv.pointblank.feature.MuzzleFlashFeature;
import com.vicmatskiv.pointblank.feature.SoundFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;



@Mixin(value = {
        ActiveMuzzleFeature.Builder.class,
        FireModeFeature.Builder.class,
        MuzzleFlashFeature.Builder.class,
        SoundFeature.Builder.class
}, remap = false)
public abstract class FeatureConditionJsonExtraMixin {

    @Redirect(
            method = "withJsonObject",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/google/gson/JsonObject;getAsJsonObject(Ljava/lang/String;)Lcom/google/gson/JsonObject;",
                    ordinal = 1,
                    remap = false
            ),
            remap = false
    )
    private JsonObject pointblankextra$allowPrimitiveNestedCondition(JsonObject obj, String memberName) {
        return ConditionCarrier.wrapCondition(obj, memberName);
    }
}
