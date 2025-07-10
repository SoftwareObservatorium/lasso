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

import de.uni_mannheim.swt.lasso.core.datasource.DataSource;
import de.uni_mannheim.swt.lasso.core.model.System;
import de.uni_mannheim.swt.lasso.core.model.*;
import de.uni_mannheim.swt.lasso.core.model.query.QueryResult;
import de.uni_mannheim.swt.lasso.corpus.Datasource;
import de.uni_mannheim.swt.lasso.datasource.maven.MavenDataSource;
import de.uni_mannheim.swt.lasso.datasource.maven.lsl.MavenQuery;
import de.uni_mannheim.swt.lasso.engine.LSLExecutionContext;
import de.uni_mannheim.swt.lasso.engine.LassoUtils;
import de.uni_mannheim.swt.lasso.engine.action.annotations.LassoAction;
import de.uni_mannheim.swt.lasso.engine.action.annotations.LassoInput;
import de.uni_mannheim.swt.lasso.engine.action.annotations.Local;
import de.uni_mannheim.swt.lasso.engine.action.annotations.Stable;
import de.uni_mannheim.swt.lasso.engine.action.maven.support.Mavenizer;
import de.uni_mannheim.swt.lasso.engine.build.JavaProjectBuildManager;
import de.uni_mannheim.swt.lasso.engine.build.ProjectBuildConfiguration;
import de.uni_mannheim.swt.lasso.engine.build.ProjectBuildManager;
import de.uni_mannheim.swt.lasso.engine.build.PythonProjectBuildManager;
import de.uni_mannheim.swt.lasso.engine.langsupport.LangSupport;
import de.uni_mannheim.swt.lasso.gai.openai.Prompt;
import de.uni_mannheim.swt.lasso.gai.openai.util.ContentParser;
import de.uni_mannheim.swt.lasso.lsl.LassoContext;
import de.uni_mannheim.swt.lasso.lsl.SimpleLogger;
import dev.langchain4j.model.ollama.OllamaChatModel;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;

/**
 * Code generation Action based on Ollama.
 *
 * Based on langchain4j.
 *
 * @author Marcus Kessel
 */
@LassoAction(desc = "Generate Code with Ollama")
@Stable
@Local(numberOfParallelStimulusMatrices = 1) // limit to one stimulus matrix at a time
public class GenerateCodeOllama extends LangChainAction {

    private static final Logger LOG = LoggerFactory
            .getLogger(GenerateCodeOllama.class);

    private static final String POM_TEMPLATE =
            Mavenizer.getPomTemplate("/mavenizer/pom_gai.template");

    @LassoInput(desc = "Override Data Source", optional = true)
    public String dataSource = null;

    @LassoInput(desc = "How to match code snippet", optional = true)
    public String regex;

    @LassoInput(desc = "List of Ollama base urls", optional = true)
    public List<String> servers = Arrays.asList("http://bagdana.informatik.uni-mannheim.de:11434");

    @LassoInput(desc = "deploy package to LASSO Nexus", optional = true)
    public boolean deploy = true;

    @LassoInput(desc = "Maven Repository for Deployment", optional = true)
    public String repoUrl;
    @LassoInput(desc = "Maven Repository Id for Deployment", optional = true)
    public String repoId;

    @LassoInput(desc = "Set Java version for compilation", optional = true)
    public String javaVersion = "17";

    @LassoInput(desc = "code model (LLM)", optional = true)
    public String model;

    @LassoInput(desc = "Programming Language (java, python)", optional = true)
    public String lang = CodeUnit.JAVA;

    @LassoInput(desc = "how many coding solutions to obtain", optional = true)
    public int samples = 1;

    @LassoInput(desc = "How many prompt requests to do in parallel", optional = true)
    public int promptRequestThreads = 1;

    @Override
    public void execute(LSLExecutionContext context, ActionConfiguration actionConfiguration) throws IOException {
        if (LOG.isInfoEnabled()) {
            LOG.info("Executing " + this.getClass());
        }

        Abstraction abstraction = actionConfiguration.getAbstraction();

        LOG.info("Abstraction = {}", abstraction.getName());
        LOG.info("Systems = {}", abstraction.getImplementations().size());

        // custom DSL command
        List<Prompt> prompts = readPrompts(context, actionConfiguration, model);

        // --
        String dataSourceId = LassoUtils.resolveDataSource(context, dataSource);
        Datasource ds = LassoUtils.getDataSource(context, dataSourceId).orElseThrow(() -> new IllegalArgumentException("Cannot find data source " + dataSourceId));

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
                        List<String> codeMatches = contentParser.extractCode(response, lang); // PL specific
                        generatedCode.addAll(codeMatches);

                        // useful package names (human readable)
                        String namespace = myPrompt.getModel().replaceAll("\\W", ""); //StringUtils.replaceEach(prompt.getModel(), new String[]{":", "-"}, new String[]{"_", "_"});

                        ProjectBuildConfiguration buildConfiguration = new ProjectBuildConfiguration();
                        buildConfiguration.setDataSource(ds);
                        buildConfiguration.setDeploy(deploy);

                        Map<String, String> meta = new HashMap<>();
                        meta.put("executionId", context.getExecutionId());
                        meta.put("action", getName());
                        meta.put("model", myPrompt.getModel());
                        meta.put("abstractionId", actionConfiguration.getAbstraction().getName());
                        meta.put("sampleId", String.valueOf(myPrompt.getSampleId()));
                        meta.put("promptId", myPrompt.getId());

                        buildConfiguration.setMeta(meta);

                        buildConfiguration.setId(UUID.randomUUID().toString());
                        buildConfiguration.setRepoUrl(repoUrl);
                        buildConfiguration.setRepoId(repoId);
                        buildConfiguration.setGroupId(namespace);
                        buildConfiguration.setArtifactId(buildConfiguration.getId() + "-gai" + "_" + myPrompt.getId());
                        buildConfiguration.setVersion(String.valueOf(myPrompt.getSampleId()));

                        // decide language
                        final ProjectBuildManager projectBuildManager;
                        if(LangSupport.isJava(lang)) {
                            //
                            buildConfiguration.setLangVersion(javaVersion);
                            buildConfiguration.setProjectTemplate(POM_TEMPLATE);

                            projectBuildManager = new JavaProjectBuildManager();
                        } else if(LangSupport.isPython(lang)) {
                            projectBuildManager = new PythonProjectBuildManager();
                        } else {
                            projectBuildManager = null;
                        }

                        Validate.notNull(projectBuildManager, "Unsupported language found");

                        // 2. parse code
                        LOG.info("Parsing code");
                        List<CodeUnit> units = generatedCode.stream().map(c -> projectBuildManager.parse(c, namespace)).filter(Objects::nonNull).collect(Collectors.toList());

                        // store in executable corpus
                        projectBuildManager.store(this, context, actionConfiguration, abstraction, units, buildConfiguration);
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

        // 6. select from data source
        LOG.info("Selecting code");
        List<System> systems = select(context, actionConfiguration, ds);
        abstraction.getSystems().addAll(systems);

        // --

        setExecutables(Systems.fromAbstraction(abstraction, getName()));
    }

    @Override
    public List<Abstraction> createAbstractions(LSLExecutionContext context, ActionConfiguration actionConfiguration) throws IOException {
        return null;
    }

    List<System> select(LSLExecutionContext context, ActionConfiguration actionConfiguration, Datasource datasource) throws IOException {
        DataSource dataSource = context.getDataSourceMap().get(datasource.getId());
        try {
            MavenDataSource ds = (MavenDataSource) dataSource;
            MavenQuery mavenQuery = (MavenQuery) ds.createQueryModelForLSL();
            LassoContext ctx = new LassoContext();
            ctx.setLogger(new SimpleLogger());
            mavenQuery.setLasso(ctx);

            mavenQuery.queryForClasses("*:*");

            mavenQuery.filter("executionId:\"" + context.getExecutionId() + "\"");
            mavenQuery.filter("action:\"" + getName() + "\"");
            mavenQuery.filter("abstractionId:\"" + actionConfiguration.getAbstraction().getName() + "\"");

            if(LangSupport.isPython(lang)) { // special handling for python
                mavenQuery.lang("python"); // set Python language
                mavenQuery.unitType("module"); // Python Module
            }

            mavenQuery.setDirectly(true);
            QueryResult queryResult = ds.query(mavenQuery);

            return queryResult.getImplementations().stream().map(System::new).collect(Collectors.toList());
        } catch (Throwable e) {
            LOG.warn("Could not get classes", e);

            throw e;
        }
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
