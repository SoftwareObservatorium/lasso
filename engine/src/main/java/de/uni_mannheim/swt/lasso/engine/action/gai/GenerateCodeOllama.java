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
import de.uni_mannheim.swt.lasso.corpus.ExecutableCorpus;
import de.uni_mannheim.swt.lasso.datasource.maven.MavenDataSource;
import de.uni_mannheim.swt.lasso.datasource.maven.build.Candidate;
import de.uni_mannheim.swt.lasso.datasource.maven.lsl.MavenQuery;
import de.uni_mannheim.swt.lasso.engine.LSLExecutionContext;
import de.uni_mannheim.swt.lasso.engine.LassoUtils;
import de.uni_mannheim.swt.lasso.engine.action.annotations.LassoAction;
import de.uni_mannheim.swt.lasso.engine.action.annotations.LassoInput;
import de.uni_mannheim.swt.lasso.engine.action.annotations.Local;
import de.uni_mannheim.swt.lasso.engine.action.annotations.Stable;
import de.uni_mannheim.swt.lasso.engine.action.maven.MavenAction;
import de.uni_mannheim.swt.lasso.engine.action.maven.support.MavenProjectManager;
import de.uni_mannheim.swt.lasso.engine.action.maven.support.Mavenizer;
import de.uni_mannheim.swt.lasso.engine.environment.ExecutionEnvironmentManager;
import de.uni_mannheim.swt.lasso.engine.environment.MavenExecutionEnvironment;
import de.uni_mannheim.swt.lasso.engine.workspace.Workspace;
import de.uni_mannheim.swt.lasso.gai.openai.Prompt;
import de.uni_mannheim.swt.lasso.gai.openai.util.ContentParser;
import de.uni_mannheim.swt.lasso.lsl.LassoContext;
import de.uni_mannheim.swt.lasso.lsl.SimpleLogger;
import dev.langchain4j.model.ollama.OllamaChatModel;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
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
                        List<String> codeMatches = contentParser.extractCode(response);
                        generatedCode.addAll(codeMatches);

                        // useful package names (human readable)
                        String pkg = myPrompt.getModel().replaceAll("\\W", ""); //StringUtils.replaceEach(prompt.getModel(), new String[]{":", "-"}, new String[]{"_", "_"});

                        // 2. parse code
                        LOG.info("Parsing code");
                        List<CodeUnit> units = generatedCode.stream().map(c -> parse(c, pkg)).filter(Objects::nonNull).collect(Collectors.toList());
                        // 3. store code in Maven project
                        LOG.info("Creating Maven project");
                        MavenProject mavenProject = createProject(context, abstraction, units, myPrompt, pkg);

                        // 4. package and deploy
                        LOG.info("Package and deploy code");
                        // create manager
                        MavenProjectManager manager = new MavenProjectManager(context);
                        List<String> pkgArgs = doPackage(context, actionConfiguration, manager, mavenProject);

                        // 5. index
                        LOG.info("Index code");
                        List<String> indexArgs = doAnalyzeAndStore(context, actionConfiguration, manager, myPrompt, mavenProject, ds);

                        // run maven
                        List<List<String>> allArgs = new ArrayList<>();
                        allArgs.add(pkgArgs);
                        allArgs.add(indexArgs);
                        runMaven(context, actionConfiguration, manager, mavenProject, allArgs);

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

    List<String> doPackage(LSLExecutionContext context, ActionConfiguration actionConfiguration, MavenProjectManager manager, MavenProject mavenProject) {
        File projectRoot = mavenProject.getBaseDir();

        // args passed
        List<String> args = new ArrayList<>(MavenAction.MAVEN_DEFAULT_COMMAND); // FIXME log is overridden if called multiple times
        // we need to change the deployment server to ours (foreign POMs may specify their own or none)
        // see https://maven.apache.org/plugins/maven-deploy-plugin/deploy-mojo.html
        String mojo = deploy ? "deploy" : "package";

        if (StringUtils.isBlank(repoUrl)) {
            ExecutableCorpus executableCorpus = context.getConfiguration().getExecutableCorpus();
            repoUrl = executableCorpus.getArtifactRepository().getDeploymentUrl();
            repoId = executableCorpus.getArtifactRepository().getId();
        }

        // FIXME make configurable
        args.addAll(
                Arrays.asList(
                        "-DskipTests",
                        "-Drat.skip=true", // not really necessary, just for this commons-lang example
                        "-DaltDeploymentRepository=" + repoId + "::default::" + repoUrl,
                        "-DaltReleaseDeploymentRepository=" + repoId + "::default::" + repoUrl,
                        "-DaltSnapshotDeploymentRepository=" + repoId + "::default::" + repoUrl,
                        "clean",
                        // also make sure to deploy a source file! (see maven-source-plugin https://maven.apache.org/plugins/maven-source-plugin/usage.html)
                        "source:jar",
                        // "source:test-jar",
                        mojo // also compiles everything
                ));

        if (LOG.isInfoEnabled()) {
            LOG.info("Packaging '{}' with args '{}'", mavenProject.getBaseDir(), args);
        }

        return args;
    }

    void runMaven(LSLExecutionContext context, ActionConfiguration actionConfiguration, MavenProjectManager manager, MavenProject mavenProject, List<List<String>> args) {
        File projectRoot = mavenProject.getBaseDir();
        //
        ExecutionEnvironmentManager executionEnvironmentManager = context.getExecutionEnvironmentManager();
        MavenExecutionEnvironment mavenExecutionEnvironment = createMavenEnvironment(context, actionConfiguration, manager, projectRoot, args);
        executionEnvironmentManager.run(mavenExecutionEnvironment);
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
            mavenQuery.setDirectly(true);
            QueryResult queryResult = ds.query(mavenQuery);

            return queryResult.getImplementations().stream().map(System::new).collect(Collectors.toList());
        } catch (Throwable e) {
            LOG.warn("Could not get classes", e);

            throw e;
        }
    }

    MavenProject createProject(LSLExecutionContext context, Abstraction abstraction, List<CodeUnit> units, Prompt prompt, String pkg) throws IOException {
        if (LOG.isInfoEnabled()) {
            LOG.info("Executing " + this.getClass());
        }

        Workspace workspace = context.getWorkspace();
        File abstractionRoot = workspace.createDirectory(abstraction.getName());

        // init other stuff
        Map<String, String> mvnOptions = new HashMap<>();

        Candidate candidate = new Candidate();
        // set id
        candidate.setId(UUID.randomUUID().toString());
        // artifact
        MavenArtifact artifact = new MavenArtifact();

        artifact.setGroupId(pkg);
        artifact.setArtifactId(candidate.getId() + "-gai" + "_" + prompt.getId());
        artifact.setVersion(String.valueOf(prompt.getSampleId()));
        candidate.setArtifact(artifact);

        Mavenizer mavenizer = new Mavenizer(abstractionRoot, mvnOptions);

        Map<String, Object> valueMap = new HashMap<>();
        valueMap.put("javaVersion", javaVersion);

        // add dependencies
        resolveDependencies(candidate, abstraction, mavenizer, valueMap);

        // mavenize, setup project
        MavenProject mavenProject = null;
        try {
            mavenProject = mavenizer.createMavenProject(context, getInstanceId(),
                    candidate, true, POM_TEMPLATE, valueMap);
        } catch (IOException e) {
            LOG.warn("Exception", e);
        }

        for (CodeUnit unit : units) {
            LOG.info("Writing source code to target '{}'", mavenProject.getBaseDir());
            try {
                mavenProject.writeCompilationUnit(unit, false);
            } catch (IOException e) {
                LOG.warn("Writing source code failed", e);
            }
        }

        return mavenProject;
    }

    void resolveDependencies(Candidate candidate, Abstraction abstraction, Mavenizer mavenizer, Map<String, Object> valueMap) {
        // determine dependencies directly from abstraction and/or tests
        if(CollectionUtils.isNotEmpty(abstraction.getSpecification().getDependencies())) {
            //
            List<Artifact> artifacts = abstraction.getSpecification().getDependencies().stream().map(coordinate -> {
                String[] parts = StringUtils.split(coordinate, ":");

                MavenArtifact dep = new MavenArtifact();
                dep.setGroupId(parts[0]);
                dep.setArtifactId(parts[1]);
                dep.setVersion(parts[2]);

                return (Artifact) dep;
            }).toList();
            candidate.setDependencies(artifacts);

            List<String> pomDeps = artifacts.stream()
                    .map(dep -> mavenizer.toSingleDependencyDeclaration(dep.asType(MavenArtifact.class)))
                    .toList();

            // write all dependencies to pom
            valueMap.put("codeDependencies", String.join("\n", pomDeps));
        } else {
            // write all dependencies to pom
            valueMap.put("codeDependencies", "");
        }
    }

    MavenExecutionEnvironment createMavenEnvironment(LSLExecutionContext context, ActionConfiguration actionConfiguration, MavenProjectManager manager, File projectRoot, List<List<String>> args) {
        //
        ExecutionEnvironmentManager executionEnvironmentManager = context.getExecutionEnvironmentManager();

        // set default commands
        Environment environment = actionConfiguration.getProfile().getEnvironment().copy();

        if (CollectionUtils.isEmpty(environment.getCommandArgsList())) {
            environment.setCommandArgsList(new LinkedList<>());
        }

        MavenExecutionEnvironment mavenExecutionEnvironment =
                (MavenExecutionEnvironment) executionEnvironmentManager.createExecutionEnvironment("maven");

        // set image
        mavenExecutionEnvironment.setImage(environment.getImage());

        mavenExecutionEnvironment.setProjectRoot(context.getWorkspace(), projectRoot);
        mavenExecutionEnvironment.setM2Home(context.getWorkspace(), manager.getM2Home());

        List<String> commands = new LinkedList<>();
        List<List<String>> commandArgsList = new LinkedList<>();
        commandArgsList.addAll(args);
        for (List<String> commandArgs : commandArgsList) {
            String command = String.join(" ", commandArgs);
            commands.add(command);
        }

        mavenExecutionEnvironment.setCommands(commands);

        return mavenExecutionEnvironment;
    }

    List<String> doAnalyzeAndStore(LSLExecutionContext context, ActionConfiguration actionConfiguration, MavenProjectManager manager, Prompt prompt, MavenProject mavenProject, Datasource ds) {
        // mvn indexer-maven-plugin:index
        File projectRoot = mavenProject.getBaseDir();

        // args passed
        List<String> args = new ArrayList<>(MavenAction.MAVEN_DEFAULT_COMMAND);

        // FIXME add more metadata
        // metadata=key1,value1|key2,value2 etc.
        String metadata = "\"executionId," + context.getExecutionId()
                + "|" + "action," + getName()
                + "|" + "model," + prompt.getModel()
                + "|" + "abstractionId," + actionConfiguration.getAbstraction().getName()
                + "|" + "sampleId," + prompt.getSampleId()
                + "|" + "promptId," + prompt.getId()
                + "\"";

        String core = StringUtils.substringAfterLast(ds.getHost(), "/");
        String url = StringUtils.substringBeforeLast(ds.getHost(), "/");

        args.addAll(
                Arrays.asList(
                        "-DskipTests",
                        "-Dindex.url=" + url,
                        "-Dindex.user=" + ds.getUser(),
                        "-Dindex.pass=" + ds.getPass(),
                        "-Dindex.core=" + core,
                        "-Dindex.owner=" + "lasso",
                        "-Dindex.metadata=" + metadata,
                        "de.uni-mannheim.swt.lasso:indexer-maven-plugin:1.0.0-SNAPSHOT:index"
                ));

        if (LOG.isInfoEnabled()) {
            LOG.info("Indexing '{}' with args '{}'", mavenProject.getBaseDir(), args);
        }

        return args;
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
