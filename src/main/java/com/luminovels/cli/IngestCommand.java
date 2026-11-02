package com.luminovels.cli;

import com.luminovels.ingest.CsvStore;
import com.luminovels.ingest.JdkHttpFetcher;
import com.luminovels.ingest.LightNovel;
import com.luminovels.ingest.MalClient;
import com.luminovels.ingest.RateLimiter;
import com.luminovels.ingest.Sleeper;
import com.luminovels.preprocess.Deduplicator;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

/**
 * Fetches the light novel ranking and writes the CSV cache.
 */
final class IngestCommand {

    private IngestCommand() {
    }

    static void run(Args args) throws Exception {
        String clientId = args.get("client-id", DotEnv.lookup("MAL_CLIENT_ID"));
        if (clientId == null || clientId.isBlank()) {
            throw new IllegalArgumentException("set --client-id or the MAL_CLIENT_ID environment variable");
        }
        Path out = Path.of(args.get("out", "data/LightNovels.csv"));
        int max = args.getInt("max", 10500);
        MalClient client = new MalClient(
                new JdkHttpFetcher(Duration.ofSeconds(30)),
                clientId,
                RateLimiter.systemClock(args.getLong("interval-ms", 1000L)),
                Sleeper.system(),
                5,
                2000L).withLog(System.err::println).withPageSize(args.getInt("page-size", MalClient.MAX_PAGE_SIZE))
                .withRankingType(args.get("ranking-type", "all"))
                .withMediaFilter(args.get("media-type", "light_novel"));
        List<LightNovel> novels = Deduplicator.byId(client.fetchRanking(max));
        CsvStore.write(out, novels);
        System.out.println("wrote " + novels.size() + " entries to " + out);
    }
}
