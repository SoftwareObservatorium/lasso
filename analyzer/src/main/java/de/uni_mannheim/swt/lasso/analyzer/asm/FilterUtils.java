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
package de.uni_mannheim.swt.lasso.analyzer.asm;

/**
 * Simple filtering utilities.
 *
 * @author Marcus Kessel
 * 
 */
public class FilterUtils {

    /**
     * Includes {@link #checkNonJdkX(String)}!!
     * 
     * @param str
     * @return
     */
    public static boolean checkNonJdk(String str) {
        return !str.startsWith("java.") && !str.startsWith("sun.")
                && !str.startsWith("com.sun.") && checkNonJdkX(str);
    }

    // ignore all extensions packages
    public static boolean checkNonJdkX(String str) {
        return !str.startsWith("javax.") && !str.startsWith("org.ietf.jgss.")
                && !str.startsWith("org.omg.")
                && !str.startsWith("org.w3c.dom.")
                && !str.startsWith("org.xml.sax.");
    }
}
