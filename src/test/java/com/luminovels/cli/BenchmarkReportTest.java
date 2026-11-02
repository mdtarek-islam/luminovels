package com.luminovels.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.luminovels.model.ClassificationScores;
import com.luminovels.model.LatencyStats;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BenchmarkReportTest {

    @TempDir
    Path dir;

    private static ObjectNode sample(String activation, double accuracy) {
        BenchmarkReport.Context ctx = new BenchmarkReport.Context("benchmark", "data/x.csv", 100, 80, 20, 7, 50,
                42L, 100, 100, 0.2, 0.01, 32, "cpu", "2026-01-01T00:00:00Z");
        return BenchmarkReport.runNode(activation, ctx, new ClassificationScores(accuracy, 0.3, 0.4, 0.1), 12.5,
                LatencyStats.fromNanos(new long[] {1_000_000L, 2_000_000L, 3_000_000L, 4_000_000L}),
                LatencyStats.fromNanos(new long[] {5_000_000L}), 0.9);
    }

    @Test
    void runNodeHasRequiredKeys() {
        JsonNode run = sample("softmax", 0.5);
        assertEquals(42, run.at("/setup/seed").asInt());
        assertEquals("relu", run.at("/setup/hidden_activation").asText());
        assertEquals(100, run.at("/rows/total").asInt());
        assertEquals(20, run.at("/rows/test").asInt());
        assertEquals(0.5, run.at("/classification/top1_accuracy").asDouble());
        assertEquals(0.3, run.at("/classification/macro_f1").asDouble());
        assertEquals(0.4, run.at("/classification/micro_f1").asDouble());
        assertEquals(12.5, run.get("training_wall_seconds").asDouble());
        assertEquals(2.0, run.at("/inference_latency_ms/p50").asDouble());
        assertEquals(4.0, run.at("/inference_latency_ms/p95").asDouble());
        assertEquals(5.0, run.at("/recommendation_top5_latency_ms/p50").asDouble());
        assertTrue(run.has("command") && run.has("data_source") && run.has("hardware"));
    }

    @Test
    void mergeKeepsEarlierRunsAndChartListsBoth() throws Exception {
        Path file = dir.resolve("out/repro-metrics.json");
        BenchmarkReport.merge(file, "softmax", sample("softmax", 0.5));
        JsonNode runs = BenchmarkReport.merge(file, "sigmoid", sample("sigmoid", 0.7));
        JsonNode saved = new ObjectMapper().readTree(Files.readString(file));
        assertEquals("luminovels", saved.get("repo").asText());
        assertTrue(saved.at("/runs/softmax").isObject());
        assertTrue(saved.at("/runs/sigmoid").isObject());

        String svg = ChartSvg.render(runs);
        assertTrue(svg.startsWith("<svg"));
        assertTrue(svg.contains("softmax output") && svg.contains("sigmoid output"));
        assertTrue(svg.contains("0.700"));
        assertFalse(svg.contains("—") || svg.contains("–"));
    }
}
