package dev.thenexusgates.hyguard.core.protection;

import dev.thenexusgates.hyguard.core.region.RegionFlag;

public enum ProtectionAction {
    BLOCK_BREAK(RegionFlag.BLOCK_BREAK),
    BLOCK_PLACE(RegionFlag.BLOCK_PLACE),
    BLOCK_INTERACT(RegionFlag.BLOCK_INTERACT),
    PVP(RegionFlag.PVP),
    PLAYER_DAMAGE(RegionFlag.PLAYER_DAMAGE),
    PLAYER_FALL_DAMAGE(RegionFlag.PLAYER_FALL_DAMAGE),
    PLAYER_ITEM_DROP(RegionFlag.PLAYER_ITEM_DROP),
    PLAYER_ITEM_PICKUP(RegionFlag.PLAYER_ITEM_PICKUP),
    MOB_DAMAGE_PLAYERS(RegionFlag.MOB_DAMAGE_PLAYERS),
    ENTITY_DAMAGE(RegionFlag.ENTITY_DAMAGE),
    ENTRY(RegionFlag.ENTRY),
    EXIT(RegionFlag.EXIT);

    private final RegionFlag flag;

    ProtectionAction(RegionFlag flag) {
        this.flag = flag;
    }

    public RegionFlag getFlag() {
        return flag;
    }
}