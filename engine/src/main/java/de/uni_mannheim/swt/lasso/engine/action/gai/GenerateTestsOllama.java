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
package de.uni_mannheim.swt.lasso.engine.action.gai;

import de.uni_mannheim.swt.lasso.core.dto.srm.JUnitCodeUnit;
import de.uni_mannheim.swt.lasso.core.dto.srm.Sheet;
import de.uni_mannheim.swt.lasso.core.model.*;
import de.uni_mannheim.swt.lasso.engine.LSLExecutionContext;
import de.uni_mannheim.swt.lasso.engine.action.annotations.LassoAction;
import de.uni_mannheim.swt.lasso.engine.action.annotations.LassoInput;
import de.uni_mannheim.swt.lasso.engine.action.annotations.Local;
import de.uni_mannheim.swt.lasso.engine.action.annotations.Stable;
import de.uni_mannheim.swt.lasso.gai.openai.Prompt;
import de.uni_mannheim.swt.lasso.gai.openai.util.ContentParser;
import dev.langchain4j.model.ollama.OllamaChatModel;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;

/**
 * Generates tests with generative AI (GAI)
 *
 * Based on langchain4j.
 *
 * @author Marcus Kessel
 */
@LassoAction(desc = "Generate tests with generative AI based on Ollama")
@Stable
@Local(numberOfParallelStimulusMatrices = 1) // limit to one stimulus matrix at a time
public class GenerateTestsOllama extends LangChainAction {

    private static final Logger LOG = LoggerFactory
            .getLogger(GenerateTestsOllama.class);

//    @LassoInput(desc = "How to match tests", optional = true)
//    public String promptTemplate = "Here is a Java method signature that we want to test:\n" +
//            "\n" +
//            "${code}" +
//            "\n" +
//            "These are some example inputs used to test the method in the following format: ${testSamples}\n" +
//            "\n" +
//            "${promptMessage}";
//
//    @LassoInput(desc = "Prompt messages (selected at random)", optional = true)
//    public List<String> promptMessages = Arrays.asList(
//            "Please generate complex inputs in the same format to test the method.",
//            "Please generate corner case inputs to test the method.",
//            "Please generate difficult inputs to test the method."
//    );

    @LassoInput(desc = "How many prompt requests in parallel", optional = false)
    public int promptRequestThreads = 1;

//    @LassoInput(desc = "How to match tests", optional = false)
//    public String regex = "\\([\\s\\S]*?\\)";

    @LassoInput(desc = "List of Ollama base urls", optional = true)
    public List<String> servers = Arrays.asList("http://bagdana.informatik.uni-mannheim.de:11434");

    @LassoInput(desc = "Ollama model", optional = true)
    public String model = "llama3.1:latest";

//    @LassoInput(desc = "maximum number of tests to generate", optional = true)
//    public int maxNoOfTests = 1;

    @LassoInput(desc = "how many coding solutions to obtain", optional = true)
    public int samples = 1;

    @LassoInput(desc = "Programming Language (java, python)", optional = true)
    public String lang = "java";

//    @LassoInput(desc = "number of prompts to fire", optional = true)
//    public int noOfPrompts = 1;

    //@LassoInput(desc = "dependencies to resolve for types", optional = true)
    //public List<String> dependencies = Arrays.asList("org.javatuples:javatuples:1.2");

    @Override
    public void execute(LSLExecutionContext context, ActionConfiguration actionConfiguration) throws IOException {
        if (LOG.isInfoEnabled()) {
            LOG.info("Executing " + this.getClass());
        }

        Abstraction abstraction = actionConfiguration.getAbstraction();

        LOG.info("Abstraction = {}", abstraction.getName());
        LOG.info("Systems = {}", abstraction.getImplementations().size());
        for (de.uni_mannheim.swt.lasso.core.model.System implementation : abstraction.getImplementations()) {
            LOG.info(">> System = {}, {}", implementation.getId(), implementation.getCode().toFQName());
        }

        // custom DSL command
        List<Prompt> prompts = readPrompts(context, actionConfiguration, model);

        // parallelization: create jobs
        List<PromptJob> jobs = new LinkedList<>();
        for (Prompt myPrompt : prompts) {
            for (int sampleId = 0; sampleId < samples; sampleId++) {
                jobs.add(new PromptJob(myPrompt, sampleId));
            }
        }

        // cycle cluster of ollamas
        Iterator<String> serverIt = getRoundRobinIterator(this.servers);

        ForkJoinPool customThreadPool = new ForkJoinPool(promptRequestThreads);
        try {
            // submit job
            customThreadPool.submit(() -> {
                jobs.parallelStream().forEach(job -> {
                    Prompt myPrompt = job.getPrompt();
                    int sampleId = job.getSampleId();

                    try {

                        // 1. generate code
                        LOG.info("Generating code");

                        // FIXME unique sample ID
                        myPrompt.setSampleId(sampleId);

                        // should never happen
                        if(!serverIt.hasNext()) {
                            //
                            throw new IllegalStateException("server iterator returned false");
                        }
                        // select ollama server
                        String server = serverIt.next();

                        String response = generate(myPrompt, server);

                        if (LOG.isDebugEnabled()) {
                            LOG.debug("Response\n{}", ToStringBuilder.reflectionToString(response));
                        }

                        ContentParser contentParser = new ContentParser();
                        List<String> generatedCode = new LinkedList<>();
                        List<String> codeMatches = contentParser.extractCode(response, lang);
                        generatedCode.addAll(codeMatches);

                        // useful package names (human readable)
                        String pkg = myPrompt.getModel().replaceAll("\\W", ""); //StringUtils.replaceEach(prompt.getModel(), new String[]{":", "-"}, new String[]{"_", "_"});

                        // 2. parse code
                        LOG.info("Parsing code");
                        List<CodeUnit> units = generatedCode.stream().map(c -> parse(c, pkg)).filter(Objects::nonNull).collect(Collectors.toList());

                        String testPrefix = pkg + sampleId;

                        // store junit test classes
                        List<Sheet> junitClasses = units.stream().map(u -> {
                            JUnitCodeUnit jUnitCodeUnit = new JUnitCodeUnit(u, abstraction.getLql());
                            jUnitCodeUnit.setTestPrefix(testPrefix);
                            return (Sheet) jUnitCodeUnit;
                        }).toList();
                        // set to FA
                        abstraction.getSpecification().getTests().addAll(junitClasses);
                    } catch (Throwable e) {
                        LOG.warn("Generation failed {}", sampleId);
                        LOG.warn("Stack", e);
                    }
                });
            }).get();
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            customThreadPool.shutdown();
        }

        // --

        setExecutables(Systems.fromAbstraction(abstraction, getName()));
    }

    @Override
    protected String generate(Prompt prompt, String endpoint)  {
        OllamaChatModel ollamaChatModel = OllamaChatModel.builder()
                .baseUrl(endpoint)
                .modelName(prompt.getModel())
                .temperature(prompt.getTemperature())
//                .numCtx(2048)
//                .numPredict(128)
                .build();

        LOG.info("Prompting '{}', '{}'", endpoint, prompt.getModel());

        String chatResponse = ollamaChatModel.generate(prompt.getPromptContent());

        return chatResponse;
    }
}
