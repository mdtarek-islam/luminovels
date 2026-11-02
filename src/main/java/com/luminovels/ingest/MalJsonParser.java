package com.luminovels.ingest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses the JSON body of GET /v2/manga/ranking into {@link RankingPage}.
 */
public final class MalJsonParser {

    private final ObjectMapper mapper = new ObjectMapper();
    private String mediaFilter;

    /** Keeps only nodes whose media_type equals this value. Null keeps all. */
    public void setMediaFilter(String mediaType) {
        this.mediaFilter = mediaType;
    }

    public RankingPage parsePage(String json) throws IOException {
        JsonNode root = mapper.readTree(json);
        List<LightNovel> items = new ArrayList<>();
        for (JsonNode item : root.path("data")) {
            JsonNode node = item.path("node");
            if (mediaFilter == null || mediaFilter.equals(node.path("media_type").asText(""))) {
                items.add(parseNode(node));
            }
        }
        JsonNode paging = root.path("paging");
        String next = paging.hasNonNull("next") ? paging.get("next").asText() : null;
        return new RankingPage(items, next);
    }

    private static LightNovel parseNode(JsonNode node) {
        List<String> genres = new ArrayList<>();
        for (JsonNode genre : node.path("genres")) {
            String name = genre.path("name").asText("");
            if (!name.isBlank()) {
                genres.add(name);
            }
        }
        List<String> authors = new ArrayList<>();
        for (JsonNode author : node.path("authors")) {
            JsonNode person = author.path("node");
            String full = (person.path("first_name").asText("") + " " + person.path("last_name").asText("")).trim();
            if (!full.isEmpty()) {
                authors.add(full);
            }
        }
        return new LightNovel(
                node.path("id").asLong(),
                node.path("title").asText(""),
                node.path("mean").asDouble(0.0),
                genres,
                authors,
                alternativeTitles(node.path("alternative_titles")),
                node.path("synopsis").asText(""));
    }

    private static List<String> alternativeTitles(JsonNode alt) {
        List<String> titles = new ArrayList<>();
        addIfPresent(titles, alt.path("en").asText(""));
        addIfPresent(titles, alt.path("ja").asText(""));
        for (JsonNode synonym : alt.path("synonyms")) {
            addIfPresent(titles, synonym.asText(""));
        }
        return titles;
    }

    private static void addIfPresent(List<String> titles, String value) {
        if (!value.isBlank()) {
            titles.add(value);
        }
    }
}
