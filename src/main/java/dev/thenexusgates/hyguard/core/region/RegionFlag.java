package dev.thenexusgates.hyguard.core.region;

import java.util.EnumSet;
import java.util.List;

public enum RegionFlag {
    BLOCK_BREAK,
    BLOCK_PLACE,
    BLOCK_INTERACT,
    BLOCK_TRAMPLE,
    BLOCK_DECAY,
    BLOCK_SPREAD,
    BLOCK_FADE,
    BLOCK_FORM,
    PVP,
    PLAYER_DAMAGE,
    PLAYER_FALL_DAMAGE,
    PLAYER_HUNGER,
    PLAYER_ITEM_DROP,
    PLAYER_ITEM_PICKUP,
    INTERACT_INVENTORY,
    MOB_DAMAGE_PLAYERS,
    MOB_SPAWN,
    MOB_SPAWN_HOSTILE,
    MOB_SPAWN_PASSIVE,
    MOB_DAMAGE_BLOCKS,
    MOB_GRIEF,
    ANIMAL_DAMAGE,
    ENTITY_DAMAGE,
    KNOCKBACK,
    FIRE_SPREAD,
    TNT,
    EXPLOSION,
    EXPLOSION_BLOCK_DAMAGE,
    LIQUID_FLOW,
    LIGHTNING,
    ENTRY,
    EXIT,
    ENTRY_DENY_MESSAGE,
    EXIT_DENY_MESSAGE,
    GREET_MESSAGE,
    FAREWELL_MESSAGE,
    ENTRY_PLAYERS,
    INVINCIBLE,
    GAME_MODE,
    WEATHER_LOCK,
    TIME_LOCK,
    FLY,
    SPAWN_LOCATION;

    private static final EnumSet<RegionFlag> HIDDEN_FLAGS = EnumSet.of(
            BLOCK_TRAMPLE,
            BLOCK_DECAY,
            BLOCK_SPREAD,
            BLOCK_FADE,
            BLOCK_FORM,
            MOB_DAMAGE_BLOCKS,
            MOB_GRIEF,
            ANIMAL_DAMAGE,
            TNT,
            LIGHTNING
    );

    public boolean isUserVisible() {
        return !HIDDEN_FLAGS.contains(this);
    }

    public static List<RegionFlag> visibleValues() {
        return EnumSet.complementOf(HIDDEN_FLAGS).stream().toList();
    }
}