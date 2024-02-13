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
package de.uni_mannheim.swt.lasso.arena.sequence.miner.guess;

import de.uni_mannheim.swt.lasso.arena.ClassUnderTest;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;

/**
 *
 * @author Marcus Kessel
 */
public class CombinedGuesser implements CutGuesser {

    @Override
    public String guess(ClassUnderTest testClass) throws IOException {
        // strategies

        // TF-IDF
        TfIdfGuesser tfidf = new TfIdfGuesser();
        String tfidfCut = null;
        try {
            tfidfCut = tfidf.guess(testClass);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if(StringUtils.isNotBlank(tfidfCut)) {
            return tfidfCut;
        }

        // Heuristic
        HeuristicGuesser heuristic = new HeuristicGuesser();
        String heuristicCut = null;
        try {
            heuristicCut = heuristic.guess(testClass);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if(StringUtils.isNotBlank(heuristicCut)) {
            return heuristicCut;
        }

        // Simple
        SimpleGuesser simple = new SimpleGuesser();
        String simpleCut = null;
        try {
            simpleCut = simple.guess(testClass);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if(StringUtils.isNotBlank(simpleCut)) {
            return simpleCut;
        }

        return null;
    }
}
