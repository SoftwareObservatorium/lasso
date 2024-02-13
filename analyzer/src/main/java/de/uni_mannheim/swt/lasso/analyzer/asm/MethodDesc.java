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
package de.uni_mannheim.swt.lasso.analyzer.asm;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.github.javaparser.printer.configuration.PrettyPrinterConfiguration;
import de.uni_mannheim.swt.lasso.analyzer.model.Method;
import de.uni_mannheim.swt.lasso.analyzer.model.Parameter;
import org.apache.commons.lang3.StringUtils;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.type.Type;

/**
 * A description of a (Java) method.
 *
 * @author Marcus Kessel
 */
public class MethodDesc {

    static final PrettyPrinterConfiguration prettyPrinterNoCommentsConfiguration = new PrettyPrinterConfiguration()
            .setPrintComments(false);

    private final MethodDeclaration declaration;
    private String content;

    public MethodDesc(MethodDeclaration declaration) {
        this.declaration = declaration;
        content = declaration.toString();
    }

    /**
     * @return the declaration
     */
    public MethodDeclaration getDeclaration() {
        return declaration;
    }

    public String getMethodBody() {
        return declaration.toString();
    }

    /**
     * @return the content
     */
    public String getContent() {
        return content;
    }

    public int getLoc() {
        return JavaParserAnalyzer.loc(content);
    }

    public boolean isSimilar(Method method) {
        // count params
        int input1 = method.getParameters() != null ? method.getParameters().size() : 0;
        int input2 = declaration.getParameters() != null ? declaration.getParameters().size() : 0;

        boolean similar = StringUtils.equals(method.getName(), declaration.getNameAsString()) && (input1 == input2);

        if (!similar) {
            return false;
        }

        // check return type
        if (!similarType(method.getReturnParameter(), declaration.getType())) {
            return false;
        }

        // check input types
        if (input2 > 0) {
            long count = IntStream.range(0, input2).filter(p -> similarType(method.getParameters().get(p),
                    declaration.getParameter(p).getType())).count();
            
            method.getParameters().stream().map(p -> p.getType()).collect(Collectors.joining(","));
            

            if (count != input2) {
                return false;
            }
        }

        return true;
    }

    public static boolean similarType(Parameter parameter, Type type) {
        // remove generics
        String a = parameter.getType() + StringUtils.repeat("[]", parameter.getArrayDim());
        if (StringUtils.contains(a, "<")) {
            a = StringUtils.substringBefore(a, "<");
        }

        String b = type.toString(prettyPrinterNoCommentsConfiguration);
        if (StringUtils.contains(b, "<")) {
            b = StringUtils.substringBefore(b, "<");
        }

        return StringUtils.contains(a, b);
    }
}
