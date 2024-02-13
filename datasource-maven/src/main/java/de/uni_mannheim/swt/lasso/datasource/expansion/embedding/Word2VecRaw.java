/*
 * LASSO - an Observatorium for the Dynamic Selection, Analysis and Comparison of Software
 * Copyright (C) 2024 Marcus Kessel (University of Mannheim) and LASSO contributers
 *
 * This file is part of LASSO.
 *
 * LASSO is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LASSO is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LASSO.  If not, see <https://www.gnu.org/licenses/>.
 */
package de.uni_mannheim.swt.lasso.datasource.expansion.embedding;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Read Word2Vec files (raw text files, not binary). Here for code2vec in particular.
 *
 * Depending on size, models are expensive to load into memory.
 *
 * @author Marcus Kessel
 *
 * @see <a href="https://github.com/tech-srl/code2vec">Code2Vec</a>
 * @see <a href="https://s3.amazonaws.com/code2vec/model/target_vecs.tar.gz">Method name vectors</a>
 */
// FIXME code2vec (methodNames) 305863 -- what is this value for?
public class Word2VecRaw {

    private static final Logger LOG = LoggerFactory.getLogger(Word2VecRaw.class);

    private Map<String, float[]> wordVectors;

    public Word2VecRaw(String modelPath) {
        wordVectors = new HashMap<>();
        try {
            loadModel(modelPath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void loadModel(String modelPath) throws IOException {
        if(LOG.isInfoEnabled()) {
            LOG.info("Loading model '{}'", modelPath);
        }

        try (BufferedReader br = new BufferedReader(new FileReader(modelPath))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(" ");
                String word = parts[0];

//                if(StringUtils.equals(word, "305863")) {
//                    System.out.println(line);
//                }

                float[] vector = new float[parts.length - 1];
                for (int i = 1; i < parts.length; i++) {
                    vector[i - 1] = Float.parseFloat(parts[i]);
                }

                wordVectors.put(word, vector);
            }
        } catch (IOException e) {
            LOG.warn("Loading model failed", e);

            throw e;
        }

        if(LOG.isInfoEnabled()) {
            LOG.info("Loading model finished for '{}'", modelPath);
        }
    }

    public double calculateWordSimilarity(String word1, String word2) {
        if (!wordVectors.containsKey(word1) || !wordVectors.containsKey(word2)) {
            return 0.0;
        }

        float[] vector1 = wordVectors.get(word1);
        float[] vector2 = wordVectors.get(word2);

        return calculateCosineSimilarity(vector1, vector2);
    }

    public LinkedHashMap<String, Double> getNearestWords(String queryWord, int topN) {
        if (!wordVectors.containsKey(queryWord)) {
            return new LinkedHashMap<>();
        }

        Map<String, Double> similarityMap = new HashMap<>();

        float[] queryVector = wordVectors.get(queryWord);

        for (Map.Entry<String, float[]> entry : wordVectors.entrySet()) {
            String word = entry.getKey();

            if(StringUtils.equals(word, "305863")) {
                continue;
            }

            float[] vector = entry.getValue();
            double similarity = calculateCosineSimilarity(queryVector, vector);
            similarityMap.put(word, similarity);
        }

        return similarityMap.entrySet()
                .stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(topN)
                .collect(Collectors.toMap(k -> k.getKey(), v -> v.getValue(),
                        (v1,v2) ->{ throw new RuntimeException(String.format("Duplicate key for values %s and %s", v1, v2));},
                        LinkedHashMap::new));
    }

    private double calculateCosineSimilarity(float[] vector1, float[] vector2) {
        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;
        for (int i = 0; i < vector1.length; i++) {
            if (i >= vector2.length) {
                break;
            }
            dotProduct += vector1[i] * vector2[i];
            norm1 += vector1[i] * vector1[i];
            norm2 += vector2[i] * vector2[i];
        }

        if (norm1 == 0.0 || norm2 == 0.0) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }
}

