package com.luminovels.nlp;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TfidfVectoriserTest {

    private static final List<List<String>> DOCS = List.of(
            List.of("sword", "sword", "magic"),
            List.of("magic", "love"),
            List.of("love", "school"));

    @Test
    void idfFollowsLogOfDocumentsOverDocumentFrequency() {
        TfidfVectoriser v = TfidfVectoriser.fit(DOCS, 1);
        assertEquals(List.of("love", "magic", "school", "sword"), v.terms());
        assertEquals(Math.log(3.0 / 1.0), v.idf("sword"), 1e-12);
        assertEquals(Math.log(3.0 / 2.0), v.idf("magic"), 1e-12);
    }

    @Test
    void transformWeightsTermFrequencyAndScalesToUnitLength() {
        TfidfVectoriser v = TfidfVectoriser.fit(DOCS, 1);
        double[] x = v.transform(DOCS.get(0)).toDense();
        double sword = 2.0 * Math.log(3.0);
        double magic = Math.log(1.5);
        double norm = Math.sqrt(sword * sword + magic * magic);
        assertEquals(sword / norm, x[3], 1e-12);
        assertEquals(magic / norm, x[1], 1e-12);
        assertEquals(0.0, x[0], 1e-12);
        assertEquals(1.0, v.transform(DOCS.get(0)).norm(), 1e-12);
    }

    @Test
    void minDfAndRestrictShrinkTheVocabulary() {
        TfidfVectoriser v = TfidfVectoriser.fit(DOCS, 2);
        assertEquals(List.of("love", "magic"), v.terms());
        TfidfVectoriser r = v.restrict(List.of("magic"));
        assertEquals(List.of("magic"), r.terms());
        assertTrue(r.transform(List.of("unknown")).size() == 0);
    }
}
