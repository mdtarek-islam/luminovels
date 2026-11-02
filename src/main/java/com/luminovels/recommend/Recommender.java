package com.luminovels.recommend;

import com.luminovels.ingest.LightNovel;
import com.luminovels.model.NeuralNetwork;
import com.luminovels.nlp.SparseVector;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Hybrid content-based filter.
 *
 * <p>score = alpha * cosine(profile, item TF-IDF) + (1 - alpha) * cosine(predicted genres, item genres).
 * The classifier predicts the genre probabilities of the user profile vector.
 */
public final class Recommender {

    private final List<LightNovel> items;
    private final List<SparseVector> itemVectors;
    private final List<double[]> itemGenres;
    private final NeuralNetwork network;
    private final double alpha;

    public Recommender(List<LightNovel> items, List<SparseVector> itemVectors, List<double[]> itemGenres,
                       NeuralNetwork network, double alpha) {
        if (items.size() != itemVectors.size() || items.size() != itemGenres.size()) {
            throw new IllegalArgumentException("items, vectors and genres differ in size");
        }
        if (alpha < 0.0 || alpha > 1.0) {
            throw new IllegalArgumentException("alpha must be in 0 to 1");
        }
        this.items = List.copyOf(items);
        this.itemVectors = List.copyOf(itemVectors);
        this.itemGenres = List.copyOf(itemGenres);
        this.network = network;
        this.alpha = alpha;
    }

    /** Returns the {@code topN} best items for the profile, skipping IDs in {@code exclude}. */
    public List<Recommendation> recommend(SparseVector profile, Set<Long> exclude, int topN) {
        double[] predicted = network.predict(profile);
        List<Recommendation> ranked = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            LightNovel item = items.get(i);
            if (exclude.contains(item.id())) {
                continue;
            }
            double content = CosineSimilarity.of(profile, itemVectors.get(i));
            double genre = CosineSimilarity.of(predicted, itemGenres.get(i));
            double score = alpha * content + (1.0 - alpha) * genre;
            ranked.add(new Recommendation(item.id(), item.title(), score, content, genre));
        }
        ranked.sort(Comparator.comparingDouble(Recommendation::score).reversed()
                .thenComparingLong(Recommendation::id));
        return ranked.size() > topN ? new ArrayList<>(ranked.subList(0, topN)) : ranked;
    }
}
