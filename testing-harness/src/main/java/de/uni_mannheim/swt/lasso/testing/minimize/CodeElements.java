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
package de.uni_mannheim.swt.lasso.testing.minimize;

import java.util.BitSet;

/**
 * Represents code elements and their coverage {@link #elements} (bit 1 == covered).
 *
 * @author Marcus Kessel
 */
public class CodeElements {

    private BitSet elements;
    private int total;

    public CodeElements(BitSet elements, int total) {
        this.elements = elements;
        this.total = total;
    }

    public CodeElements(boolean[] elements) {
        this.elements = toBitSet(elements);
        this.total = elements.length;
    }

    protected BitSet toBitSet(boolean[] elements) {
        BitSet bitSet = new BitSet(elements.length);
        for(int i = 0; i < elements.length; i++) {
            if(elements[i]) {
                bitSet.set(i);
            }
        }

        return bitSet;
    }

    public int getCovered() {
        return this.elements.cardinality();
    }

    public int getUncovered() {
        return this.total - this.elements.cardinality();
    }

    public BitSet getElements() {
        return elements;
    }

    public int getTotal() {
        return total;
    }
}

