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
import de.uni_mannheim.swt.lasso.core.adapter.InterfaceDesc;
import org.apache.commons.collections4.CollectionUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Interface specification of a class under test.
 *
 * @author Marcus Kessel
 */
public class InterfaceSpecification {

    private String className;

    private List<MethodSignature> constructors;
    private List<MethodSignature> methods;

    public boolean isEmpty() {
        return CollectionUtils.isEmpty(constructors) && CollectionUtils.isEmpty(methods);
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public List<MethodSignature> getConstructors() {
        return constructors;
    }

    public void setConstructors(List<MethodSignature> constructors) {
        this.constructors = constructors;
    }

    public String[] getMethodNames() {
        return getMethods().stream().map(MethodSignature::getName).toArray(String[]::new);
    }

    public List<MethodSignature> getMethods() {
        return methods;
    }

    public void setMethods(List<MethodSignature> methods) {
        this.methods = methods;
    }

    /**
     * e.g. Base64(encode(char[]):char[];)
     *
     * @return
     */
    public String toLQL() {
        StringBuilder sb = new StringBuilder();
        sb.append(className);
        sb.append("{\n");
        if(CollectionUtils.isNotEmpty(constructors)) {
//            // make sure to use Clazz name and not <init>
//            constructors.forEach(c -> {
//                c.setName(c.getName());
//                c.setReturnType("void");
//            });

            sb.append(constructors.stream().map(MethodSignature::toLQL).collect(Collectors.joining("\n")));

            sb.append("\n");
        }
        if(CollectionUtils.isNotEmpty(methods)) {
            sb.append(methods.stream().map(MethodSignature::toLQL).collect(Collectors.joining("\n")));

            sb.append("\n");
        }

        sb.append("}");

        return sb.toString();
    }

    public String toJava() {
        StringBuilder sb = new StringBuilder();
        sb.append("class ");
        sb.append(className);
        sb.append("{\n");
        if(CollectionUtils.isNotEmpty(constructors)) {
//            // make sure to use Clazz name and not <init>
//            constructors.forEach(c -> {
//                c.setName(c.getName());
//                c.setReturnType("void");
//            });

            sb.append(constructors.stream().map(MethodSignature::toJava).collect(Collectors.joining("\n")));

            sb.append("\n");
        }
        if(CollectionUtils.isNotEmpty(methods)) {
            sb.append(methods.stream().map(MethodSignature::toJava).collect(Collectors.joining("\n")));

            sb.append("\n");
        }

        sb.append("}");

        return sb.toString();
    }

    public InterfaceDesc toDescription() {
        InterfaceDesc interfaceDesc = new InterfaceDesc();
        interfaceDesc.setName(getClassName());

        if(CollectionUtils.isNotEmpty(constructors)) {
            interfaceDesc.setInitializers(constructors.stream().map(MethodSignature::toDescription).collect(Collectors.toList()));
        }

        if(CollectionUtils.isNotEmpty(methods)) {
            interfaceDesc.setMethods(methods.stream().map(MethodSignature::toDescription).collect(Collectors.toList()));
        }

        return interfaceDesc;
    }

    @Override
    public String toString() {
        return "Specification{" +
                "LQL='" + toLQL() +
                '}';
    }
}
