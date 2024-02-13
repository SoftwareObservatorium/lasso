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
package de.uni_mannheim.swt.lasso.arena.adaptation.filter;

import de.uni_mannheim.swt.lasso.arena.ClassUnderTest;
import de.uni_mannheim.swt.lasso.runner.permutator.filter.MethodFilter;
import org.apache.commons.lang3.Validate;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A method signature filter.
 *
 * @author Marcus Kessel
 */
public class CompositeSpecFilter implements SpecFilter {

    private final List<SpecFilter> filters;

    /**
     * @param filters
     *            One or more {@link MethodFilter}s
     */
    public CompositeSpecFilter(SpecFilter... filters) {
        Validate.notEmpty(filters, "SpecFilter array cannot be empty");
        this.filters = new ArrayList<>(Arrays.asList(filters));
    }

    public void addFilter(SpecFilter specFilter) {
        filters.add(specFilter);
    }

    @Override
    public boolean accept(ClassUnderTest classUnderTest, Method method) {
        return filters.stream().map(v -> v.accept(classUnderTest, method)).allMatch(v -> v);
    }
}
