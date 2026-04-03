package dev.thenexusgates.hyguard.sound;

import com.hypixel.hytale.protocol.SoundCategory;
import com.hypixel.hytale.server.core.asset.type.soundevent.config.SoundEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.SoundUtil;
import dev.thenexusgates.hyguard.config.HyGuardConfig;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class HyGuardSounds {

    public enum Cue {
        SELECTION_POINT_ONE,
        SELECTION_POINT_TWO,
        SUCCESS,
        DELETE,
        MEMBER_ADDED,
        MEMBER_REMOVED
    }

    private final HyGuardConfig.Sounds config;
    private final Logger logger;
    private final Map<Cue, Integer> resolved = new EnumMap<>(Cue.class);

    public HyGuardSounds(HyGuardConfig.Sounds config, Logger logger) {
        this.config = config;
        this.logger = logger;
    }

    public void play(PlayerRef playerRef, Cue cue) {
        if (!config.enabled || playerRef == null) {
            return;
        }

        OptionalInt soundIndex = resolve(cue);
        if (soundIndex.isEmpty()) {
            return;
        }

        SoundUtil.playSoundEvent2dToPlayer(playerRef, soundIndex.getAsInt(), category(cue));
    }

    private OptionalInt resolve(Cue cue) {
        Integer cached = resolved.get(cue);
        if (cached != null && cached > 0) {
            return OptionalInt.of(cached);
        }

        try {
            var assetMap = SoundEvent.getAssetMap();
            for (String candidate : candidates(cue)) {
                int index = assetMap.getIndex(candidate);
                if (index > 0) {
                    resolved.put(cue, index);
                    return OptionalInt.of(index);
                }
            }
        } catch (Throwable throwable) {
            logger.log(Level.FINE, "[HyGuard] Failed to resolve sound cue " + cue, throwable);
        }

        return OptionalInt.empty();
    }

    private List<String> candidates(Cue cue) {
        return switch (cue) {
            case SELECTION_POINT_ONE -> config.selectionPointOne;
            case SELECTION_POINT_TWO -> config.selectionPointTwo;
            case SUCCESS -> config.success;
            case DELETE -> config.delete;
            case MEMBER_ADDED -> config.memberAdded;
            case MEMBER_REMOVED -> config.memberRemoved;
        };
    }

    private SoundCategory category(Cue cue) {
        return switch (cue) {
            case DELETE -> SoundCategory.SFX;
            case MEMBER_REMOVED -> SoundCategory.SFX;
            default -> SoundCategory.UI;
        };
    }
}