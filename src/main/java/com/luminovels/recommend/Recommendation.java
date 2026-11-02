package com.luminovels.recommend;

/**
 * One ranked suggestion.
 *
 * @param id MyAnimeList identifier
 * @param title main title
 * @param score blended score
 * @param contentScore cosine similarity of TF-IDF vectors
 * @param genreScore cosine similarity of predicted and actual genre vectors
 */
public record Recommendation(long id, String title, double score, double contentScore, double genreScore) {
}
