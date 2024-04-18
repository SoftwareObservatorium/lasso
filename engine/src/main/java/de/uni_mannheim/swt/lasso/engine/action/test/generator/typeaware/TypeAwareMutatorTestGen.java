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
package de.uni_mannheim.swt.lasso.engine.action.test.generator.typeaware;

import de.uni_mannheim.swt.lasso.benchmark.*;
import de.uni_mannheim.swt.lasso.benchmark.Sequence;
import de.uni_mannheim.swt.lasso.classloader.Container;
import de.uni_mannheim.swt.lasso.core.model.*;

import de.uni_mannheim.swt.lasso.engine.LSLExecutionContext;
import de.uni_mannheim.swt.lasso.engine.LassoUtils;
import de.uni_mannheim.swt.lasso.engine.action.DefaultAction;
import de.uni_mannheim.swt.lasso.engine.action.annotations.LassoAction;
import de.uni_mannheim.swt.lasso.engine.action.annotations.LassoInput;
import de.uni_mannheim.swt.lasso.engine.action.annotations.Local;
import de.uni_mannheim.swt.lasso.engine.action.annotations.Stable;
import de.uni_mannheim.swt.lasso.engine.action.utils.ClazzContainerUtils;
import de.uni_mannheim.swt.lasso.engine.action.utils.DistributedFileSystemUtils;
import de.uni_mannheim.swt.lasso.engine.action.utils.SequenceUtils;
import de.uni_mannheim.swt.lasso.testing.generate.typeaware.TypeAwareMutator;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.reflect.TypeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

/**
 * Test generation based on a type-aware mutator for test inputs. See {@link de.uni_mannheim.swt.lasso.testing.generate.typeaware.TypeAwareMutator}.
 *
 * @author Marcus Kessel
 */
@LassoAction(desc = "Type-aware mutator for test generation")
@Stable
@Local
public class TypeAwareMutatorTestGen extends DefaultAction {

    private static final Logger LOG = LoggerFactory
            .getLogger(TypeAwareMutatorTestGen.class);

    @LassoInput(desc = "how many tests to generate", optional = true)
    public int noOfTests = 1;

    @LassoInput(desc = "dependencies to resolve for types", optional = true)
    public List<String> dependencies = Arrays.asList("org.javatuples:javatuples:1.2");

    // benchmarking
    @LassoInput(desc = "Use Sequences provided by benchmark", optional = true)
    public String benchmark;

    @LassoInput(desc = "Obtain Sequences from the following actions", optional = true)
    public List<String> sequenceActions = Collections.emptyList();

    // manual sequences
    @LassoInput(desc = "User-provided Sequence Sheets", optional = true)
    public Map<String, Object> sequences;

    @Override
    public void execute(LSLExecutionContext context, ActionConfiguration actionConfiguration) throws IOException {
        if(LOG.isInfoEnabled()) {
            LOG.info("Executing "+  this.getClass());
        }

        Abstraction abstraction = actionConfiguration.getAbstraction();

        // set executables
        setExecutables(Systems.fromAbstraction(abstraction, getName()));

        // custom class loader for special types
        Container container = ClazzContainerUtils.createClazzContainer(context, dependencies);

        // original sequences
        List<Sequence> originalSequences = collectSequences(context, getExecutables());

        // generated sequences
        List<Sequence> sequences = generate(context, abstraction.getSpecification(), originalSequences, container);

        if(LOG.isDebugEnabled()) {
            for(Sequence sequence : sequences) {
                LOG.debug("Sequence: {}", sequence.getId());
                LOG.debug("{}", sequence.toSequenceString());
            }
        }

        // write sequences locally for each candidate
        //SequenceUtils.writeSequences2Xlsx(this, getExecutables(), sequences, abstraction.getSpecification());

        // write sequences to DFS
        LOG.info("Writing {} test sequences to DFS", sequences.size());
        try {
            DistributedFileSystemUtils.writeSequences(context, this, abstraction.getName(), sequences);
        } catch (Throwable e) {
            LOG.warn("Could not write test sequences to DFS");
            LOG.warn("Stack trace:", e);
        }

        // cleanup
        ClazzContainerUtils.dispose(container);
    }

    /**
     * Generate test sequences based on {@link Sequence}s.
     *
     * @param context
     * @param specification
     * @param originalSequences
     * @param container
     * @return
     */
    protected List<de.uni_mannheim.swt.lasso.benchmark.Sequence> generate(LSLExecutionContext context, Specification specification, List<Sequence> originalSequences, Container container) {
        Interface iFace = SequenceUtils.resolveInterface(context, getExecutables(), benchmark);

        if(iFace != null) {
            LOG.debug("Found specification in abstraction '{}'", specification.getInterfaceSpecification());

            TypeAwareMutator mutator = new TypeAwareMutator();

//            List<List<Statement>> statementList = new ArrayList<>();
//            for(Sequence originalSequence : originalSequences) {
//                LOG.debug("Original Sequence: {}", originalSequence.getId());
//                LOG.debug("{}", originalSequence.toSequenceString());
//
//                for(Statement originalStatement : originalSequence.getStatements()) {
//                    try {
//                        List<Statement> statements = generate(mutator, originalStatement, 1, container);
//                        statementList.add(statements);
//                    } catch (Throwable e) {
//                        LOG.warn("Mutating sequence failed: {}", originalSequence);
//                    }
//                }
//            }
//
//            // generate multi-statement sequences
//            List<Sequence> sequences = SequenceUtils.generateLinearSequences(this, noOfTests, statementList);

            List<Sequence> sequences = new ArrayList<>();
            for (int i = 0; i < noOfTests; i++) {
                for(Sequence originalSequence : originalSequences) {
                    LOG.debug("Original Sequence: {}", originalSequence.getId());
                    LOG.debug("{}", originalSequence.toSequenceString());
                    for(Statement originalStatement : originalSequence.getStatements()) {
                        try {
                            Sequence sequence = new Sequence();
                            sequence.setId(LassoUtils.compactUUID(UUID.randomUUID().toString()) + "_" + getName());
                            sequence.setStatements(new LinkedList<>());
                            List<Statement> statements = generate(mutator, originalStatement, 1, container);
                            sequence.addStatement(statements.get(0));
                            sequences.add(sequence);
                        } catch (Throwable e) {
                            LOG.warn("Mutating sequence failed: {}", originalSequence);
                            e.printStackTrace();
                        }
                    }
                }
            }

            return sequences;
        }

        throw new IllegalArgumentException("No specification found");
    }

    /**
     * Collect existing sequences
     *
     * @param context
     * @param systems
     * @return
     */
    protected List<Sequence> collectSequences(LSLExecutionContext context, Systems systems) {
        List<Sequence> sequenceList = new ArrayList<>();
        if(MapUtils.isNotEmpty(sequences)) {
            List<Sequence> sheets = SequenceUtils.toSequences(sequences, SequenceUtils.resolveInterface(context, systems, benchmark));
            if(CollectionUtils.isNotEmpty(sheets)) {
                sequenceList.addAll(sheets);
            }
        }

        List<Sequence> collected = SequenceUtils.collectSequences(context, systems, benchmark, sequenceActions);

        if(CollectionUtils.isNotEmpty(collected)) {
            sequenceList.addAll(collected);
        }

        return sequenceList;
    }

    /**
     * Generate statements for {@link MethodSignature}
     *
     * @param mutator
     * @param originalStatement
     * @param number
     * @param container
     * @return
     * @throws ClassNotFoundException
     */
    protected List<Statement> generate(TypeAwareMutator mutator,
                                       Statement originalStatement, int number, Container container) throws ClassNotFoundException {
        List<Statement> statements = new LinkedList<>();

        for(int i = 0; i < number; i++) {
            Statement statement = new Statement();
            statement.setOperation(originalStatement.getOperation());
            statement.setInputs(new ArrayList<>());
            statement.setExpectedOutputs(new LinkedList<>());

            statements.add(statement);

            for(Value input : originalStatement.getInputs()) {
                LOG.debug("Type is {}", input.getValue().getClass());
                LOG.debug("Type is {}", input.getType());
                Object casted = getObject(input, container);

                Object generated = mutator.mutateValue(casted);

                Value value = new Value();
                value.setValue(generated);
                value.setType(input.getType());
                statement.getInputs().add(value);
            }
        }

        return statements;
    }

    public Object getObject(Value input, Container container) throws ClassNotFoundException {
        if(input.getValue() == null) {
            return null;
        }
        Object o = input.getValue();
        Class requiredType = container.loadClass(input.getType());
        boolean number = TypeUtils.isAssignable(requiredType, Number.class);

        Object val = input.getValue();
        Object valSet = input.getValue();
        if(number) {
            if(requiredType.equals(byte.class) || requiredType.equals(Byte.class)) {
                valSet = Byte.valueOf(((Number)val).byteValue());
            } else if(requiredType.equals(char.class) || requiredType.equals(Character.class)) {
                valSet = Character.valueOf((char) ((Number)val).intValue());
            } else if(requiredType.equals(short.class) || requiredType.equals(Short.class)) {
                valSet= Short.valueOf(((Number)val).shortValue());
            } else if(requiredType.equals(int.class) || requiredType.equals(Integer.class)) {
                valSet = Integer.valueOf(((Number)val).intValue());
            } else if(requiredType.equals(long.class) || requiredType.equals(Long.class)) {
                valSet = Long.valueOf(((Number)val).longValue());
            } else if(requiredType.equals(double.class) || requiredType.equals(Double.class)) {
                valSet = Double.valueOf(((Number)val).doubleValue());
            } else if(requiredType.equals(float.class) || requiredType.equals(Float.class)) {
                valSet = Float.valueOf(((Number)val).floatValue());
            }
        }

        return valSet;
    }
}
