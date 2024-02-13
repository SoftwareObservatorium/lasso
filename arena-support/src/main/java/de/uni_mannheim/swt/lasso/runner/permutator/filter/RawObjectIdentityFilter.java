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
package de.uni_mannheim.swt.lasso.runner.permutator.filter;

import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Marcus Kessel
 */
public class RawObjectIdentityFilter implements RawMethodFilter {

    public static final Set<String> METHOD_NAMES = new HashSet<>(
            Arrays.asList("clone", "toString", "equals", "hashCode", "finalize"));

    @Override
    public boolean accept(String methodName, String[] paramTypes, String returnType) {
        if(METHOD_NAMES.contains(methodName)) {
            if(paramTypes.length == 0) {
                return false;
            }

            // is equals(Object)?
            if(paramTypes.length == 1 &&
                    StringUtils.equals(methodName, "equals") &&
                    StringUtils.equals("java.lang.Object", paramTypes[0])) {
                return false;
            }
        }

        return true;
    }
}
