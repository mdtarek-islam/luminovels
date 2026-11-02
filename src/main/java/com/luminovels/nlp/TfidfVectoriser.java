package com.luminovels.nlp;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Word-level TF-IDF, w = tf * log(N / df), with each document vector scaled to unit length.
 */
public final class TfidfVectoriser {

    private final List<String> terms;
    private final double[] idf;
    private final Map<String, Integer> index = new HashMap<>();

    public TfidfVectoriser(List<String> terms, double[] idf) {
        if (terms.size() != idf.length) {
            throw new IllegalArgumentException("terms and idf differ in length");
        }
        this.terms = List.copyOf(terms);
        this.idf = idf.clone();
        for (int i = 0; i < this.terms.size(); i++) {
            index.put(this.terms.get(i), i);
        }
    }

    /** Learns the vocabulary and IDF weights. Terms in fewer than {@code minDf} documents are dropped. */
    public static TfidfVectoriser fit(List<List<String>> documents, int minDf) {
        Map<String, Integer> df = new TreeMap<>();
        for (List<String> doc : documents) {
            for (String term : new HashSet<>(doc)) {
                df.merge(term, 1, Integer::sum);
            }
        }
        List<String> terms = new ArrayList<>();
        List<Double> weights = new ArrayList<>();
        int n = documents.size();
        for (Map.Entry<String, Integer> e : df.entrySet()) {
            if (e.getValue() >= minDf) {
                terms.add(e.getKey());
                weights.add(Math.log((double) n / e.getValue()));
            }
        }
        double[] idf = new double[weights.size()];
        for (int i = 0; i < idf.length; i++) {
            idf[i] = weights.get(i);
        }
        return new TfidfVectoriser(terms, idf);
    }

    /** Returns a vectoriser that keeps only the given terms, in vocabulary order. */
    public TfidfVectoriser restrict(Collection<String> keep) {
        Set<String> wanted = new HashSet<>(keep);
        List<String> kept = new ArrayList<>();
        List<Double> weights = new ArrayList<>();
        for (int i = 0; i < terms.size(); i++) {
            if (wanted.contains(terms.get(i))) {
                kept.add(terms.get(i));
                weights.add(idf[i]);
            }
        }
        double[] w = new double[weights.size()];
        for (int i = 0; i < w.length; i++) {
            w[i] = weights.get(i);
        }
        return new TfidfVectoriser(kept, w);
    }

    public SparseVector transform(List<String> tokens) {
        Map<Integer, Double> tf = new HashMap<>();
        for (String token : tokens) {
            Integer i = index.get(token);
            if (i != null) {
                tf.merge(i, 1.0, Double::sum);
            }
        }
        double norm = 0.0;
        for (Map.Entry<Integer, Double> e : tf.entrySet()) {
            double w = e.getValue() * idf[e.getKey()];
            e.setValue(w);
            norm += w * w;
        }
        if (norm > 0.0) {
            double scale = 1.0 / Math.sqrt(norm);
            for (Map.Entry<Integer, Double> e : tf.entrySet()) {
                e.setValue(e.getValue() * scale);
            }
        }
        return SparseVector.of(terms.size(), tf);
    }

    public int dimension() {
        return terms.size();
    }

    public List<String> terms() {
        return terms;
    }

    public double idf(String term) {
        Integer i = index.get(term);
        return i == null ? 0.0 : idf[i];
    }

    public void write(DataOutputStream out) throws IOException {
        out.writeInt(terms.size());
        for (int i = 0; i < terms.size(); i++) {
            out.writeUTF(terms.get(i));
            out.writeDouble(idf[i]);
        }
    }

    public static TfidfVectoriser read(DataInputStream in) throws IOException {
        int n = in.readInt();
        List<String> terms = new ArrayList<>(n);
        double[] idf = new double[n];
        for (int i = 0; i < n; i++) {
            terms.add(in.readUTF());
            idf[i] = in.readDouble();
        }
        return new TfidfVectoriser(terms, idf);
    }
}
