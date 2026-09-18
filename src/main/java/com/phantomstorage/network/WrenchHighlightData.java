package com.phantomstorage.network;

import java.util.Collections;
import java.util.List;

public final class WrenchHighlightData {
    private WrenchHighlightData() {}

    private static volatile List<LinkedStorageSyncPayload.HighlightEntry> current = Collections.emptyList();
    private static volatile int tier = 0;

    public static void update(List<LinkedStorageSyncPayload.HighlightEntry> entries, int newTier) {
        current = List.copyOf(entries);
        tier = newTier;
    }

    public static List<LinkedStorageSyncPayload.HighlightEntry> get() {
        return current;
    }

    public static int getTier() {
        return tier;
    }
}
