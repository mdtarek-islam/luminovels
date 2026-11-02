package com.luminovels.ingest;

/**
 * Status code and body of one HTTP response.
 *
 * @param status HTTP status code
 * @param body response body as text
 */
public record HttpResult(int status, String body) {
}
