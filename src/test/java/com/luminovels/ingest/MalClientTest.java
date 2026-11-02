package com.luminovels.ingest;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MalClientTest {

    private static final String PAGE_ONE = """
            {"data":[{"node":{"id":21479,"title":"Sword Art Online","mean":7.31,
              "genres":[{"id":1,"name":"Action"},{"id":10,"name":"Fantasy"}],
              "authors":[{"node":{"id":1,"first_name":"Reki","last_name":"Kawahara"},"role":"Story"}],
              "synopsis":"Trapped in a game.",
              "alternative_titles":{"synonyms":["SAO"],"en":"Sword Art Online","ja":""}},
              "ranking":{"rank":1}}],
             "paging":{"next":"https://api.myanimelist.net/v2/manga/ranking?offset=1&limit=1"}}
            """;
    private static final String PAGE_TWO = """
            {"data":[{"node":{"id":2,"title":"No Extras"}}],"paging":{}}
            """;

    /** Replays queued responses and records each request. */
    private static final class StubFetcher implements HttpFetcher {
        final Deque<HttpResult> responses = new ArrayDeque<>();
        final List<URI> requested = new ArrayList<>();
        final List<Map<String, String>> headers = new ArrayList<>();

        @Override
        public HttpResult get(URI uri, Map<String, String> h) {
            requested.add(uri);
            headers.add(h);
            return responses.removeFirst();
        }
    }

    private final List<Long> sleeps = new ArrayList<>();
    private final Sleeper recorder = sleeps::add;

    private MalClient client(StubFetcher fetcher, int maxRetries) {
        RateLimiter limiter = new RateLimiter(0L, () -> 0L, recorder);
        return new MalClient(fetcher, "test-id", limiter, recorder, maxRetries, 100L);
    }

    @Test
    void parsesAllRequestedFields() throws IOException {
        RankingPage page = new MalJsonParser().parsePage(PAGE_ONE);
        LightNovel n = page.items().get(0);
        assertEquals(21479L, n.id());
        assertEquals("Sword Art Online", n.title());
        assertEquals(7.31, n.mean(), 1e-12);
        assertEquals(List.of("Action", "Fantasy"), n.genres());
        assertEquals(List.of("Reki Kawahara"), n.authors());
        assertEquals(List.of("Sword Art Online", "SAO"), n.alternativeTitles());
        assertEquals("Trapped in a game.", n.synopsis());
        assertTrue(page.nextUrl().contains("offset=1"));
    }

    @Test
    void missingFieldsGiveDefaultsAndNoNextPage() throws IOException {
        RankingPage page = new MalJsonParser().parsePage(PAGE_TWO);
        LightNovel n = page.items().get(0);
        assertEquals(0.0, n.mean(), 0.0);
        assertTrue(n.genres().isEmpty());
        assertEquals("", n.synopsis());
        assertNull(page.nextUrl());
    }

    @Test
    void followsPagingRetriesOn429AndSendsClientHeader() throws Exception {
        StubFetcher fetcher = new StubFetcher();
        fetcher.responses.add(new HttpResult(429, ""));
        fetcher.responses.add(new HttpResult(200, PAGE_ONE));
        fetcher.responses.add(new HttpResult(200, PAGE_TWO));
        List<LightNovel> novels = client(fetcher, 3).fetchRanking(10);
        assertEquals(List.of(21479L, 2L), novels.stream().map(LightNovel::id).toList());
        assertEquals(3, fetcher.requested.size());
        assertTrue(fetcher.requested.get(0).toString().contains("ranking_type=novels"));
        assertEquals("test-id", fetcher.headers.get(0).get("X-MAL-CLIENT-ID"));
        assertEquals(List.of(100L), sleeps);
    }

    @Test
    void stopsAtMaxEntries() throws Exception {
        StubFetcher fetcher = new StubFetcher();
        fetcher.responses.add(new HttpResult(200, PAGE_ONE));
        assertEquals(1, client(fetcher, 0).fetchRanking(1).size());
        assertEquals(1, fetcher.requested.size());
    }

    @Test
    void failsAfterRetriesRunOutOrOnClientError() {
        StubFetcher busy = new StubFetcher();
        busy.responses.add(new HttpResult(503, ""));
        busy.responses.add(new HttpResult(503, ""));
        assertThrows(IOException.class, () -> client(busy, 1).fetchRanking(5));

        StubFetcher bad = new StubFetcher();
        bad.responses.add(new HttpResult(400, ""));
        assertThrows(IOException.class, () -> client(bad, 3).fetchRanking(5));
    }

    @Test
    void rateLimiterWaitsForTheGap() throws InterruptedException {
        long[] now = {1000L};
        RateLimiter limiter = new RateLimiter(500L, () -> now[0], recorder);
        limiter.acquire();
        now[0] = 1200L;
        limiter.acquire();
        assertEquals(List.of(300L), sleeps);
    }
}
