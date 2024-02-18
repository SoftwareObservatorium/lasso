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
package de.uni_mannheim.swt.lasso.srm.operators;

import java.util.BitSet;

/**
 * Simple model for equivalence
 *
 * @author Marcus Kessel
 */
public class Similarity {

    private final BitSet matchIndex;
    private int total;

    public Similarity(int size) {
        this.matchIndex = new BitSet(size);
        this.total = size;
    }

    public void setMatch(int index) {
        this.matchIndex.set(index);
    }

    /**
     *
     * @return total number of statements over all sequences (sheets)
     */
    public int getTotal() {
        return this.total;
    }

    /**
     *
     * @return total number of statement matches over all sequences (sheets)
     */
    public int getMatches() {
        return this.matchIndex.cardinality();
    }

    /**
     * Jaccard'ish (Intersection over Union), but for (ordered) lists
     *
     * @return
     */
    public double getSimilarity() {
        return (double) getMatches() / getTotal();
    }

    /**
     *
     * @return true if comparison resulted in equivalence, false otherwise
     */
    public boolean isEquivalent() {
        return getSimilarity() == 1d;
    }
}
