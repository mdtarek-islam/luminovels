package com.luminovels.model;

import com.luminovels.nlp.TfidfVectoriser;
import com.luminovels.preprocess.GenreEncoder;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Everything the recommender needs at run time, saved as one binary file.
 *
 * @param vectoriser selected vocabulary and IDF weights
 * @param genres genre dictionary, in network output order
 * @param network trained classifier
 */
public record ModelBundle(TfidfVectoriser vectoriser, GenreEncoder genres, NeuralNetwork network) {

    public void save(Path path) throws IOException {
        Path parent = path.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(path)))) {
            vectoriser.write(out);
            genres.write(out);
            network.write(out);
        }
    }

    public static ModelBundle load(Path path) throws IOException {
        try (DataInputStream in = new DataInputStream(new BufferedInputStream(Files.newInputStream(path)))) {
            TfidfVectoriser vectoriser = TfidfVectoriser.read(in);
            GenreEncoder genres = GenreEncoder.read(in);
            NeuralNetwork network = NeuralNetwork.read(in);
            return new ModelBundle(vectoriser, genres, network);
        }
    }
}
