package dev.thenexusgates.hyguard.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.thenexusgates.hyguard.core.region.Region;

public final class RegionSerializer {

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public String serialize(Region region) {
        return gson.toJson(region);
    }

    public Region deserialize(String raw) {
        return gson.fromJson(raw, Region.class);
    }
}