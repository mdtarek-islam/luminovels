package com.luminovels.cli;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.luminovels.model.ClassificationScores;
import com.luminovels.model.LatencyStats;
import com.luminovels.model.ModelBundle;
import com.luminovels.model.OutputActivation;
import com.luminovels.nlp.SparseVector;
import com.luminovels.preprocess.TextPipeline;
import com.luminovels.recommend.Recommender;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Trains each requested setup, saves the model and measures quality, training time and latency.
 *
 * <p>Writes repro-metrics.json and classifier-performance.svg into the output directory.
 */
final class BenchmarkCommand {

    private static final int RECOMMEND_QUERIES = 100;
    private static final int WARM_UP = 20;

    private BenchmarkCommand() {
    }

    static void run(Args args) throws Exception {
        Corpus corpus = Corpus.load(Path.of(args.get("data", "data/LightNovels.csv")), TextPipeline.defaults());
        Path outDir = Path.of(args.get("out-dir", "docs/local"));
        String which = args.get("activation", "both").toLowerCase(Locale.ROOT);
        List<OutputActivation> setups = which.equals("both")
                ? List.of(OutputActivation.SOFTMAX, OutputActivation.SIGMOID)
                : List.of(OutputActivation.valueOf(which.toUpperCase(Locale.ROOT)));
        JsonNode runs = null;
        for (OutputActivation activation : setups) {
            String name = activation.name().toLowerCase(Locale.ROOT);
            ObjectNode node = measure(corpus, args, activation, name);
            runs = BenchmarkReport.merge(outDir.resolve("repro-metrics.json"), name, node);
            System.out.println(node.toPrettyString());
        }
        Files.createDirectories(outDir);
        Files.writeString(outDir.resolve("classifier-performance.svg"), ChartSvg.render(runs));
        System.out.println("wrote " + outDir.resolve("repro-metrics.json") + " and classifier-performance.svg");
    }

    private static ObjectNode measure(Corpus corpus, Args args, OutputActivation activation, String name) throws Exception {
        TrainingRun run = TrainingRun.execute(corpus, args, activation, System.out::println);
        Path modelPath = ModelPaths.dir(args).resolve("luminovels-" + name + ".model");
        new ModelBundle(run.vectoriser(), run.genres(), run.net()).save(modelPath);

        List<SparseVector> testX = run.testVectors();
        List<double[]> testY = run.testTruth();
        ClassificationScores scores = ClassificationScores.evaluate(run.net(), testX, testY);
        LatencyStats inference = inferenceLatency(run, testX);
        LatencyStats recommend = recommendationLatency(run, testX);

        int rows = run.novels().size();
        BenchmarkReport.Context ctx = new BenchmarkReport.Context(
                "benchmark --activation " + name + " --seed " + run.config().seed(),
                args.get("data", "data/LightNovels.csv") + " (MyAnimeList v2, media_type light_novel)",
                rows, rows - testX.size(), testX.size(), run.genres().size(), run.vectoriser().dimension(),
                run.config().seed(), run.config().epochs(), run.hidden(), args.getDouble("test-split", 0.2),
                run.config().learningRate(), run.config().batchSize(), hardware(), Instant.now().toString());
        double finalLoss = run.epochLoss()[run.epochLoss().length - 1];
        return BenchmarkReport.runNode(name, ctx, scores, run.trainNanos() / 1e9, inference, recommend, finalLoss);
    }

    /** Times one forward pass per held-out sample, after a warm-up. */
    private static LatencyStats inferenceLatency(TrainingRun run, List<SparseVector> testX) {
        for (int i = 0; i < Math.min(WARM_UP, testX.size()); i++) {
            run.net().predict(testX.get(i));
        }
        long[] nanos = new long[testX.size()];
        for (int i = 0; i < nanos.length; i++) {
            long t = System.nanoTime();
            run.net().predict(testX.get(i));
            nanos[i] = System.nanoTime() - t;
        }
        return LatencyStats.fromNanos(nanos);
    }

    /** Times a top 5 hybrid recommendation over the whole kept corpus for held-out entries as profiles. */
    private static LatencyStats recommendationLatency(TrainingRun run, List<SparseVector> testX) {
        List<SparseVector> vectors = new ArrayList<>();
        List<double[]> genreVectors = new ArrayList<>();
        for (int i = 0; i < run.novels().size(); i++) {
            vectors.add(run.vectoriser().transform(run.docs().get(i)));
            genreVectors.add(run.genres().multiHot(run.novels().get(i)));
        }
        Recommender recommender = new Recommender(run.novels(), vectors, genreVectors, run.net(), 0.7);
        int queries = Math.min(RECOMMEND_QUERIES, testX.size());
        for (int i = 0; i < Math.min(WARM_UP, queries); i++) {
            recommender.recommend(testX.get(i), Set.of(), 5);
        }
        long[] nanos = new long[queries];
        for (int i = 0; i < queries; i++) {
            long t = System.nanoTime();
            recommender.recommend(testX.get(i), Set.of(), 5);
            nanos[i] = System.nanoTime() - t;
        }
        return LatencyStats.fromNanos(nanos);
    }

    static String hardware() {
        String cpu = System.getenv("PROCESSOR_IDENTIFIER");
        return (cpu == null ? System.getProperty("os.arch") : cpu) + ", "
                + Runtime.getRuntime().availableProcessors() + " logical cores, "
                + System.getProperty("os.name") + ", Java " + System.getProperty("java.version");
    }
}
