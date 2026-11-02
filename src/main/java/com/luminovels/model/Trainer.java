package com.luminovels.model;

import com.luminovels.nlp.SparseVector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.function.Consumer;

/**
 * Runs mini-batch SGD over shuffled data for a fixed number of epochs.
 */
public final class Trainer {

    private Trainer() {
    }

    /** Trains in place and returns the mean training loss of each epoch. */
    public static double[] train(NeuralNetwork net, List<SparseVector> xs, List<double[]> ys,
                                 TrainingConfig config, Consumer<String> log) {
        if (xs.size() != ys.size() || xs.isEmpty()) {
            throw new IllegalArgumentException("need equal, non-empty inputs and targets");
        }
        Random random = new Random(config.seed());
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < xs.size(); i++) {
            order.add(i);
        }
        double[] epochLoss = new double[config.epochs()];
        for (int epoch = 0; epoch < config.epochs(); epoch++) {
            Collections.shuffle(order, random);
            double total = 0.0;
            for (int start = 0; start < order.size(); start += config.batchSize()) {
                int end = Math.min(order.size(), start + config.batchSize());
                List<SparseVector> bx = new ArrayList<>();
                List<double[]> by = new ArrayList<>();
                for (int i = start; i < end; i++) {
                    bx.add(xs.get(order.get(i)));
                    by.add(ys.get(order.get(i)));
                }
                total += net.trainBatch(bx, by, config.learningRate()) * bx.size();
            }
            epochLoss[epoch] = total / xs.size();
            log.accept(String.format(Locale.ROOT, "epoch %d/%d loss %.4f", epoch + 1, config.epochs(), epochLoss[epoch]));
        }
        return epochLoss;
    }
}
