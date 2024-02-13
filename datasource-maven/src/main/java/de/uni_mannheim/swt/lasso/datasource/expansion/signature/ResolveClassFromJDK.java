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

import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;

/**
 * @author Marcus Kessel
 */
public class ResolveClassFromJDK {

    public static final CharSequence[] PKGS = {"java.", "javax."};

    /**
     * Resolve from JDK.
     *
     * @param paramType
     * @return
     * @throws IOException
     */
    public static String findClass(String paramType) throws IOException {
        // 1. step: try ClassUtils.getClass(className) (including primitives)
        try {
            Class<?> clazz = ClassUtils.getClass(paramType);

            if(clazz.isPrimitive()) {
                return clazz.getCanonicalName();
            }

            //System.out.println(clazz.getCanonicalName() + " " + clazz.getComponentType() + " " + clazz.isPrimitive());

            if(clazz.isArray()) {
                Class<?> inner = clazz;

                while(inner.isArray()) {
                    inner = inner.getComponentType();
                }

                if(inner.isPrimitive()) {
                    return inner.getCanonicalName();
                }

                if(StringUtils.startsWithAny(inner.getCanonicalName(), PKGS)) {
                    return inner.getCanonicalName();
                }
            }

            String name = clazz.getCanonicalName();

            if(StringUtils.startsWithAny(name, PKGS)) {
                return name;
            }
        } catch (Throwable e) {
            //e.printStackTrace();
        }

        // 2. step: guess from default package
        try {
            return ClassUtils.getClass("java.lang." + paramType).getCanonicalName();
        } catch (Throwable e) {
            //e.printStackTrace();
        }

        throw new IOException(String.format("Cannot find type '%s'", paramType));
    }
}
