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
package de.uni_mannheim.swt.lasso.arena;

import de.uni_mannheim.swt.lasso.arena.search.InterfaceSpecification;
import de.uni_mannheim.swt.lasso.core.adapter.MethodDesc;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.Objects;

/**
 * A method signature.
 *
 * @author Marcus Kessel
 */
public class MethodSignature {

    private final InterfaceSpecification parent;

    private String name;
    private Class<?>[] parameterTypes;
    private Class<?> returnType;

    private String className;

    public MethodSignature(InterfaceSpecification parent) {
        this.parent = parent;
    }

    public String[] toParameterString() {
        if(ArrayUtils.isEmpty(parameterTypes)) {
            return new String[]{};
        }

        return Arrays.stream(parameterTypes).map(Class::getCanonicalName).toArray(String[]::new);
    }

    public boolean isStatic() {
        return false;
    }

    public String toReturnString() {
        return returnType.getCanonicalName();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Class<?>[] getParameterTypes() {
        return parameterTypes;
    }

    public Class<?>[] getParameterTypes(Class<?> forClass) {
        return parameterTypes;
    }

    public void setParameterTypes(Class<?>[] parameterTypes) {
        this.parameterTypes = parameterTypes;
    }

    public Class<?> getReturnType() {
        return returnType;
    }

    public Class<?> getReturnType(Class<?> forClass) {
        return returnType;
    }

    public void setReturnType(Class<?> returnType) {
        this.returnType = returnType;
    }

    public boolean isValid() {
        return StringUtils.isBlank(getName()) || Arrays.stream(getParameterTypes()).allMatch(Objects::nonNull) || getReturnType() == null;
    }

    public String toLQL() {
        StringBuilder sb = new StringBuilder();
//        sb.append(className);
//        sb.append("(");

        sb.append(getName());
        sb.append("(");
        sb.append(String.join(",", toParameterString()));
        sb.append(")->");
        try {
            sb.append(toReturnString());
        } catch (Throwable e) {
            System.err.println("invalid LQL: " + sb.toString());

            e.printStackTrace();

            throw e;
        }

        return sb.toString();
    }

    public MethodDesc toDescription() {
        MethodDesc methodDesc = new MethodDesc();
        methodDesc.setMethod(toLQL());

        return methodDesc;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public InterfaceSpecification getParent() {
        return parent;
    }

    public boolean isFA(String name) {
        if(parent != null) {
            return StringUtils.equalsIgnoreCase(name, parent.getClassName());
        }

        return false;
    }
}
