package com.luminovels.preprocess;

import com.luminovels.ingest.LightNovel;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Keeps the first entry for each MyAnimeList ID and preserves input order.
 */
public final class Deduplicator {

    private Deduplicator() {
    }

    public static List<LightNovel> byId(List<LightNovel> novels) {
        Map<Long, LightNovel> unique = new LinkedHashMap<>();
        for (LightNovel n : novels) {
            unique.putIfAbsent(n.id(), n);
        }
        return new ArrayList<>(unique.values());
    }
}
