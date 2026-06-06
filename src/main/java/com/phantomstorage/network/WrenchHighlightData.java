package com.phantomstorage.network;

import java.util.Collections;
import java.util.List;

public final class WrenchHighlightData {
    private WrenchHighlightData() {}

    private static volatile List<LinkedStorageSyncPayload.HighlightEntry> current = Collections.emptyList();

    public static void update(List<LinkedStorageSyncPayload.HighlightEntry> entries) {
        current = List.copyOf(entries);
    }

    public static List<LinkedStorageSyncPayload.HighlightEntry> get() {
        return current;
    }
}
