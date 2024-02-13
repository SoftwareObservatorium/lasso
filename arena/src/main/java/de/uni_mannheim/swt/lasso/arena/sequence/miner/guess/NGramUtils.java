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

import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/*
Copyright (c) 2022, Chair of Software Technology
All rights reserved.

Redistribution and use in source and binary forms, with or without modification,
are permitted provided that the following conditions are met:

Redistributions of source code must retain the above copyright notice,
this list of conditions and the following disclaimer.

Redistributions in binary form must reproduce the above copyright notice,
this list of conditions and the following disclaimer in the documentation and/or
other materials provided with the distribution.

Neither the name of the University Mannheim nor the names of its contributors
may be used to endorse or promote products derived from this software without
specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY,
OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

/** Utility class to calculate the jaccard similarity between two words based on n-grams
 * @author Malte Brockmeier
 */
public class NGramUtils {
    public static double calculateJaccardDistance(String term1, String term2, int n) {
        ArrayList<String> term1NGrams = getNGrams(n, term1);
        ArrayList<String> term2NGrams = getNGrams(n, term2);

        ArrayList<String> distinctNGrams = Stream.concat(term1NGrams.stream(), term2NGrams.stream()).distinct().collect(Collectors.toCollection(ArrayList::new));
        ArrayList<String> commonNGrams = term1NGrams.stream().filter(term2NGrams::contains).collect(Collectors.toCollection(ArrayList::new));

        double jaccard = ((double) commonNGrams.size()) / distinctNGrams.size();

        return jaccard;
    }

    public static double calculateJaccardDistance(String term1, String term2) {
        return calculateJaccardDistance(term1, term2, 3);
    }

    public static ArrayList<String> getNGrams(int n, String term) {
        ArrayList<String> nGrams = new ArrayList<>();

        if (term == null) return nGrams;

        term = term.toLowerCase();

        for (int i = 0; (i+n) <= term.length(); i++) {
            String nGram = term.substring(i, (i+n));
            nGrams.add(nGram);
        }

        return nGrams;
    }
}
