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
package de.uni_mannheim.swt.lasso.engine.action.test.generator.gai;

import de.uni_mannheim.swt.lasso.benchmark.*;
import de.uni_mannheim.swt.lasso.benchmark.Sequence;
import de.uni_mannheim.swt.lasso.classloader.Container;
import de.uni_mannheim.swt.lasso.core.model.*;
import de.uni_mannheim.swt.lasso.engine.LSLExecutionContext;
import de.uni_mannheim.swt.lasso.engine.action.DefaultAction;
import de.uni_mannheim.swt.lasso.engine.action.annotations.LassoAction;
import de.uni_mannheim.swt.lasso.engine.action.annotations.LassoInput;
import de.uni_mannheim.swt.lasso.engine.action.annotations.Local;
import de.uni_mannheim.swt.lasso.engine.action.annotations.Stable;
import de.uni_mannheim.swt.lasso.engine.action.test.generator.gai.eval.Eval;
import de.uni_mannheim.swt.lasso.engine.action.test.generator.gai.parser.TestParser;
import de.uni_mannheim.swt.lasso.engine.action.utils.ClazzContainerUtils;

import de.uni_mannheim.swt.lasso.engine.action.utils.DistributedFileSystemUtils;
import de.uni_mannheim.swt.lasso.engine.action.utils.SequenceUtils;
import de.uni_mannheim.swt.lasso.gai.openai.*;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.StrSubstitutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;

import java.util.stream.IntStream;

/**
 * Generates tests with generative AI (GAI) based on OpenAI Restful API endpoints.
 *
 * @author Marcus Kessel
 */
@LassoAction(desc = "Generate tests with generative AI")
@Stable
@Local
public class GAITestGen extends DefaultAction {

    private static final Logger LOG = LoggerFactory
            .getLogger(GAITestGen.class);

    @LassoInput(desc = "How to match tests", optional = true)
    public String promptTemplate = "Here is a Java method signature that we want to test:\n" +
            "\n" +
            "${code}" +
            "\n" +
            "These are some example inputs used to test the method in the following format: ${testSamples}\n" +
            "\n" +
            "${promptMessage}";

    @LassoInput(desc = "Prompt messages (selected at random)", optional = true)
    public List<String> promptMessages = Arrays.asList(
            "Please generate complex inputs in the same format to test the method.",
            "Please generate corner case inputs to test the method.",
            "Please generate difficult inputs to test the method."
    );

    @LassoInput(desc = "How many prompt requests in parallel", optional = false)
    public int promptRequestThreads = 1;

    @LassoInput(desc = "How to match tests", optional = false)
    public String regex = "\\([\\s\\S]*?\\)";

    @LassoInput(desc = "API Key", optional = true)
    public String apiKey = "";

    @LassoInput(desc = "API Url (non-OpenAI servers)", optional = true)
    public String apiUrl = "https://api.openai.com/v1/chat/completions";

    @LassoInput(desc = "maximum number of tests to generate", optional = true)
    public int maxNoOfTests = 10;

    @LassoInput(desc = "number of prompts to fire", optional = true)
    public int noOfPrompts = 1;

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
     * Generate test sequences based on {@link Specification}
     *
     * @param context
     * @param specification
     * @param container
     * @return
     */
    protected List<de.uni_mannheim.swt.lasso.benchmark.Sequence> generate(LSLExecutionContext context, Specification specification, List<Sequence> originalSequences, Container container) {
        Interface iFace = SequenceUtils.resolveInterface(context, getExecutables(), benchmark);

        if(iFace != null) {
            LOG.debug("Found specification in abstraction '{}'", specification.getInterfaceSpecification());

            List<List<Statement>> statementList = new ArrayList<>();

            // FIXME do it for all original ones or sample a random one?
            boolean sample = true;
            if(sample) {
                Sequence sequenceSample = originalSequences.get(new Random().nextInt(originalSequences.size()));
                for(Statement originalStatement : sequenceSample.getStatements()) {
                    try {
                        // FIXME weak .. could be more .. also check types
                        Optional<MethodSignature> methodSignatureOp = SequenceUtils.findFirstMethodSignature(
                                iFace, originalStatement.getOperation());

                        List<Statement> statements = generate(methodSignatureOp.get(), originalStatement, container);
                        statementList.add(statements);
                    } catch (Throwable e) {
                        LOG.warn("Mutating sequence failed: {}", sequenceSample);
                        e.printStackTrace();
                    }
                }
            }

            // all ...
//            for(Sequence originalSequence : originalSequences) {
//                for(Statement originalStatement : originalSequence.getStatements()) {
//                    try {
//                        // FIXME weak .. could be more .. also check types
//                        Optional<MethodSignature> methodSignatureOp = iFace.getMethods().stream()
//                                .filter(m -> StringUtils.equals(m.getName(), originalStatement.getOperation()))
//                                .findFirst();
//
//                        List<Statement> statements = generate(methodSignatureOp.get(), originalStatement, container);
//                        statementList.add(statements);
//                    } catch (Throwable e) {
//                        LOG.warn("Mutating sequence failed: {}", originalSequence);
//                    }
//                }
//            }

            // generate multi-statement sequences
            List<Sequence> sequences = SequenceUtils.generateLinearSequences(this, maxNoOfTests, statementList);

            return sequences;
        }

        throw new IllegalArgumentException("No specification found for " + getExecutables().getAbstractionName());
    }

    /**
     * Generate statements for {@link MethodSignature}
     *
     * @param originalStatement
     * @param container
     * @return
     * @throws ClassNotFoundException
     */
    protected List<Statement> generate(MethodSignature methodSignature, Statement originalStatement, Container container) throws ClassNotFoundException, ExecutionException, InterruptedException {
        List<Statement> statements = new LinkedList<>();

        List<List<?>> tests = retrieve(methodSignature, originalStatement, container);
        for(List<?> inputs : tests) {
            Statement statement = new Statement();
            statement.setOperation(originalStatement.getOperation());
            statement.setInputs(new ArrayList<>());
            statement.setExpectedOutputs(new LinkedList<>());

            statements.add(statement);

            for(int i = 0; i < inputs.size(); i++) {
                Object generated  = inputs.get(i);
                String type = methodSignature.getInputs().get(i);
                Value value = new Value();
                value.setValue(generated);
                value.setType(type);
                statement.getInputs().add(value);
            }
        }

        return statements;
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
     * Obtain tests from GAI
     *
     * @param methodSignature
     * @param statement
     * @param container
     * @return
     * @throws ExecutionException
     * @throws InterruptedException
     */
    private List<List<?>> retrieve(MethodSignature methodSignature, Statement statement, Container container) throws ExecutionException, InterruptedException {
        OpenAiClient client = new OpenAiClient(this.apiUrl,
                this.apiKey);

        TestParser testParser = new TestParser();
        testParser.setRegex(regex);

        Eval eval = new Eval(container);

        List<List<?>> tests = Collections.synchronizedList(new ArrayList<>());

        ForkJoinPool customThreadPool = new ForkJoinPool(promptRequestThreads);
        customThreadPool.submit(
                () -> IntStream.range(0, noOfPrompts).parallel().forEach(sample -> {
                    try {
                        Map<String, Object> valueMap = new HashMap<>();
                        valueMap.put("code", methodSignature.toJava());
                        // FIXME better way to do it (JSON etc.)
                        String testSamples = SequenceUtils.translateInputs(statement);
                        valueMap.put("testSamples", testSamples);

                        // at random
                        String promptMessage = promptMessages.get(new Random().nextInt(promptMessages.size()));
                        valueMap.put("promptMessage", promptMessage);
                        String prompt = StrSubstitutor.replace(promptTemplate, valueMap);
                        if(LOG.isDebugEnabled()) {
                            LOG.debug("Generated prompt\n{}", prompt);
                        }

                        OllamaCompletionRequest request = new OllamaCompletionRequest();
                        Message message = new Message();
                        message.setRole("user");
                        message.setContent(prompt);
                        //request.setN(1);
                        request.setTemperature(0.7);
                        request.setTop_p(0.95);
                        request.setTop_k(40);
                        request.setRepeat_penalty(1.18);
                        request.setMax_tokens(256);
                        request.setStream(false); // return complete response
                        request.setEcho(true); // include prompt

                        request.setMessages(Collections.singletonList(message));

                        // not really necessary
                        //request.setModel("deepseek-coder-33b-instruct.Q5_K_M.gguf");

                        CompletionResponse response = client.complete(request);
                        for(Choice choice : response.getChoices()) {
                            if(LOG.isDebugEnabled()) {
                                LOG.debug("Next choice\n{}", choice.getMessage().getContent());
                            }

                            List<String> rawTests = testParser.extractTests(choice.getMessage().getContent());

                            for(String testInputs : rawTests) {
                                testInputs = "[" + StringUtils.substring(testInputs, 1, testInputs.length() - 1) + "]";
                                try {
                                    List obj = (List) eval.expr(testInputs);
                                    tests.add(obj);
                                } catch (Throwable e) {
                                    LOG.warn("Failed to evaluate test inputs {}", testInputs);
                                    LOG.warn("Exception", e);
                                }
                            }
                        }
                    } catch (Throwable e) {
                        LOG.warn("Test call failed", e);
                    }
                })).get();

        customThreadPool.shutdown();

        return tests;
    }
}
