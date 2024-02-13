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

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;

/**
 * Expand method names based on Word Embeddings (e.g., {@link Word2VecRaw}).
 *
 * @author Marcus Kessel
 */
public class MethodNameExpander {

    private static final Logger LOG = LoggerFactory.getLogger(MethodNameExpander.class);

    private Word2VecRaw word2VecRaw;

    private static MethodNameExpander instance;

    private MethodNameExpander(Word2VecRaw word2VecRaw) {
        this.word2VecRaw = word2VecRaw;
    }

    public static MethodNameExpander getInstance() {
        if(instance == null) {
            String model = System.getProperty("models.embedding.code2vec");
            instance = new MethodNameExpander(new Word2VecRaw(model));
        }

        return instance;
    }

    /**
     * Get nearest (based on co-occurence) method names.
     *
     * Input method name needs to be lower-cased (see <a href="https://github.com/tech-srl/code2vec/">code2vec</a>)
     *
     * @param methodName
     * @param topN
     * @return
     */
    public LinkedHashMap<String, Double> getNearestMethodNames(String methodName, int topN) {
        if(StringUtils.isBlank(methodName)) {
            return new LinkedHashMap<>();
        }

        String name = StringUtils.lowerCase(methodName);

        LinkedHashMap<String, Double> nearestMethodNames = null;
        try {
            nearestMethodNames = word2VecRaw.getNearestWords(name, topN);
        } catch (Throwable e) {
            nearestMethodNames = new LinkedHashMap<>();
        }

        if(MapUtils.isNotEmpty(nearestMethodNames)) {
            nearestMethodNames.remove(name);
        }

        return nearestMethodNames;
    }
}
