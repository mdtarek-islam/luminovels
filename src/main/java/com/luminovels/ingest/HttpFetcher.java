package com.luminovels.ingest;

import java.io.IOException;
import java.net.URI;
import java.util.Map;

/**
 * Performs one HTTP GET request. Tests inject a stub in place of the network.
 */
@FunctionalInterface
public interface HttpFetcher {

    HttpResult get(URI uri, Map<String, String> headers) throws IOException, InterruptedException;
}
