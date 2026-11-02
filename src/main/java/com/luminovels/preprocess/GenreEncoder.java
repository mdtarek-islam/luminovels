package com.luminovels.preprocess;

import com.luminovels.ingest.LightNovel;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * Builds the genre dictionary and encodes each entry as a binary genre vector.
 */
public final class GenreEncoder {

    private final List<String> names;
    private final Map<String, Integer> index = new HashMap<>();

    public GenreEncoder(List<String> names) {
        this.names = List.copyOf(names);
        for (int i = 0; i < this.names.size(); i++) {
            index.put(this.names.get(i), i);
        }
    }

    public static GenreEncoder fit(List<LightNovel> novels) {
        TreeSet<String> all = new TreeSet<>();
        for (LightNovel n : novels) {
            all.addAll(n.genres());
        }
        return new GenreEncoder(new ArrayList<>(all));
    }

    public int size() {
        return names.size();
    }

    public String name(int i) {
        return names.get(i);
    }

    /** Returns 1.0 for each genre the entry has and 0.0 otherwise. Unknown genres are ignored. */
    public double[] multiHot(LightNovel novel) {
        double[] v = new double[names.size()];
        for (String g : novel.genres()) {
            Integer i = index.get(g);
            if (i != null) {
                v[i] = 1.0;
            }
        }
        return v;
    }

    /** Returns the binary vector scaled to sum to 1, or null when the entry has no known genre. */
    public double[] distribution(LightNovel novel) {
        double[] v = multiHot(novel);
        double sum = 0.0;
        for (double x : v) {
            sum += x;
        }
        if (sum == 0.0) {
            return null;
        }
        for (int i = 0; i < v.length; i++) {
            v[i] /= sum;
        }
        return v;
    }

    public void write(DataOutputStream out) throws IOException {
        out.writeInt(names.size());
        for (String n : names) {
            out.writeUTF(n);
        }
    }

    public static GenreEncoder read(DataInputStream in) throws IOException {
        int n = in.readInt();
        List<String> names = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            names.add(in.readUTF());
        }
        return new GenreEncoder(names);
    }
}
