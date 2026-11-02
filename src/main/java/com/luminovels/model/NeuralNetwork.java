package com.luminovels.model;

import com.luminovels.nlp.SparseVector;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Random;

/**
 * Two-layer network: dense ReLU hidden layer, then a softmax or sigmoid output layer.
 * Weights start from Xavier uniform initialisation. Training uses mini-batch SGD.
 */
public final class NeuralNetwork {

    private static final int MAGIC = 0x4C4E4E31;

    private final int inputs;
    private final int hidden;
    private final int outputs;
    private final OutputActivation activation;
    private final double[][] w1;
    private final double[] b1;
    private final double[][] w2;
    private final double[] b2;

    private NeuralNetwork(int inputs, int hidden, int outputs, OutputActivation activation) {
        this.inputs = inputs;
        this.hidden = hidden;
        this.outputs = outputs;
        this.activation = activation;
        this.w1 = new double[hidden][inputs];
        this.b1 = new double[hidden];
        this.w2 = new double[outputs][hidden];
        this.b2 = new double[outputs];
    }

    public static NeuralNetwork create(int inputs, int hidden, int outputs, OutputActivation activation, long seed) {
        if (inputs <= 0 || hidden <= 0 || outputs <= 0) {
            throw new IllegalArgumentException("layer sizes must be positive");
        }
        NeuralNetwork net = new NeuralNetwork(inputs, hidden, outputs, activation);
        Random random = new Random(seed);
        xavier(net.w1, inputs, hidden, random);
        xavier(net.w2, hidden, outputs, random);
        return net;
    }

    private static void xavier(double[][] w, int fanIn, int fanOut, Random random) {
        double limit = Math.sqrt(6.0 / (fanIn + fanOut));
        for (double[] row : w) {
            for (int j = 0; j < row.length; j++) {
                row[j] = (random.nextDouble() * 2.0 - 1.0) * limit;
            }
        }
    }

    public int inputs() {
        return inputs;
    }

    public int outputs() {
        return outputs;
    }

    public OutputActivation activation() {
        return activation;
    }

    /** Returns output probabilities for one input. */
    public double[] predict(SparseVector x) {
        return activation.apply(outputLogits(hiddenPreActivation(x)));
    }

    /** Mean loss over the given samples. */
    public double loss(List<SparseVector> xs, List<double[]> ys) {
        double total = 0.0;
        for (int s = 0; s < xs.size(); s++) {
            total += activation.loss(predict(xs.get(s)), ys.get(s));
        }
        return total / xs.size();
    }

    /**
     * Applies one mini-batch SGD step. All gradients use the weights from before the step.
     *
     * @return mean loss of the batch before the step
     */
    public double trainBatch(List<SparseVector> xs, List<double[]> ys, double learningRate) {
        int batch = xs.size();
        double[][] dz1 = new double[batch][];
        double[][] gw2 = new double[outputs][hidden];
        double[] gb2 = new double[outputs];
        double[] gb1 = new double[hidden];
        double loss = 0.0;
        for (int s = 0; s < batch; s++) {
            SparseVector x = checkInput(xs.get(s));
            double[] z1 = hiddenPreActivation(x);
            double[] h = relu(z1);
            double[] p = activation.apply(outputLogits(z1));
            double[] y = ys.get(s);
            loss += activation.loss(p, y);
            double[] dh = new double[hidden];
            for (int o = 0; o < outputs; o++) {
                double d2 = p[o] - y[o];
                gb2[o] += d2;
                for (int j = 0; j < hidden; j++) {
                    gw2[o][j] += d2 * h[j];
                    dh[j] += w2[o][j] * d2;
                }
            }
            for (int j = 0; j < hidden; j++) {
                dh[j] = z1[j] > 0.0 ? dh[j] : 0.0;
                gb1[j] += dh[j];
            }
            dz1[s] = dh;
        }
        double step = learningRate / batch;
        for (int s = 0; s < batch; s++) {
            SparseVector x = xs.get(s);
            for (int j = 0; j < hidden; j++) {
                double d = dz1[s][j];
                if (d == 0.0) {
                    continue;
                }
                for (int k = 0; k < x.size(); k++) {
                    w1[j][x.index(k)] -= step * d * x.value(k);
                }
            }
        }
        for (int j = 0; j < hidden; j++) {
            b1[j] -= step * gb1[j];
        }
        for (int o = 0; o < outputs; o++) {
            b2[o] -= step * gb2[o];
            for (int j = 0; j < hidden; j++) {
                w2[o][j] -= step * gw2[o][j];
            }
        }
        return loss / batch;
    }

    private SparseVector checkInput(SparseVector x) {
        if (x.dimension() != inputs) {
            throw new IllegalArgumentException("expected input dimension " + inputs + ", got " + x.dimension());
        }
        return x;
    }

    private double[] hiddenPreActivation(SparseVector x) {
        checkInput(x);
        double[] z1 = b1.clone();
        for (int j = 0; j < hidden; j++) {
            double[] row = w1[j];
            for (int k = 0; k < x.size(); k++) {
                z1[j] += row[x.index(k)] * x.value(k);
            }
        }
        return z1;
    }

    private double[] outputLogits(double[] z1) {
        double[] h = relu(z1);
        double[] z2 = b2.clone();
        for (int o = 0; o < outputs; o++) {
            for (int j = 0; j < hidden; j++) {
                z2[o] += w2[o][j] * h[j];
            }
        }
        return z2;
    }

    private static double[] relu(double[] z) {
        double[] h = new double[z.length];
        for (int i = 0; i < z.length; i++) {
            h[i] = Math.max(0.0, z[i]);
        }
        return h;
    }

    public void write(DataOutputStream out) throws IOException {
        out.writeInt(MAGIC);
        out.writeInt(inputs);
        out.writeInt(hidden);
        out.writeInt(outputs);
        out.writeUTF(activation.name());
        writeMatrix(out, w1);
        writeArray(out, b1);
        writeMatrix(out, w2);
        writeArray(out, b2);
    }

    public static NeuralNetwork read(DataInputStream in) throws IOException {
        if (in.readInt() != MAGIC) {
            throw new IOException("not a LumiNovels network file");
        }
        int inputs = in.readInt();
        int hidden = in.readInt();
        int outputs = in.readInt();
        NeuralNetwork net = new NeuralNetwork(inputs, hidden, outputs, OutputActivation.valueOf(in.readUTF()));
        readMatrix(in, net.w1);
        readArray(in, net.b1);
        readMatrix(in, net.w2);
        readArray(in, net.b2);
        return net;
    }

    private static void writeMatrix(DataOutputStream out, double[][] m) throws IOException {
        for (double[] row : m) {
            writeArray(out, row);
        }
    }

    private static void writeArray(DataOutputStream out, double[] a) throws IOException {
        for (double v : a) {
            out.writeDouble(v);
        }
    }

    private static void readMatrix(DataInputStream in, double[][] m) throws IOException {
        for (double[] row : m) {
            readArray(in, row);
        }
    }

    private static void readArray(DataInputStream in, double[] a) throws IOException {
        for (int i = 0; i < a.length; i++) {
            a[i] = in.readDouble();
        }
    }
}
