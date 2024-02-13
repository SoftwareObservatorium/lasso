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
package de.uni_mannheim.swt.lasso.datasource.expansion.signature;

import org.apache.commons.lang3.StringUtils;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 *
 * @author Marcus Kessel
 */
public class SignatureFilter {

    private Set<String> allowedInputTypes = new HashSet<String>() {
        {
            add("byte");
            add("char");
            add("int");
            add("short");
            add("long");
            add("float");
            add("double");
            add("boolean");

            add("String".toLowerCase());

            add("Byte".toLowerCase());
            add("Character".toLowerCase());
            add("Integer".toLowerCase());
            add("Short".toLowerCase());
            add("Long".toLowerCase());
            add("Float".toLowerCase());
            add("Double".toLowerCase());
            add("Boolean".toLowerCase());

            add("java.lang.Byte".toLowerCase());
            add("java.lang.Character".toLowerCase());
            add("java.lang.Integer".toLowerCase());
            add("java.lang.Short".toLowerCase());
            add("java.lang.Long".toLowerCase());
            add("java.lang.Float".toLowerCase());
            add("java.lang.Double".toLowerCase());
            add("java.lang.Boolean".toLowerCase());

            add("java.lang.String".toLowerCase());

            add("byte[]");
            add("char[]");
            add("int[]");
            add("short[]");
            add("long[]");
            add("float[]");
            add("double[]");
            add("boolean[]");

            add("java.lang.String[]".toLowerCase());

            // TODO two-dimensional arrays?

            // XXX Collections
            add("java.util.List".toLowerCase());
            add("java.util.Set".toLowerCase());
            add("java.util.Map".toLowerCase());
        }
    };

    private Set<String> allowedReturnTypes = allowedInputTypes;

    public boolean accept(Signature signature) {
        // note: lower case comparison
        boolean accepted = signature.getInputTypes().stream().allMatch(s -> containsLowerCase(allowedInputTypes, s)) && containsLowerCase(allowedReturnTypes, signature.getReturnType());

        //System.out.println(signature.toMQL(true) + " => " + accepted);

        return accepted;
    }

    static boolean containsLowerCase(Set<String> set, String s) {
        if(StringUtils.isBlank(s)) {
            return false;
        }

        return set.contains(s.toLowerCase());
    }

    public void setAllowedInputTypes(Set<String> allowedInputTypes) {
        this.allowedInputTypes = allowedInputTypes.stream().map(s -> s.toLowerCase()).collect(Collectors.toSet());
    }

    public void setAllowedReturnTypes(Set<String> allowedReturnTypes) {
        this.allowedReturnTypes = allowedReturnTypes.stream().map(s -> s.toLowerCase()).collect(Collectors.toSet());
    }

    public Set<String> getAllowedInputTypes() {
        return allowedInputTypes;
    }

    public Set<String> getAllowedReturnTypes() {
        return allowedReturnTypes;
    }
}
