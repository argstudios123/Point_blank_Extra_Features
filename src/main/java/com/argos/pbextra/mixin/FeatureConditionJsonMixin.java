package com.argos.pbextra.mixin;

import com.argos.pbextra.condition.ConditionCarrier;
import com.google.gson.JsonObject;
import com.vicmatskiv.pointblank.feature.AccuracyFeature;
import com.vicmatskiv.pointblank.feature.ActiveMuzzleFeature;
import com.vicmatskiv.pointblank.feature.AimingFeature;
import com.vicmatskiv.pointblank.feature.AlternatingMuzzleFeature;
import com.vicmatskiv.pointblank.feature.AmmoCapacityFeature;
import com.vicmatskiv.pointblank.feature.DamageFeature;
import com.vicmatskiv.pointblank.feature.DurabilityFeature;
import com.vicmatskiv.pointblank.feature.FireModeFeature;
import com.vicmatskiv.pointblank.feature.MuzzleFlashFeature;
import com.vicmatskiv.pointblank.feature.RecoilFeature;
import com.vicmatskiv.pointblank.feature.ReloadFeature;
import com.vicmatskiv.pointblank.feature.ReticleFeature;
import com.vicmatskiv.pointblank.feature.ShellEjectionFeature;
import com.vicmatskiv.pointblank.feature.SkinFeature;
import com.vicmatskiv.pointblank.feature.SoundFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;



@Mixin(value = {
        AccuracyFeature.Builder.class,
        ActiveMuzzleFeature.Builder.class,
        AimingFeature.Builder.class,
        AlternatingMuzzleFeature.Builder.class,
        AmmoCapacityFeature.Builder.class,
        DamageFeature.Builder.class,
        DurabilityFeature.Builder.class,
        FireModeFeature.Builder.class,
        MuzzleFlashFeature.Builder.class,
        RecoilFeature.Builder.class,
        ReloadFeature.Builder.class,
        ReticleFeature.Builder.class,
        ShellEjectionFeature.Builder.class,
        SkinFeature.Builder.class,
        SoundFeature.Builder.class
}, remap = false)
public abstract class FeatureConditionJsonMixin {

    @Redirect(
            method = "withJsonObject",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/google/gson/JsonObject;getAsJsonObject(Ljava/lang/String;)Lcom/google/gson/JsonObject;",
                    ordinal = 0,
                    remap = false
            ),
            remap = false
    )
    private JsonObject pointblankextra$allowPrimitiveCondition(JsonObject obj, String memberName) {
        return ConditionCarrier.wrapCondition(obj, memberName);
    }
}
