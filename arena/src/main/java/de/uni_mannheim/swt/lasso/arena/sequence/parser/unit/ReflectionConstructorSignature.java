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
package de.uni_mannheim.swt.lasso.arena.sequence.parser.unit;

import de.uni_mannheim.swt.lasso.arena.MethodSignature;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.Objects;

/**
 *
 * @author Marcus Kessel
 */
public class ReflectionConstructorSignature extends MethodSignature {

    private final Constructor constructor;

    public ReflectionConstructorSignature(Constructor constructor) {
        super(null); // FIXME
        this.constructor = constructor;
    }

    public Constructor getConstructor() {
        return constructor;
    }

    @Override
    public String[] toParameterString() {
        return Arrays.stream(getParameterTypes()).map(ReflectionUtils::toClassName).toArray(String[]::new);
    }

    @Override
    public String toReturnString() {
        return ReflectionUtils.toClassName(getReturnType());
    }

    @Override
    public String getName() {
        return constructor.getDeclaringClass().getSimpleName();
    }

    @Override
    public Class<?>[] getParameterTypes() {
        return constructor.getParameterTypes();
    }

    @Override
    public Class<?> getReturnType() {
        return constructor.getDeclaringClass();
    }

    @Override
    public String getClassName() {
        return constructor.getDeclaringClass().getName();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ReflectionConstructorSignature that = (ReflectionConstructorSignature) o;
        return Objects.equals(constructor, that.constructor);
    }

    @Override
    public int hashCode() {
        return Objects.hash(constructor);
    }

    @Override
    public String toLQL() {
        StringBuilder sb = new StringBuilder();
//        sb.append(className);
//        sb.append("(");

        sb.append(getName());
        sb.append("(");
        sb.append(String.join(",", toParameterString()));
        sb.append(")");
        //sb.append(toReturnString());

        return sb.toString();
    }
}
