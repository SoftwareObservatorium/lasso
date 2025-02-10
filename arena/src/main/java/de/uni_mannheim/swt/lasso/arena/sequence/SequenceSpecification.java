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
package de.uni_mannheim.swt.lasso.arena.sequence;

import de.uni_mannheim.swt.lasso.arena.MethodSignature;
import de.uni_mannheim.swt.lasso.arena.search.InterfaceSpecification;
import de.uni_mannheim.swt.lasso.arena.sequence.parser.unit.ReflectionConstructorSignature;
import de.uni_mannheim.swt.lasso.arena.sequence.parser.unit.ReflectionMethodSignature;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.Validate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * A sequence specification.
 *
 * @author Marcus Kessel
 */
public class SequenceSpecification {

    private static final Logger LOG = LoggerFactory
            .getLogger(SequenceSpecification.class);

    /**
     * Name of sequence
     */
    private String name;

    public List<SpecificationStatement> getStatements() {
        return statements;
    }

    public void setStatements(List<SpecificationStatement> statements) {
        this.statements = statements;
    }

    public boolean isIgnoreVisibility() {
        return ignoreVisibility;
    }

    public void setIgnoreVisibility(boolean ignoreVisibility) {
        this.ignoreVisibility = ignoreVisibility;
    }

    /**
     * Statements in sequence
     */
    private List<SpecificationStatement> statements = new LinkedList<>();

    /**
     * Custom {@link InterfaceSpecification}.
     */
    private InterfaceSpecification interfaceSpecification;

    /**
     * Ignore invisible members
     */
    private boolean ignoreVisibility = false;

    /**
     * Extract interface signatures from this Sequence. If {@link SequenceSpecification#interfaceSpecification} is set, it is used instead.
     *
     * @return
     */
    public InterfaceSpecification toInterfaceSpecification() {
        if(getInterfaceSpecification() != null) {
            return getInterfaceSpecification();
        }

        InterfaceSpecification specification = new InterfaceSpecification();
        List<MethodSignature> cSpecs = new LinkedList<>();
        specification.setConstructors(cSpecs);
        List<MethodSignature> mSpecs = new LinkedList<>();
        specification.setMethods(mSpecs);

        Class<?> cutClazz = null;
        for(SpecificationStatement statement : statements) {
            if(statement instanceof CallStatement) {
                if(statement.isClassUnderTest()) {
                    if(statement instanceof MethodCallStatement) {
                        MethodCallStatement m = (MethodCallStatement) statement;

                        if(cutClazz == null) {
                            cutClazz = m.getResolvedMethod().getDeclaringClass();
                        }

                        ReflectionMethodSignature sig = new ReflectionMethodSignature(m.getResolvedMethod());
                        if(!mSpecs.contains(sig)) {
                            mSpecs.add(sig);
                        }
                    }

                    if(statement instanceof ConstructorCallStatement) {
                        ConstructorCallStatement c = (ConstructorCallStatement) statement;

                        if(cutClazz == null) {
                            cutClazz = c.getResolvedConstructor().getDeclaringClass();
                        }

                        ReflectionConstructorSignature sig = new ReflectionConstructorSignature(c.getResolvedConstructor());
                        if(!cSpecs.contains(sig)) {
                            cSpecs.add(sig);
                        }
                    }
                }
            }
        }

        if(cutClazz == null) {
            //throw new IllegalArgumentException("Could not find CUT class");

            // sequence specification does not contain CUT
            return specification;
        }

        specification.setClassName(cutClazz.getSimpleName());

        // add default constructor if none is used (required for adaptation)
        if(CollectionUtils.isEmpty(cSpecs)) {
            if(LOG.isWarnEnabled()) {
                LOG.warn("Adding default constructor");
            }

            MethodSignature defaultConstructor = new MethodSignature(specification) {
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
            };
            defaultConstructor.setName(cutClazz.getSimpleName());
            defaultConstructor.setReturnType(cutClazz);
            defaultConstructor.setParameterTypes(new Class[0]);
            cSpecs.add(defaultConstructor);
        }

        return specification;
    }

    public void addStatement(SpecificationStatement statement, int position) {
        //validate(position);

//        if(position < 0) {
//            throw new AssertionError("position was negative for " + statement.getClass());
//        }

        statement.setPosition(position);

        statements.add(statement);
    }

    private void validate(int position) {
        Validate.isTrue(statements.stream()
                .mapToInt(SpecificationStatement::getPosition)
                .noneMatch(i -> position == i), "position is taken = " + position);
    }

    public SpecificationStatement getStatement(int position) {
        return statements.stream().filter(s -> s.getPosition() == position).findFirst().orElse(null);

        //return statements.get(position);
    }

    public int getLength() {
        return this.statements.size();
    }

    public int getNextPosition() {
        return getLength();
    }

    public SpecificationStatement getLastStatement() {
        if (getLength() < 1) {
            return null;
        }

        return statements.get(statements.size() - 1);
    }

    public InterfaceSpecification getInterfaceSpecification() {
        return interfaceSpecification;
    }

    public void setInterfaceSpecification(InterfaceSpecification interfaceSpecification) {
        this.interfaceSpecification = interfaceSpecification;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
