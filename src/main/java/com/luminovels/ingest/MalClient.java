package com.luminovels.ingest;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Pages through the MyAnimeList light novel ranking with rate limiting and retry.
 */
public final class MalClient {

    public static final String BASE_URL = "https://api.myanimelist.net/v2";
    public static final String FIELDS =
            "id,title,mean,genres,authors{first_name,last_name},synopsis,alternative_titles,media_type";
    public static final int MAX_PAGE_SIZE = 500;

    private final HttpFetcher fetcher;
    private final String clientId;
    private final RateLimiter limiter;
    private final Sleeper sleeper;
    private final int maxRetries;
    private final long backoffMillis;
    private final MalJsonParser parser = new MalJsonParser();

    private java.util.function.Consumer<String> log = s -> { };
    private int pageSize = MAX_PAGE_SIZE;
    private String rankingType = "novels";
    private String mediaFilter;

    public MalClient(HttpFetcher fetcher, String clientId, RateLimiter limiter,
                     Sleeper sleeper, int maxRetries, long backoffMillis) {
        this.fetcher = fetcher;
        this.clientId = clientId;
        this.limiter = limiter;
        this.sleeper = sleeper;
        this.maxRetries = maxRetries;
        this.backoffMillis = backoffMillis;
    }

    /** Sets the ranking type, for example novels or bypopularity. */
    public MalClient withRankingType(String type) {
        this.rankingType = type;
        return this;
    }

    /** Keeps only entries with this media_type, for example light_novel. */
    public MalClient withMediaFilter(String mediaType) {
        this.mediaFilter = mediaType;
        this.parser.setMediaFilter(mediaType);
        return this;
    }

    /** Sets the page size, 1 to 500. */
    public MalClient withPageSize(int size) {
        this.pageSize = Math.max(1, Math.min(MAX_PAGE_SIZE, size));
        return this;
    }

    /** Sets a progress logger. */
    public MalClient withLog(java.util.function.Consumer<String> sink) {
        this.log = sink;
        return this;
    }

    /** Builds the URL of the first ranking page. */
    public static String firstPageUrl(int limit) {
        return firstPageUrl("novels", limit);
    }

    /** Builds the URL of the first page for a ranking type. */
    public static String firstPageUrl(String rankingType, int limit) {
        return BASE_URL + "/manga/ranking?ranking_type=" + rankingType + "&limit=" + limit
                + "&offset=0&fields=" + URLEncoder.encode(FIELDS, StandardCharsets.UTF_8);
    }

    /** Fetches up to {@code maxEntries} entries, following the paging links. */
    public List<LightNovel> fetchRanking(int maxEntries) throws IOException, InterruptedException {
        List<LightNovel> all = new ArrayList<>();
        String url = firstPageUrl(rankingType, Math.min(pageSize, maxEntries));
        while (url != null && all.size() < maxEntries) {
            RankingPage page = parser.parsePage(fetchWithRetry(URI.create(url)));
            log.accept("page items " + page.items().size() + ", next " + (page.nextUrl() != null));
            for (LightNovel novel : page.items()) {
                if (all.size() < maxEntries) {
                    all.add(novel);
                }
            }
            url = page.items().isEmpty() && mediaFilter == null ? null : page.nextUrl();
        }
        return all;
    }

    String fetchWithRetry(URI uri) throws IOException, InterruptedException {
        Map<String, String> headers = Map.of("X-MAL-CLIENT-ID", clientId);
        for (int attempt = 0; ; attempt++) {
            limiter.acquire();
            HttpResult result = fetcher.get(uri, headers);
            if (result.status() == 200) {
                return result.body();
            }
            if (!isRetryable(result.status()) || attempt >= maxRetries) {
                throw new IOException("GET " + uri + " failed with status " + result.status());
            }
            sleeper.sleep(backoffMillis << attempt);
        }
    }

    static boolean isRetryable(int status) {
        return status == 403 || status == 429 || status >= 500;
    }
}
