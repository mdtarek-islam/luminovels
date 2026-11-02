package com.luminovels.cli;

/**
 * Command line entry point with the subcommands ingest, train and recommend.
 */
public final class Main {

    static final String USAGE = String.join(System.lineSeparator(),
            "usage: luminovels <command> [--key value ...]",
            "  ingest    --client-id ID (or MAL_CLIENT_ID) --out data/LightNovels.csv --max 10500 --interval-ms 1000",
            "  train     --data data/LightNovels.csv --model models/luminovels.model --epochs 100 --hidden 100",
            "            --lr 0.01 --batch 32 --seed 42 --features 2000 --min-df 2 --max-overlap 3",
            "            --activation softmax|sigmoid --test-split 0.2",
            "  benchmark --data data/LightNovels.csv --model-dir models --out-dir docs/local --activation both|softmax|sigmoid",
            "  recommend --data data/LightNovels.csv --model models/luminovels.model",
            "            (--query TEXT | --like ID,ID) --top 5 --alpha 0.7");

    private Main() {
    }

    public static void main(String[] argv) throws Exception {
        if (argv.length == 0) {
            System.err.println(USAGE);
            System.exit(2);
            return;
        }
        Args args = new Args(argv, 1);
        switch (argv[0]) {
            case "ingest" -> IngestCommand.run(args);
            case "train" -> TrainCommand.run(args);
            case "recommend" -> RecommendCommand.run(args);
            case "benchmark", "evaluate" -> BenchmarkCommand.run(args);
            default -> {
                System.err.println(USAGE);
                System.exit(2);
            }
        }
    }
}
