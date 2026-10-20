package com.arg.pbextra.bullethole;

import com.vicmatskiv.pointblank.item.HurtingItem;

import java.util.ArrayDeque;
import java.util.Deque;


/**
 * Carries the Point Blank item that produced the block impact currently being
 * dispatched.
 * <p>
 * {@code BlockHitEvent} itself only exposes the shooter and the optional
 * projectile. The item is needed for per-gun configuration (and for telling a
 * main-hand and an off-hand gun apart in dual wield). Point Blank calls
 * {@code HurtingItem#handleBlockHit} on the firing item, so a mixin publishes it
 * here for the duration of that call. Impacts are handled on the server thread,
 * and the value is overwritten on the next hit, so a missed pop cannot leak
 * across shots.
 */
public final class BulletHoleImpactContext {

    private static final ThreadLocal<Deque<HurtingItem>> CURRENT =
            ThreadLocal.withInitial(ArrayDeque::new);

    private BulletHoleImpactContext() {
    }

    public static void push(HurtingItem item) {
        if (item != null) {
            CURRENT.get().addLast(item);
        }
    }

    public static void pop() {
        Deque<HurtingItem> stack = CURRENT.get();
        if (!stack.isEmpty()) {
            stack.removeLast();
        }
        if (stack.isEmpty()) {
            CURRENT.remove();
        }
    }

    /** The item for the block hit currently being dispatched, or {@code null}. */
    public static HurtingItem current() {
        Deque<HurtingItem> stack = CURRENT.get();
        return stack.isEmpty() ? null : stack.peekLast();
    }
}
