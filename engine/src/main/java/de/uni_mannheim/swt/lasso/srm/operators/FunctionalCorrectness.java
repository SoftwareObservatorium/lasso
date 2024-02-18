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

import java.util.List;

/**
 * Pair-wise determination of functional correctness.
 *
 * @author Marcus Kessel
 */
public interface FunctionalCorrectness {

    /**
     *
     *
     * @param stmts
     * @param s1
     * @param s2
     * @return Similarity
     */
    Similarity assertStringEquals(List<String> stmts, List<String> s1, List<String> s2);

    /**
     *
     *
     * @param stmt
     * @param s1
     * @param s2
     * @return
     */
    boolean assertStringEquals(String stmt, String s1, String s2);

    /**
     *
     *
     * @param stmts
     * @param o1
     * @param o2
     * @return Similarity
     */
    Similarity assertObjectEquals(List<String> stmts, List o1, List o2);

    /**
     *
     *
     * @param stmt
     * @param o1
     * @param o2
     * @return
     */
    boolean assertObjectEquals(String stmt, Object o1, Object o2);
}
