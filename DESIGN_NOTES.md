# Design notes

Sources: `LumiNovels_Final_Report.pdf` and `LumiNovels_Progress.pdf` in this folder. Project dates: October 2023 to April 2024.

## Taken from the reports

1. Data source: MyAnimeList API v2, `GET /v2/manga/ranking`, up to 500 entries per request, paged with `offset`.
2. Authentication: a MyAnimeList client ID sent as the `X-MAL-CLIENT-ID` header.
3. Fields: ID, title, mean score, genres, authors, synopsis and alternative titles.
4. Cache: one CSV file, `LightNovels.csv`. The reports describe 10,343 unique entries and 71 genres.
5. Text for each entry: synopsis, title, alternative titles and author names.
6. Stop words include "can" and "source". The reports found both in nearly every synopsis.
7. Genre labels: a binary vector with one position per genre in the genre dictionary.
8. TF-IDF at word level, w = tf * log(N / df). No n-grams.
9. Network: one hidden layer with ReLU, Xavier initialisation and SGD.
10. Defaults: 100 hidden units, 100 epochs and an 80:20 train and test split.
11. Recommender: cosine similarity, top 5 results, hybrid with the classifier output.

## Chosen here, where the reports are silent or differ

1. The network is written from scratch in plain Java. The reports used Deeplearning4j.
2. The default output layer is softmax with categorical cross-entropy. Each target is the entry's genre vector scaled to sum to 1. Use `--activation sigmoid` to get the reports' final configuration: sigmoid output with binary cross-entropy.
3. Plain mini-batch SGD. The reports got their best scores with Adam. This build does not include Adam.
4. Learning rate 0.01, batch size 32 and seed 42 are chosen defaults. The reports do not state them for the final model.
5. Feature selection uses chi-square, which the reports do not use. A term overlaps a genre when chi-square reaches 3.841 (p = 0.05, one degree of freedom) and the term is more common inside the genre. Terms that overlap more than 3 genres are dropped. The 2,000 highest-scoring terms are kept, and each term must appear in at least 2 documents.
6. TF-IDF vectors are scaled to unit length, so inputs stay in 0 to 1, as the reports require.
7. The tokeniser uses a fixed stop word list in place of the OpenNLP part-of-speech filter and lemmatiser from the reports.
8. The reports merged 71 genres into 26 clusters with K-means++. This build does not merge genres.
9. The reports also queried AniList through GraphQL. This build uses MyAnimeList only.
10. Ingest uses `ranking_type=all` and keeps `media_type=light_novel`. The `novels` ranking type is capped by the API. The flags are `--ranking-type`, `--media-type` and `--page-size`. The code default for `--ranking-type` is still `novels`, so pass `all` for a full run.
11. Blend: score = 0.7 * content cosine + 0.3 * cosine of the predicted and actual genre vectors.
12. Retry on status 403, 429 and 5xx, up to 5 times. The wait starts at 2 seconds and doubles. Requests are at least 1 second apart.
13. CSV and JSON parsing: a small CSV reader and writer in place of OpenCSV, and Jackson for JSON.

## Build status

The project compiles with JDK 21 (target Java 17) and Maven. `mvn -q -B test` passes all 38 tests. TF-IDF and chi-square selection fit on the training split only.

The `benchmark` command (alias `evaluate`) writes accuracy, macro and micro F1, exact match and latency percentiles to `docs/local/`.
