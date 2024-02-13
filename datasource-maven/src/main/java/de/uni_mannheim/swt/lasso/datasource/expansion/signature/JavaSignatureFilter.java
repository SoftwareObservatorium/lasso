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

import java.util.Set;

/**
 * Allow Java JDK types only (including primitives).
 *
 * @author Marcus Kessel
 */
public class JavaSignatureFilter extends SignatureFilter {

    public boolean accept(Signature signature) {
        // note: lower case comparison

        boolean accepted = signature.getInputTypes().stream().allMatch(s ->
                (containsLowerCase(getAllowedInputTypes(), s) || isJava(s, getAllowedInputTypes())) && (containsLowerCase(getAllowedReturnTypes(), signature.getReturnType()) || isJava(signature.getReturnType(), getAllowedReturnTypes())));

        //System.out.println(signature.toMQL(true) + " => " + accepted);

        return accepted;
    }

    private boolean isJava(String s, Set<String> set) {
        if(StringUtils.isBlank(s)) {
            return false;
        }

        // handle arrays
        if(StringUtils.contains(s, "[" )) {
            s = StringUtils.substringBefore(s, "[");

            if(containsLowerCase(set, s)) {
                return true;
            }
        }

        if(s.equalsIgnoreCase("void")) {
            return true;
        }

        return StringUtils.startsWithAny(s.toLowerCase(), "java.", "javax.");

        //return s.toLowerCase().startsWithAny("java.", "javax.");// || s.toLowerCase().startsWith("javax.");
    }
}
