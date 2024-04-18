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
package de.uni_mannheim.swt.lasso.engine.action.test.generator.random;

import de.uni_mannheim.swt.lasso.benchmark.Sequence;
import de.uni_mannheim.swt.lasso.benchmark.Statement;
import de.uni_mannheim.swt.lasso.benchmark.Value;
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
import de.uni_mannheim.swt.lasso.engine.action.utils.JavaLangUtils;
import de.uni_mannheim.swt.lasso.testing.generate.random.RandomObjectGenerator;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Random test input generation. See {@link de.uni_mannheim.swt.lasso.testing.generate.random.RandomObjectGenerator}.
 *
 * @author Marcus Kessel
 */
@LassoAction(desc = "Random test value generator for Java types")
@Stable
@Local
public class RandomTestGen extends DefaultAction {

    private static final Logger LOG = LoggerFactory
            .getLogger(RandomTestGen.class);

    //@LassoInput(desc = "seed value for random generator (makes generator deterministic)", optional = true)
    //public int seed = -1;

    @LassoInput(desc = "how many tests to generate", optional = true)
    public int noOfTests = 1;

    @LassoInput(desc = "shuffle sequence statements", optional = true)
    public boolean shuffleSequence = true;

    @LassoInput(desc = "dependencies to resolve for types", optional = true)
    public List<String> dependencies = Arrays.asList("org.javatuples:javatuples:1.2");

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

        // generated sequences
        List<Sequence> sequences = generate(abstraction.getSpecification(), container);

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
     * Generate test sequences based on {@link Specification}
     *
     * @param specification
     * @param container
     * @return
     */
    protected List<Sequence> generate(Specification specification, Container container) {
        RandomObjectGenerator randomObjectGenerator = new RandomObjectGenerator();

        if(specification.getInterfaceSpecification() != null) {
            LOG.debug("Found specification in abstraction '{}'", specification.getInterfaceSpecification());

            Interface iFace = specification.getInterfaceSpecification();

            List<List<Statement>> statementList = new ArrayList<>();
            for(MethodSignature methodSignature : iFace.getMethods()) {
                try {
                    List<Statement> statements = generate(randomObjectGenerator, methodSignature, container);
                    statementList.add(statements);
                } catch (Throwable e) {
                    LOG.warn("Generating sequence for method signature failed: {}", methodSignature);
                }
            }

            // generate multi-statement sequences
            List<Sequence> sequences = generateSequences(statementList);

            return sequences;
        }

        throw new IllegalArgumentException("No specification found");
    }

    /**
     * Generate random sequences based on list of lists. Each nested list contains alternative statements.
     *
     * @param statementList
     * @return
     */
    protected List<Sequence> generateSequences(List<List<Statement>> statementList) {
        // some randomness
        Random random = new Random();

        List<Sequence> randomList = new ArrayList<>();

        for(int i = 0; i < noOfTests; i++) {
            Sequence sequence = new Sequence();
            sequence.setId(LassoUtils.compactUUID(UUID.randomUUID().toString()) + "_" + getName());

            for (List<Statement> statements : statementList) {
                if (statements.isEmpty()) {
                    throw new IllegalArgumentException("Nested list cannot be empty");
                }

                sequence.addStatement(statements.get(random.nextInt(statements.size())));
            }

            // we may also shuffle statements
            if(shuffleSequence) {
                Collections.shuffle(sequence.getStatements());
            }

            randomList.add(sequence);
        }

        return randomList;
    }

    /**
     * Generate statements for {@link MethodSignature}
     *
     * @param randomObjectGenerator
     * @param methodSignature
     * @param container
     * @return
     * @throws ClassNotFoundException
     */
    protected List<Statement> generate(RandomObjectGenerator randomObjectGenerator,
                                                                MethodSignature methodSignature, Container container) throws ClassNotFoundException {
        List<Statement> statements = new LinkedList<>();

        for(int i = 0; i < noOfTests; i++) {
            Statement statement = new Statement();
            statement.setOperation(methodSignature.getName());
            statement.setInputs(new ArrayList<>());
            statement.setExpectedOutputs(new LinkedList<>());

            statements.add(statement);

            for(String input : methodSignature.getInputs()) {
                // use container to also allow for foreign types
                String className = input;
                List<Class<?>> genericTypes = new LinkedList<>();
                if(StringUtils.contains(input, "<")) {
                    className = StringUtils.substringBefore(input, "<");
                    // read types
                    genericTypes = JavaLangUtils.parseGenericType(input).stream().map(t -> {
                        try {
                            return ClassUtils.getClass(container, t);
                        } catch (ClassNotFoundException e) {
                            throw new RuntimeException(e);
                        }
                    }).collect(Collectors.toList());
                }
                Class<?> type = ClassUtils.getClass(container, className);
                Object generated;
                if(CollectionUtils.isNotEmpty(genericTypes)) {
                    generated = randomObjectGenerator.random(type, genericTypes.toArray(new Class<?>[0]));
                } else {
                    generated = randomObjectGenerator.random(type);
                }

                Value value = new Value();
                value.setValue(generated);
                value.setType(input);
                statement.getInputs().add(value);
            }
        }

        return statements;
    }
}
