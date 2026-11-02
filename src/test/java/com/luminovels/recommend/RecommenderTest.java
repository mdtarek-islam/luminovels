package com.luminovels.recommend;

import com.luminovels.ingest.LightNovel;
import com.luminovels.model.NeuralNetwork;
import com.luminovels.model.OutputActivation;
import com.luminovels.nlp.SparseVector;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RecommenderTest {

    private static LightNovel novel(long id, String title) {
        return new LightNovel(id, title, 7.0, List.of(), List.of(), List.of(), "");
    }

    private final List<LightNovel> items = List.of(novel(1, "Far"), novel(2, "Close"), novel(3, "Middle"));
    private final List<SparseVector> vectors = List.of(
            SparseVector.fromDense(new double[] {0, 0, 1}),
            SparseVector.fromDense(new double[] {1, 0.1, 0}),
            SparseVector.fromDense(new double[] {1, 1, 0}));
    private final List<double[]> genres = List.of(new double[] {1, 0}, new double[] {0, 1}, new double[] {1, 1});
    private final NeuralNetwork net = NeuralNetwork.create(3, 4, 2, OutputActivation.SOFTMAX, 1L);

    @Test
    void cosineSimilarityOfKnownVectors() {
        assertEquals(1.0, CosineSimilarity.of(new double[] {1, 2}, new double[] {2, 4}), 1e-12);
        assertEquals(0.0, CosineSimilarity.of(new double[] {1, 0}, new double[] {0, 3}), 1e-12);
        assertEquals(0.0, CosineSimilarity.of(new double[] {0, 0}, new double[] {1, 1}), 1e-12);
    }

    @Test
    void ranksByContentCosineWhenAlphaIsOne() {
        Recommender r = new Recommender(items, vectors, genres, net, 1.0);
        List<Recommendation> top = r.recommend(SparseVector.fromDense(new double[] {1, 0, 0}), Set.of(), 3);
        assertEquals(List.of(2L, 3L, 1L), top.stream().map(Recommendation::id).toList());
        assertEquals(0.0, top.get(2).score(), 1e-12);
    }

    @Test
    void excludesLikedItemsAndLimitsToTopN() {
        Recommender r = new Recommender(items, vectors, genres, net, 1.0);
        List<Recommendation> top = r.recommend(SparseVector.fromDense(new double[] {1, 0, 0}), Set.of(2L), 1);
        assertEquals(1, top.size());
        assertEquals(3L, top.get(0).id());
    }

    @Test
    void blendedScoreMixesContentAndGenreParts() {
        Recommender r = new Recommender(items, vectors, genres, net, 0.5);
        for (Recommendation rec : r.recommend(SparseVector.fromDense(new double[] {1, 0, 0}), Set.of(), 3)) {
            assertEquals(0.5 * rec.contentScore() + 0.5 * rec.genreScore(), rec.score(), 1e-12);
        }
    }
}
