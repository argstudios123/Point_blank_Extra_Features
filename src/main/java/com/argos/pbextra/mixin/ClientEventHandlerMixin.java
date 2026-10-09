package com.argos.pbextra.mixin;

import com.argos.pbextra.config.PointBlankExtraConfig;
import com.argos.pbextra.data.WeaponExtraDataManager;
import com.vicmatskiv.pointblank.client.ClientEventHandler;
import com.vicmatskiv.pointblank.item.GunItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;


@Mixin(value = ClientEventHandler.class, remap = false)
public abstract class ClientEventHandlerMixin {

    @Unique
    private boolean pointblankextra$toggleState;

    @Unique
    private boolean pointblankextra$previousPhysicalState;

    @Unique
    private int pointblankextra$lastGunSlot = Integer.MIN_VALUE;

    @Unique
    private ItemStack pointblankextra$lastGunStack = ItemStack.EMPTY;

    /**
     * VPB 2.2.0's onClientTick stores the right-mouse physical state in local
     * variable slot 17. We replace that value with our toggle state.
     */
    @ModifyVariable(
            method = "onClientTick",
            at = @At("STORE"),
            index = 17,
            remap = false
    )
    private boolean pointblankextra$modifyRightMouseState(boolean physicalState) {

        Minecraft minecraft = Minecraft.getInstance();

        // The config itself is the master switch for this mod's functionality.
        if (!PointBlankExtraConfig.ENABLE_MOD.get()) {
            pointblankextra$reset(physicalState);
            return physicalState;
        }

        LocalPlayer player = minecraft.player;

        if (player == null) {
            pointblankextra$reset(physicalState);
            return physicalState;
        }

        // VPB uses RMB for firing when both gun slots are occupied.
        // Leave that special case completely under VPB's control.
        if (GunItem.hasGunsInMainAndAltSlots(player)) {
            pointblankextra$reset(physicalState);
            return physicalState;
        }

        GunItem.OperableGunContext context =
                GunItem.resolveMainHandGunContext(player);

        // No main-hand VPB gun: don't alter normal Minecraft input.
        if (context == null || context.itemStack().isEmpty()) {
            pointblankextra$reset(physicalState);
            return physicalState;
        }

        ItemStack stack = context.itemStack();

        if (!(stack.getItem() instanceof GunItem gun) || !gun.isAimingEnabled()) {
            pointblankextra$reset(physicalState);
            return physicalState;
        }

        // Per-weapon toggle aiming (JSON: "toggle_aiming": true).
        if (!WeaponExtraDataManager.isToggleAiming(stack)) {
            pointblankextra$reset(physicalState);
            return physicalState;
        }

        // Switching guns/slots starts a fresh toggle state.
        int slot = context.slotIndex();
        if (slot != pointblankextra$lastGunSlot
                || pointblankextra$lastGunStack.isEmpty()
                || pointblankextra$lastGunStack.getItem() != stack.getItem()) {
            pointblankextra$lastGunSlot = slot;
            pointblankextra$lastGunStack = stack.copy();
            pointblankextra$toggleState = false;
            pointblankextra$previousPhysicalState = physicalState;
        }

        // Detect a real physical RMB press: UP -> DOWN.
        boolean justPressed =
                physicalState && !pointblankextra$previousPhysicalState;

        pointblankextra$previousPhysicalState = physicalState;

        if (justPressed) {
            pointblankextra$toggleState = !pointblankextra$toggleState;
        }

        // This is the value VPB uses for its own press/release detection.
        return pointblankextra$toggleState;
    }

    @Unique
    private void pointblankextra$reset(boolean physicalState) {
        pointblankextra$toggleState = false;
        pointblankextra$previousPhysicalState = physicalState;
        pointblankextra$lastGunSlot = Integer.MIN_VALUE;
        pointblankextra$lastGunStack = ItemStack.EMPTY;
    }
}
