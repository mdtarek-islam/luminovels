package com.luminovels.cli;

import com.luminovels.model.Metrics;
import com.luminovels.model.ModelBundle;
import com.luminovels.model.OutputActivation;
import com.luminovels.preprocess.TextPipeline;

import java.nio.file.Path;
import java.util.Locale;

/**
 * Trains the classifier on a train split, reports held-out accuracy and saves the model.
 */
final class TrainCommand {

    private TrainCommand() {
    }

    static void run(Args args) throws Exception {
        Corpus corpus = Corpus.load(Path.of(args.get("data", "data/LightNovels.csv")), TextPipeline.defaults());
        OutputActivation activation = OutputActivation.valueOf(args.get("activation", "softmax").toUpperCase(Locale.ROOT));
        TrainingRun run = TrainingRun.execute(corpus, args, activation, System.out::println);
        double accuracy = Metrics.top1Accuracy(run.net(), run.testVectors(), run.testTruth());
        System.out.println(String.format(Locale.ROOT, "held-out top-1 genre accuracy %.4f on %d entries",
                accuracy, run.testIndex().size()));
        Path modelPath = ModelPaths.file(args, "luminovels.model");
        new ModelBundle(run.vectoriser(), run.genres(), run.net()).save(modelPath);
        System.out.println("saved model to " + modelPath);
    }
}
