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

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.type.Type;
import de.uni_mannheim.swt.lasso.arena.ClassUnderTest;
import de.uni_mannheim.swt.lasso.arena.search.InterfaceSpecification;
import de.uni_mannheim.swt.lasso.arena.sequence.SequenceSpecification;
import de.uni_mannheim.swt.lasso.arena.sequence.eval.Eval;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Context model for JUnit parser.
 *
 * @author Marcus Kessel
 */
public class JUnitParseContext {

    private final MethodDeclaration testMethod;
    private SequenceSpecification sequenceSpecification;
    private InterfaceSpecification specification;

    private List<String> localFields = new LinkedList<>();
    private Map<String, Type> localFieldTypes = new LinkedHashMap<>();
    private ClassUnderTest classUnderTest;
    private Eval eval;

    public JUnitParseContext(MethodDeclaration testMethod) {
        this.testMethod = testMethod;
    }

    public List<String> getLocalFields() {
        return localFields;
    }

    public void setLocalFields(List<String> localFields) {
        this.localFields = localFields;
    }

    public ClassUnderTest getClassUnderTest() {
        return classUnderTest;
    }

    public void setClassUnderTest(ClassUnderTest classUnderTest) {
        this.classUnderTest = classUnderTest;
        this.eval = new Eval(classUnderTest.getProject().getContainer());
    }

    public Class<?> resolveClass(String clazz) throws ClassNotFoundException {
        // remove generic types
        if(StringUtils.contains(clazz, "<")) {
            clazz = StringUtils.substringBefore(clazz, "<");
        }

        return ClassUtils.getClass(getClassUnderTest().getProject().getContainer(), clazz);
    }

    public Eval getEval() {
        return eval;
    }

    public InterfaceSpecification getSpecification() {
        return specification;
    }

    public void setSpecification(InterfaceSpecification specification) {
        this.specification = specification;
    }

    public SequenceSpecification getSequenceSpecification() {
        return sequenceSpecification;
    }

    public void setSequenceSpecification(SequenceSpecification sequenceSpecification) {
        this.sequenceSpecification = sequenceSpecification;
    }

    public Map<String, Type> getLocalFieldTypes() {
        return localFieldTypes;
    }

    public MethodDeclaration getTestMethod() {
        return testMethod;
    }
}
