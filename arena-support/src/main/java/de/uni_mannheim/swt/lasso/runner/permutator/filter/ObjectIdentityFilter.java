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

import java.lang.reflect.Method;

/**
 * java.lang.Object filter.
 *
 * @author Marcus Kessel
 *
 */
public class ObjectIdentityFilter implements MethodFilter {

    /**
     *
     */
    @Override
    public boolean accept(Method method) {
        if(RawObjectIdentityFilter.METHOD_NAMES.contains(method.getName())) {
            if(method.getParameterCount() == 0) {
                return false;
            }

            // is equals(Object)?
            if(method.getParameterCount() == 1 &&
                    StringUtils.equals(method.getName(), "equals") &&
                    method.getParameters()[0].getType().equals(Object.class)) {
                return false;
            }
        }

        return true;
    }

}

