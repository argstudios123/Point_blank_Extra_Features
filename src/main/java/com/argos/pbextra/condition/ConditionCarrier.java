package com.argos.pbextra.condition;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;


public final class ConditionCarrier {

   // this will be the condition carrier which fixes most of the issues which were IMPOSSIBLE on point blank mod
    public static final String EXPRESSION = "pointblankextra$expression";

    private ConditionCarrier() {
    }

    public static JsonObject wrapCondition(JsonObject owner, String memberName) {
        JsonElement value = "condition".equals(memberName) ? owner.get(memberName) : null;
        if (value == null || !value.isJsonPrimitive()) {
            // Not a condition, or a condition Point Blank can read on its own.
            return owner.getAsJsonObject(memberName);
        }

        JsonObject carrier = new JsonObject();
        carrier.add(EXPRESSION, value);
        return carrier;
    }

    public static JsonElement unwrapCondition(JsonElement condition) {
        if (condition instanceof JsonObject carrier && carrier.size() == 1) {
            JsonElement expression = carrier.get(EXPRESSION);
            if (expression != null) {
                return expression;
            }
        }
        return null;
    }
}
