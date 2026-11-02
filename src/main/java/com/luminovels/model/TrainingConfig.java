package com.luminovels.model;

/**
 * Training hyper-parameters.
 *
 * @param epochs passes over the training set
 * @param batchSize samples per SGD step
 * @param learningRate SGD step size
 * @param seed seed for weight initialisation and shuffling
 */
public record TrainingConfig(int epochs, int batchSize, double learningRate, long seed) {

    public TrainingConfig {
        if (epochs <= 0 || batchSize <= 0 || learningRate <= 0.0) {
            throw new IllegalArgumentException("epochs, batchSize and learningRate must be positive");
        }
    }
}
