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
package de.uni_mannheim.swt.lasso.engine.matcher;

import de.uni_mannheim.swt.lasso.core.model.Abstraction;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.AntPathMatcher;

import java.util.Arrays;

/**
 *
 * @author Marcus Kessel
 */
public class AbstractionMatcher implements EntityMatcher<Abstraction> {

    private AntPathMatcher antPathMatcher = new AntPathMatcher();

    public boolean match(String pattern, Abstraction entity) {
        if(StringUtils.equals(pattern, "*")) {
            return true;
        }

        // multi pattern
        if(StringUtils.contains(pattern, ",")) {
            String[] patterns = StringUtils.split(pattern, ',');

            return anyMatch(patterns, entity);
        }

        return antPathMatcher.match(pattern, entity.getName());
    }

    public boolean anyMatch(String[] patterns, Abstraction entity) {
        if(ArrayUtils.contains(patterns, "*")) {
            return true;
        }

        return Arrays.stream(patterns).anyMatch(pat -> match(pat, entity));
    }
}
