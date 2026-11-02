package com.luminovels.ingest;

import java.util.List;

/**
 * One light novel entry as fetched from the MyAnimeList ranking endpoint.
 *
 * @param id unique MyAnimeList identifier
 * @param title main title
 * @param mean mean user score on a 0.0 to 10.0 scale, 0.0 when absent
 * @param genres genre names
 * @param authors author full names
 * @param alternativeTitles English, Japanese and synonym titles
 * @param synopsis plot synopsis, empty when absent
 */
public record LightNovel(
        long id,
        String title,
        double mean,
        List<String> genres,
        List<String> authors,
        List<String> alternativeTitles,
        String synopsis) {

    public LightNovel {
        title = title == null ? "" : title;
        genres = genres == null ? List.of() : List.copyOf(genres);
        authors = authors == null ? List.of() : List.copyOf(authors);
        alternativeTitles = alternativeTitles == null ? List.of() : List.copyOf(alternativeTitles);
        synopsis = synopsis == null ? "" : synopsis;
    }
}
