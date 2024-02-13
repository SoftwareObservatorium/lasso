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
package de.uni_mannheim.swt.lasso.arena.search;

import de.uni_mannheim.swt.lasso.arena.MethodSignature;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;

/**
 * LQL method signature model.
 *
 * @author Marcus Kessel
 */
public class LQLMethodSignature extends MethodSignature {

    private final de.uni_mannheim.swt.lasso.index.query.lql.MethodSignature delegate;

    public LQLMethodSignature(InterfaceSpecification parent, de.uni_mannheim.swt.lasso.index.query.lql.MethodSignature delegate) {
        super(parent);
        this.delegate = delegate;
    }

    @Override
    public String[] toParameterString() {
        return delegate.getParameterTypes().toArray(new String[0]);
    }

    @Override
    public String toReturnString() {
        return delegate.getReturnValue();
    }

    @Override
    public String getName() {
        return delegate.getMethodName();
    }

    /**
     * Better use {@link #getParameterTypes(Class)}
     * 
     * @return
     */
    @Override
    public Class<?>[] getParameterTypes() {
        return Arrays.stream(toParameterString()).map(p -> {
            try {
                return ClassUtils.getClass(stripGenerics(p));
            } catch (ClassNotFoundException e) {
                throw new IllegalArgumentException(e);
            }
        }).toArray(Class[]::new);
    }

    @Override
    public Class<?>[] getParameterTypes(Class<?> forClass) {
        // check if FA/CUT is passed (e.g., Fraction { simplify()->Fraction })
        return Arrays.stream(toParameterString()).map(p -> {
            if (isFA(p)) {
                return forClass;
            } else {
                try {
                    // make sure to use CUTs classloader!
                    return ClassUtils.getClass(forClass.getClassLoader(), stripGenerics(p));
                } catch (ClassNotFoundException e) {
                    throw new IllegalArgumentException(e);
                }
            }
        }).toArray(Class[]::new);
    }

    /**
     * Better use {@link #getReturnType(Class)}
     * 
     * @return
     */
    @Override
    public Class<?> getReturnType() {
        try {
            return ClassUtils.getClass(stripGenerics(toReturnString()));
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public Class<?> getReturnType(Class<?> forClass) {
        // check if FA/CUT is passed (e.g., Fraction { simplify()->Fraction })
        if (isFA(toReturnString())) {
            return forClass;
        } else {
            try {
                // make sure to use CUTs classloader!
                return ClassUtils.getClass(forClass.getClassLoader(), stripGenerics(toReturnString()));
            } catch (ClassNotFoundException e) {
                throw new IllegalArgumentException(e);
            }
        }
    }

    static String stripGenerics(String p) {
        if(StringUtils.contains(p, "<")) {
            return StringUtils.substringBefore(p, "<");
        }

        return p;
    }

    @Override
    public String getClassName() {
        return super.getClassName();
    }
}
