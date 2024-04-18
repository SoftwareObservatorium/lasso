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
package de.uni_mannheim.swt.lasso.engine.action.utils;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 *
 * @author Marcus Kessel
 */
public class JavaLangUtils {

    private static final Logger LOG = LoggerFactory
            .getLogger(JavaLangUtils.class);

    public static Pattern genericTypePattern = Pattern.compile("<(.*?)>");

    /**
     * Parse generic type information
     *
     * e.g.,
     *
     * <pre>
     *     Map<String, Integer>
     * </pre>
     *
     * @param type
     * @return
     */
    public static List<String> parseGenericType(String type) {
        Matcher matcher = genericTypePattern.matcher(type);

        List<String> genericTypes = new LinkedList<>();
        if (matcher.find()) {
            String types = matcher.group(1);

            if(StringUtils.contains(types, ",")) {
                String[] parts = StringUtils.split(types, ",");
                genericTypes = Arrays.stream(parts).map(String::trim)
                        .collect(Collectors.toList());
            } else {
                genericTypes.add(types.trim());
            }
        }

        return genericTypes;
    }
}
