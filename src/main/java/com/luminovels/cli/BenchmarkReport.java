package com.luminovels.cli;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.luminovels.model.ClassificationScores;
import com.luminovels.model.LatencyStats;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Builds and merges the repro-metrics JSON document. One entry per output activation.
 */
final class BenchmarkReport {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private BenchmarkReport() {
    }

    /** Context recorded beside the measured values. */
    record Context(String command, String dataSource, int rows, int trainRows, int testRows, int genres,
                   int features, long seed, int epochs, int hidden, double testSplit, double learningRate,
                   int batchSize, String hardware, String measuredAt) {
    }

    static ObjectNode runNode(String activation, Context ctx, ClassificationScores scores, double trainSeconds,
                              LatencyStats inference, LatencyStats recommend, double finalLoss) {
        ObjectNode run = MAPPER.createObjectNode();
        run.put("activation", activation);
        run.put("command", ctx.command());
        run.put("data_source", ctx.dataSource());
        run.put("measured_at", ctx.measuredAt());
        run.put("hardware", ctx.hardware());
        ObjectNode setup = run.putObject("setup");
        setup.put("seed", ctx.seed());
        setup.put("epochs", ctx.epochs());
        setup.put("hidden_units", ctx.hidden());
        setup.put("hidden_activation", "relu");
        setup.put("test_split", ctx.testSplit());
        setup.put("learning_rate", ctx.learningRate());
        setup.put("batch_size", ctx.batchSize());
        ObjectNode rows = run.putObject("rows");
        rows.put("total", ctx.rows());
        rows.put("train", ctx.trainRows());
        rows.put("test", ctx.testRows());
        rows.put("genres", ctx.genres());
        rows.put("features", ctx.features());
        ObjectNode quality = run.putObject("classification");
        quality.put("top1_accuracy", scores.top1Accuracy());
        quality.put("macro_f1", scores.macroF1());
        quality.put("micro_f1", scores.microF1());
        quality.put("exact_match", scores.exactMatch());
        quality.put("final_train_loss", finalLoss);
        run.put("training_wall_seconds", trainSeconds);
        run.set("inference_latency_ms", latency(inference));
        run.set("recommendation_top5_latency_ms", latency(recommend));
        return run;
    }

    private static ObjectNode latency(LatencyStats s) {
        ObjectNode node = MAPPER.createObjectNode();
        node.put("p50", s.p50Ms());
        node.put("p95", s.p95Ms());
        node.put("mean", s.meanMs());
        node.put("samples", s.samples());
        return node;
    }

    /** Reads the existing document if any, sets {@code runs.<activation>}, writes it back and returns the runs node. */
    static JsonNode merge(Path file, String activation, ObjectNode run) throws IOException {
        ObjectNode root = MAPPER.createObjectNode();
        if (Files.isRegularFile(file)) {
            JsonNode existing = MAPPER.readTree(Files.readString(file));
            if (existing instanceof ObjectNode obj) {
                root = obj;
            }
        }
        root.put("repo", "luminovels");
        ObjectNode runs = root.get("runs") instanceof ObjectNode r ? r : root.putObject("runs");
        runs.set(activation, run);
        Path parent = file.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.writeString(file, MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(root) + System.lineSeparator());
        return runs;
    }
}
