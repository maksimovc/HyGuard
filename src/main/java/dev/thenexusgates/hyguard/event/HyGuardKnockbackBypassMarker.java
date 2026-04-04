package dev.thenexusgates.hyguard.event;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public final class HyGuardKnockbackBypassMarker implements Component<EntityStore> {

    private static ComponentType<EntityStore, HyGuardKnockbackBypassMarker> type;

    @Override
    public Component<EntityStore> clone() {
        return new HyGuardKnockbackBypassMarker();
    }

    public static ComponentType<EntityStore, HyGuardKnockbackBypassMarker> getComponentType() {
        return type;
    }

    public static void register(ComponentType<EntityStore, HyGuardKnockbackBypassMarker> componentType) {
        type = componentType;
    }
}