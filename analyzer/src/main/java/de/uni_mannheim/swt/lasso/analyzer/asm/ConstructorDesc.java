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

import java.util.stream.IntStream;

import de.uni_mannheim.swt.lasso.analyzer.model.Method;

import com.github.javaparser.ast.body.ConstructorDeclaration;

/**
 * Description of a (Java) constructor.
 *
 * @author Marcus Kessel
 *
 */
public class ConstructorDesc {

    private final ConstructorDeclaration declaration;
    private String content;

    public ConstructorDesc(ConstructorDeclaration declaration) {
        this.declaration = declaration;
        content = declaration.toString();
    }

    /**
     * @return the declaration
     */
    public ConstructorDeclaration getDeclaration() {
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

        boolean similar = input1 == input2;

        if (!similar) {
            return false;
        }

        // check input types
        if (input2 > 0) {
            long count = IntStream.range(0, input2).filter(p -> MethodDesc.similarType(method.getParameters().get(p),
                    declaration.getParameter(p).getType())).count();

            if (count != input2) {
                return false;
            }
        }

        return true;
    }
}
