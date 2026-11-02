package com.luminovels.ingest;

import java.util.List;

/**
 * One page of the ranking response.
 *
 * @param items entries on this page
 * @param nextUrl absolute URL of the next page, or null on the last page
 */
public record RankingPage(List<LightNovel> items, String nextUrl) {

    public RankingPage {
        items = items == null ? List.of() : List.copyOf(items);
    }
}
