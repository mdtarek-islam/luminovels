package com.luminovels.cli;

import com.luminovels.ingest.LightNovel;
import com.luminovels.model.NeuralNetwork;
import com.luminovels.model.OutputActivation;
import com.luminovels.model.Trainer;
import com.luminovels.model.TrainingConfig;
import com.luminovels.nlp.ChiSquareSelector;
import com.luminovels.nlp.SparseVector;
import com.luminovels.nlp.TfidfVectoriser;
import com.luminovels.preprocess.GenreEncoder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;

/**
 * One full training run: split, feature selection on the train part only, and SGD.
 *
 * @param activation output activation used
 * @param vectoriser vocabulary fitted on the train split
 * @param genres genre dictionary
 * @param net trained network
 * @param novels entries kept for training and testing, in corpus order
 * @param docs tokens of each kept entry
 * @param testIndex indices into {@code novels} of the held-out entries
 * @param epochLoss mean training loss per epoch
 * @param trainNanos wall time of the training loop
 * @param config hyper-parameters used
 * @param hidden hidden units
 */
record TrainingRun(OutputActivation activation, TfidfVectoriser vectoriser, GenreEncoder genres, NeuralNetwork net,
                   List<LightNovel> novels, List<List<String>> docs, List<Integer> testIndex,
                   double[] epochLoss, long trainNanos, TrainingConfig config, int hidden) {

    static TrainingRun execute(Corpus corpus, Args args, OutputActivation activation, Consumer<String> log) {
        GenreEncoder genres = GenreEncoder.fit(corpus.novels());
        List<LightNovel> novels = new ArrayList<>();
        List<List<String>> docs = new ArrayList<>();
        for (int i = 0; i < corpus.novels().size(); i++) {
            if (genres.distribution(corpus.novels().get(i)) != null && !corpus.tokens().get(i).isEmpty()) {
                novels.add(corpus.novels().get(i));
                docs.add(corpus.tokens().get(i));
            }
        }
        if (docs.isEmpty()) {
            throw new IllegalStateException("no entries with both text and genres");
        }
        long seed = args.getLong("seed", 42L);
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < docs.size(); i++) {
            order.add(i);
        }
        Collections.shuffle(order, new Random(seed));
        int testSize = (int) Math.round(docs.size() * args.getDouble("test-split", 0.2));
        List<Integer> testIndex = new ArrayList<>(order.subList(0, testSize));
        List<Integer> trainIndex = order.subList(testSize, order.size());

        List<List<String>> trainDocs = new ArrayList<>();
        List<double[]> trainHot = new ArrayList<>();
        for (int i : trainIndex) {
            trainDocs.add(docs.get(i));
            trainHot.add(genres.multiHot(novels.get(i)));
        }
        TfidfVectoriser full = TfidfVectoriser.fit(trainDocs, args.getInt("min-df", 2));
        ChiSquareSelector selector = new ChiSquareSelector(
                args.getInt("features", 2000), args.getInt("max-overlap", 3), ChiSquareSelector.P05_THRESHOLD);
        TfidfVectoriser vectoriser = full.restrict(selector.select(trainDocs, trainHot, full.terms()));
        log.accept("entries " + docs.size() + ", train " + trainIndex.size() + ", test " + testSize
                + ", genres " + genres.size() + ", terms " + full.dimension() + " -> " + vectoriser.dimension());
        if (vectoriser.dimension() == 0) {
            throw new IllegalStateException("feature selection kept no terms");
        }

        List<SparseVector> trainX = new ArrayList<>();
        List<double[]> trainY = new ArrayList<>();
        for (int i : trainIndex) {
            trainX.add(vectoriser.transform(docs.get(i)));
            trainY.add(activation == OutputActivation.SOFTMAX
                    ? genres.distribution(novels.get(i)) : genres.multiHot(novels.get(i)));
        }
        TrainingConfig config = new TrainingConfig(
                args.getInt("epochs", 100), args.getInt("batch", 32), args.getDouble("lr", 0.01), seed);
        int hidden = args.getInt("hidden", 100);
        NeuralNetwork net = NeuralNetwork.create(vectoriser.dimension(), hidden, genres.size(), activation, seed);
        long start = System.nanoTime();
        double[] loss = Trainer.train(net, trainX, trainY, config, log);
        long nanos = System.nanoTime() - start;
        return new TrainingRun(activation, vectoriser, genres, net, novels, docs, testIndex, loss, nanos, config, hidden);
    }

    List<SparseVector> testVectors() {
        List<SparseVector> xs = new ArrayList<>();
        for (int i : testIndex) {
            xs.add(vectoriser.transform(docs.get(i)));
        }
        return xs;
    }

    List<double[]> testTruth() {
        List<double[]> ys = new ArrayList<>();
        for (int i : testIndex) {
            ys.add(genres.multiHot(novels.get(i)));
        }
        return ys;
    }
}
