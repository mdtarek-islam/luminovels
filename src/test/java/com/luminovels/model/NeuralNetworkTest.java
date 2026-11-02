package com.luminovels.model;

import com.luminovels.nlp.SparseVector;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NeuralNetworkTest {

    private static final List<SparseVector> XS = List.of(
            SparseVector.fromDense(new double[] {1, 0, 0, 0}),
            SparseVector.fromDense(new double[] {0.9, 0.1, 0, 0}),
            SparseVector.fromDense(new double[] {0, 0, 1, 0}),
            SparseVector.fromDense(new double[] {0, 0, 0.8, 0.2}));
    private static final List<double[]> YS = List.of(
            new double[] {1, 0, 0},
            new double[] {1, 0, 0},
            new double[] {0, 0, 1},
            new double[] {0, 0, 1});

    @Test
    void forwardPassReturnsOneProbabilityPerClassAndSoftmaxSumsToOne() {
        NeuralNetwork net = NeuralNetwork.create(4, 5, 3, OutputActivation.SOFTMAX, 7L);
        double[] p = net.predict(XS.get(0));
        assertEquals(3, p.length);
        double sum = 0.0;
        for (double v : p) {
            assertTrue(v > 0.0 && v < 1.0);
            sum += v;
        }
        assertEquals(1.0, sum, 1e-9);
    }

    @Test
    void sigmoidOutputsLieBetweenZeroAndOne() {
        NeuralNetwork net = NeuralNetwork.create(4, 5, 3, OutputActivation.SIGMOID, 7L);
        for (double v : net.predict(XS.get(2))) {
            assertTrue(v > 0.0 && v < 1.0);
        }
    }

    @Test
    void oneTrainingStepLowersLossOnToySet() {
        NeuralNetwork net = NeuralNetwork.create(4, 8, 3, OutputActivation.SOFTMAX, 11L);
        double before = net.loss(XS, YS);
        double reported = net.trainBatch(XS, YS, 0.05);
        double after = net.loss(XS, YS);
        assertEquals(before, reported, 1e-12);
        assertTrue(after < before, "loss " + before + " did not drop, got " + after);
    }

    @Test
    void trainerReducesLossOverEpochs() {
        NeuralNetwork net = NeuralNetwork.create(4, 8, 3, OutputActivation.SOFTMAX, 3L);
        double[] losses = Trainer.train(net, XS, YS, new TrainingConfig(50, 2, 0.1, 3L), line -> { });
        assertTrue(losses[losses.length - 1] < losses[0]);
    }

    @Test
    void seedMakesInitialisationRepeatable() {
        double[] a = NeuralNetwork.create(4, 5, 3, OutputActivation.SOFTMAX, 99L).predict(XS.get(1));
        double[] b = NeuralNetwork.create(4, 5, 3, OutputActivation.SOFTMAX, 99L).predict(XS.get(1));
        assertArrayEquals(a, b, 0.0);
    }

    @Test
    void saveThenLoadGivesIdenticalPredictions() throws IOException {
        NeuralNetwork net = NeuralNetwork.create(4, 5, 3, OutputActivation.SIGMOID, 5L);
        net.trainBatch(XS, YS, 0.1);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(bytes)) {
            net.write(out);
        }
        NeuralNetwork loaded = NeuralNetwork.read(new DataInputStream(new ByteArrayInputStream(bytes.toByteArray())));
        assertEquals(OutputActivation.SIGMOID, loaded.activation());
        assertArrayEquals(net.predict(XS.get(3)), loaded.predict(XS.get(3)), 0.0);
    }

    @Test
    void rejectsInputOfWrongDimension() {
        NeuralNetwork net = NeuralNetwork.create(4, 5, 3, OutputActivation.SOFTMAX, 1L);
        assertThrows(IllegalArgumentException.class,
                () -> net.predict(SparseVector.fromDense(new double[] {1, 0})));
    }
}
