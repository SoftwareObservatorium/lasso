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

import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Objects;

/**
 * Heuristics-based behaviour comparisons.
 *
 * @author Marcus Kessel
 */
public class HeuristicsBasedCorrectness implements FunctionalCorrectness {

    public static final String _INSTANCE_ = "_INSTANCE_";
    public static final String _NA_ = "_NA_";

    @Override
    public Similarity assertStringEquals(List<String> stmts, List<String> s1, List<String> s2) {
        Similarity similarity = new Similarity(stmts.size());
        for (int i = 0; i < stmts.size(); i++) {
            if(assertStringEquals(stmts.get(i), s1.get(i), s2.get(i))) {
                // record match
                similarity.setMatch(i);
            }
        }

        return similarity;
    }

    @Override
    public boolean assertStringEquals(String stmt, String s1, String s2) {
        if(isInstanceOrNa(s1) &&
                isInstanceOrNa(s2)) {
            return true;
        }

        return StringUtils.equals(s1, s2);
    }

    @Override
    public Similarity assertObjectEquals(List<String> stmts, List o1, List o2) {
        Similarity similarity = new Similarity(stmts.size());
        for (int i = 0; i < stmts.size(); i++) {
            if(assertObjectEquals(stmts.get(i), o1.get(i), o2.get(i))) {
                // record match
                similarity.setMatch(i);
            }
        }

        return similarity;
    }

    @Override
    public boolean assertObjectEquals(String stmt, Object o1, Object o2) {
        if(o1 instanceof String && o2 instanceof String) {
            if(isInstanceOrNa((String) o1) &&
                    isInstanceOrNa((String) o2)) {
                return true;
            }
        }

        return Objects.equals(o1, o2);
    }

    private boolean isInstanceOrNa(String s) {
        return isInstance(s) || isNa(s);
    }

    private boolean isInstance(String s) {
        return StringUtils.equals(s, _INSTANCE_);
    }

    private boolean isNa(String s) {
        return StringUtils.equals(s, _NA_);
    }
}
