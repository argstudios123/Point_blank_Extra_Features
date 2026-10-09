package com.argos.pbextra.mixin;

import com.argos.pbextra.condition.ConditionCarrier;
import com.google.gson.JsonElement;
import com.vicmatskiv.pointblank.feature.ConditionContext;
import com.vicmatskiv.pointblank.util.Conditions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Predicate;



@Mixin(value = Conditions.class, remap = false)
public abstract class ConditionsCarrierMixin {

    @Inject(method = "fromJson", at = @At("HEAD"), cancellable = true, remap = false)
    private static void pointblankextra$unwrapCarrier(
            JsonElement condition, CallbackInfoReturnable<Predicate<ConditionContext>> cir) {

        JsonElement expression = ConditionCarrier.unwrapCondition(condition);
        if (expression != null) {
            cir.setReturnValue(Conditions.fromJson(expression));
        }
    }
}
