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
package de.uni_mannheim.swt.lasso.runner.permutator.rank;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

/**
 * Name distance measure
 * 
 * @author Marcus Kessel
 *
 */
public class NameDistance {

    /**
     * Return the inverse distance of Jaro Winkler's distance measure
     * 
     * percentage as double. 1 equal, less than one less similarity.
     * 
     * @param s1
     *            String
     * @param s2
     *            another String
     * @return
     * 
     * @see StringUtils#getJaroWinklerDistance(CharSequence, CharSequence)
     */
    public static double similarity(String s1, String s2) {
        Validate.isTrue(
                StringUtils.isNotBlank(s1) && StringUtils.isNotBlank(s2));

        double distance = StringUtils.getJaroWinklerDistance(s1, s2);

//        // TODO remove
//        if (distance > -1d) {
//            System.out.println(
//                    "STRING MATCH: " + s1 + " ~ " + s2 + " = " + distance);
//        }

        return 1d - distance;
    }
}
