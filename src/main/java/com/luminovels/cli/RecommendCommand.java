package com.luminovels.cli;

import com.luminovels.model.ModelBundle;
import com.luminovels.nlp.SparseVector;
import com.luminovels.preprocess.TextPipeline;
import com.luminovels.recommend.Recommendation;
import com.luminovels.recommend.Recommender;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Prints the top entries for a free-text query or a list of liked entry IDs.
 */
final class RecommendCommand {

    private RecommendCommand() {
    }

    static void run(Args args) throws Exception {
        TextPipeline pipeline = TextPipeline.defaults();
        ModelBundle bundle = ModelBundle.load(ModelPaths.file(args, "luminovels.model"));
        Corpus corpus = Corpus.load(Path.of(args.get("data", "data/LightNovels.csv")), pipeline);
        List<SparseVector> vectors = new ArrayList<>();
        List<double[]> genreVectors = new ArrayList<>();
        for (int i = 0; i < corpus.novels().size(); i++) {
            vectors.add(bundle.vectoriser().transform(corpus.tokens().get(i)));
            genreVectors.add(bundle.genres().multiHot(corpus.novels().get(i)));
        }

        Set<Long> liked = new HashSet<>();
        SparseVector profile;
        String query = args.get("query");
        String like = args.get("like");
        if (query != null) {
            profile = bundle.vectoriser().transform(pipeline.tokens(query));
        } else if (like != null) {
            for (String id : like.split(",")) {
                liked.add(Long.parseLong(id.trim()));
            }
            List<SparseVector> likedVectors = new ArrayList<>();
            for (int i = 0; i < corpus.novels().size(); i++) {
                if (liked.contains(corpus.novels().get(i).id())) {
                    likedVectors.add(vectors.get(i));
                }
            }
            if (likedVectors.isEmpty()) {
                throw new IllegalArgumentException("none of the liked IDs is in the dataset");
            }
            profile = SparseVector.mean(likedVectors);
        } else {
            throw new IllegalArgumentException("pass --query TEXT or --like ID,ID");
        }

        Recommender recommender = new Recommender(corpus.novels(), vectors, genreVectors,
                bundle.network(), args.getDouble("alpha", 0.7));
        List<Recommendation> top = recommender.recommend(profile, liked, args.getInt("top", 5));
        for (Recommendation r : top) {
            System.out.println(String.format(Locale.ROOT, "%d\t%.4f\t%s", r.id(), r.score(), r.title()));
        }
        if (top.isEmpty()) {
            System.out.println("no entries to recommend");
        }
    }
}
