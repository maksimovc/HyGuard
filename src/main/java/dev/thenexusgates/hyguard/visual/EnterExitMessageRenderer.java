package dev.thenexusgates.hyguard.visual;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import dev.thenexusgates.hyguard.HyGuardPlugin;
import dev.thenexusgates.hyguard.core.region.Region;
import dev.thenexusgates.hyguard.core.region.RegionFlag;
import dev.thenexusgates.hyguard.core.region.RegionFlagValue;

import java.util.List;
import java.util.Map;

public final class EnterExitMessageRenderer {

    private final HyGuardPlugin plugin;

    public EnterExitMessageRenderer(HyGuardPlugin plugin) {
        this.plugin = plugin;
    }

    public void sendGreeting(PlayerRef playerRef, List<Region> currentRegions) {
        sendResolved(playerRef, currentRegions, RegionFlag.GREET_MESSAGE, false);
    }

    public void sendGreeting(PlayerRef playerRef, Region region) {
        sendResolved(playerRef, List.of(region), RegionFlag.GREET_MESSAGE, false);
    }

    public void sendFarewell(PlayerRef playerRef, List<Region> exitedRegions) {
        sendResolved(playerRef, exitedRegions, RegionFlag.FAREWELL_MESSAGE, false);
    }

    public void sendFarewell(PlayerRef playerRef, Region region) {
        sendResolved(playerRef, List.of(region), RegionFlag.FAREWELL_MESSAGE, false);
    }

    public void sendEntryDenied(PlayerRef playerRef, Region region) {
        sendCustom(playerRef, region, RegionFlag.ENTRY_DENY_MESSAGE, true);
    }

    public void sendExitDenied(PlayerRef playerRef, Region region) {
        sendCustom(playerRef, region, RegionFlag.EXIT_DENY_MESSAGE, true);
    }

    private void sendResolved(PlayerRef playerRef,
                              List<Region> regions,
                              RegionFlag messageFlag,
                              boolean fallbackToDeniedMessage) {
        String customMessage = resolveFlagText(regions, messageFlag);
        if (customMessage == null) {
            if (fallbackToDeniedMessage) {
                plugin.send(playerRef, plugin.getConfigSnapshot().messages.protectionDenied);
            }
            return;
        }
        playerRef.sendMessage(plugin.rawMessage(customMessage, Map.of("name", resolveRegionName(regions)), false));
    }

    private void sendCustom(PlayerRef playerRef, Region region, RegionFlag messageFlag, boolean fallbackToDeniedMessage) {
        String customMessage = resolveFlagText(region, messageFlag);
        if (customMessage == null) {
            if (fallbackToDeniedMessage) {
                plugin.send(playerRef, plugin.getConfigSnapshot().messages.protectionDenied);
            }
            return;
        }
        playerRef.sendMessage(plugin.rawMessage(customMessage, Map.of("name", region.getName()), false));
    }

    private String resolveFlagText(List<Region> regions, RegionFlag messageFlag) {
        if (regions == null || regions.isEmpty()) {
            return null;
        }

        for (Region region : regions) {
            String resolved = resolveDirectFlagText(region, messageFlag);
            if (resolved != null) {
                return resolved;
            }
        }
        return null;
    }

    private String resolveFlagText(Region region, RegionFlag messageFlag) {
        Region current = region;
        while (current != null) {
            String resolved = resolveDirectFlagText(current, messageFlag);
            if (resolved != null) {
                return resolved;
            }
            String parentRegionId = current.getParentRegionId();
            if (parentRegionId == null || parentRegionId.isBlank()) {
                break;
            }
            current = plugin.findRegionById(parentRegionId);
        }
        return null;
    }

    private String resolveDirectFlagText(Region region, RegionFlag messageFlag) {
        if (region == null) {
            return null;
        }
        RegionFlagValue flagValue = region.getFlags().get(messageFlag);
        if (flagValue == null || flagValue.getMode() == RegionFlagValue.Mode.INHERIT || flagValue.getTextValue() == null || flagValue.getTextValue().isBlank()) {
            return null;
        }
        return flagValue.getTextValue();
    }

    private String resolveRegionName(List<Region> regions) {
        if (regions == null || regions.isEmpty() || regions.get(0) == null || regions.get(0).getName() == null) {
            return "region";
        }
        return regions.get(0).getName();
    }
}