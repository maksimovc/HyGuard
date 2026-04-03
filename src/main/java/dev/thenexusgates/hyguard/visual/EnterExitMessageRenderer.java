package dev.thenexusgates.hyguard.visual;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import dev.thenexusgates.hyguard.HyGuardPlugin;
import dev.thenexusgates.hyguard.core.region.Region;
import dev.thenexusgates.hyguard.core.region.RegionFlag;
import dev.thenexusgates.hyguard.core.region.RegionFlagValue;

import java.util.Map;

public final class EnterExitMessageRenderer {

    private final HyGuardPlugin plugin;

    public EnterExitMessageRenderer(HyGuardPlugin plugin) {
        this.plugin = plugin;
    }

    public void sendGreeting(PlayerRef playerRef, Region region) {
        sendCustom(playerRef, region, RegionFlag.GREET_MESSAGE, false);
    }

    public void sendFarewell(PlayerRef playerRef, Region region) {
        sendCustom(playerRef, region, RegionFlag.FAREWELL_MESSAGE, false);
    }

    public void sendEntryDenied(PlayerRef playerRef, Region region) {
        sendCustom(playerRef, region, RegionFlag.ENTRY_DENY_MESSAGE, true);
    }

    public void sendExitDenied(PlayerRef playerRef, Region region) {
        sendCustom(playerRef, region, RegionFlag.EXIT_DENY_MESSAGE, true);
    }

    private void sendCustom(PlayerRef playerRef, Region region, RegionFlag messageFlag, boolean fallbackToDeniedMessage) {
        String customMessage = resolveFlagText(region, messageFlag);
        if (customMessage == null) {
            if (fallbackToDeniedMessage) {
                plugin.send(playerRef, plugin.getConfigSnapshot().messages.protectionDenied);
            }
            return;
        }
        playerRef.sendMessage(Message.raw(plugin.message(customMessage, Map.of("name", region.getName()))));
    }

    private String resolveFlagText(Region region, RegionFlag messageFlag) {
        if (region == null) {
            return null;
        }
        RegionFlagValue flagValue = region.getFlags().get(messageFlag);
        if (flagValue == null || flagValue.getTextValue() == null || flagValue.getTextValue().isBlank()) {
            return null;
        }
        return flagValue.getTextValue();
    }
}