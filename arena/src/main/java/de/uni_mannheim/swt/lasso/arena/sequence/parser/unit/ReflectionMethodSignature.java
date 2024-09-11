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

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Objects;

/**
 *
 * @author Marcus Kessel
 */
public class ReflectionMethodSignature extends MethodSignature {

    private final Method method;

    public ReflectionMethodSignature(Method method) {
        super(null); // fixme
        this.method = method;
    }

    public Method getMethod() {
        return method;
    }

    @Override
    public String[] toParameterString() {
        return Arrays.stream(getParameterTypes()).map(ReflectionUtils::toClassName).toArray(String[]::new);
    }

    @Override
    public boolean isStatic() {
        return Modifier.isStatic(method.getModifiers());
    }

    @Override
    public String toReturnString() {
        return ReflectionUtils.toClassName(getReturnType());
    }

    @Override
    public String getName() {
        return method.getName();
    }

    @Override
    public Class<?>[] getParameterTypes() {
        return method.getParameterTypes();
    }

    @Override
    public Class<?> getReturnType() {
        return method.getReturnType();
    }

    @Override
    public String getClassName() {
        return method.getDeclaringClass().getName();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ReflectionMethodSignature that = (ReflectionMethodSignature) o;
        return Objects.equals(method, that.method);
    }

    @Override
    public int hashCode() {
        return Objects.hash(method);
    }

    @Override
    public String toLQL() {
        StringBuilder sb = new StringBuilder();
//        sb.append(className);
//        sb.append("(");

        sb.append(getName());
        sb.append("(");
        sb.append(String.join(",", toParameterString()));
        sb.append(")->");
        sb.append(toReturnString());

        return sb.toString();
    }
}
