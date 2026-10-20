package com.arg.pbextra.bullethole;

import com.vicmatskiv.pointblank.item.HurtingItem;

import java.util.ArrayDeque;
import java.util.Deque;



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
