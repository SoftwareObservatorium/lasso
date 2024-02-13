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
package de.uni_mannheim.swt.lasso.arena.check;

import de.uni_mannheim.swt.lasso.arena.sequence.ValueStatement;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Represents an oracle that holds the expected values
 * (typically resulting from stimulus sheets or reference implementations).
 *
 * @author Marcus Kessel
 */
public class Oracle {

    private Map<Integer, ValueStatement> expectedValues = new LinkedHashMap<>();

    public boolean hasOracle(int statement) {
        return expectedValues.containsKey(statement);
    }

    public void addExpectedValueForStatement(int statement, ValueStatement oracleValue) {
        expectedValues.put(statement, oracleValue);
    }

    public ValueStatement getExpectedValueForStatement(int statement) {
        if(!hasOracle(statement)) {
            return null;
        }

        return expectedValues.get(statement);
    }
}
