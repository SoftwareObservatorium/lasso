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

import com.github.javaparser.JavaParser;
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
import de.uni_mannheim.swt.lasso.engine.action.DefaultAction;
import de.uni_mannheim.swt.lasso.engine.action.annotations.LassoAction;
import de.uni_mannheim.swt.lasso.engine.action.annotations.LassoInput;
import de.uni_mannheim.swt.lasso.engine.action.annotations.Local;
import de.uni_mannheim.swt.lasso.engine.action.annotations.Stable;

import de.uni_mannheim.swt.lasso.engine.action.maven.MavenAction;
import de.uni_mannheim.swt.lasso.engine.action.maven.support.MavenProjectManager;
import de.uni_mannheim.swt.lasso.engine.action.maven.support.Mavenizer;
import de.uni_mannheim.swt.lasso.engine.action.test.support.adaptation.TestAdaptationManager;

import de.uni_mannheim.swt.lasso.engine.environment.ExecutionEnvironmentManager;
import de.uni_mannheim.swt.lasso.engine.environment.MavenExecutionEnvironment;
import de.uni_mannheim.swt.lasso.engine.workspace.Workspace;
import de.uni_mannheim.swt.lasso.gai.openai.*;
import de.uni_mannheim.swt.lasso.gai.openai.util.ContentParser;
import de.uni_mannheim.swt.lasso.lsl.LassoContext;
import de.uni_mannheim.swt.lasso.lsl.SimpleLogger;
import de.uni_mannheim.swt.lasso.lsl.spec.AbstractionSpec;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * GAI Action based on OpenAI's RESTful API.
 *
 * Supports both official OpenAI service (ChatGPT) as well as non-OpenAI services (e.g., gpt4all).
 *
 * @see <a href="https://platform.openai.com/docs/api-reference/completions/create">OpenAI Spec</a>
 * @see <a href="https://docs.gpt4all.io/gpt4all_chat.html#server-mode">Gpt4All Spec</a>
 *
 * @author Marcus Kessel
 */
@LassoAction(desc = "Generate Systems Code with (Open)AI")
@Stable
@Local
public class GenerativeAI extends DefaultAction {

    private static final Logger LOG = LoggerFactory
            .getLogger(GenerativeAI.class);

    private static final String POM_TEMPLATE =
            Mavenizer.getPomTemplate("/mavenizer/pom_gai.template");
    public static final String DE_UNI_MANNHEIM_SWT_LASSO_SYSTEMS_GAI = "de.uni_mannheim.swt.lasso.systems.gai";
    public static final String URL = "https://api.openai.com/v1/chat/completions";

    @LassoInput(desc = "Data Source", optional = true)
    public String dataSource = "gitimport";

    @LassoInput(desc = "How to match code snippet", optional = true)
    public String regex;

    @LassoInput(desc = "API Key", optional = true)
    public String apiKey = "";

    @LassoInput(desc = "API Url (non-OpenAI servers)", optional = true)
    public String apiUrl = "https://api.openai.com/v1/chat/completions";

    @LassoInput(desc = "deploy package to LASSO Nexus", optional = true)
    public boolean deploy = true;

    @LassoInput(desc = "Maven Repository for Deployment", optional = true)
    public String repoUrl;
    @LassoInput(desc = "Maven Repository Id for Deployment", optional = true)
    public String repoId;

    @LassoInput(desc = "Set Java version for compilation", optional = true)
    public String javaVersion = "17";

    @Override
    public Abstraction createAbstraction(LSLExecutionContext context, ActionConfiguration actionConfiguration, AbstractionSpec abstractionSpec) throws IOException {
        Abstraction abstraction = new Abstraction();
        abstraction.setName(abstractionSpec.getName());

        // get query model
        Prompt prompt = new Prompt();
        abstractionSpec.getLasso().register(prompt);
        abstractionSpec.apply(prompt);

        // 1. generate code
        LOG.info("Generating code");
        CompletionResponse response = generate(prompt);

        if(LOG.isDebugEnabled()) {
            LOG.debug("Response\n}{}", ToStringBuilder.reflectionToString(response));
        }

        ContentParser contentParser = new ContentParser();
        List<String> generatedCode = new LinkedList<>();
        for(Choice choice : response.getChoices()) {
            List<String> codeMatches = contentParser.extractCode(choice.getMessage().getContent());
            generatedCode.addAll(codeMatches);
        }
        // 2. parse code
        LOG.info("Parsing code");
        List<CodeUnit> units = generatedCode.stream().map(this::parse).collect(Collectors.toList());
        // 3. store code in Maven project
        LOG.info("Creating Maven project");
        MavenProject mavenProject = createProject(context, abstractionSpec, units);

        // 4. package and deploy
        LOG.info("Package and deploy code");
        // create manager
        MavenProjectManager manager = new MavenProjectManager(context);
        doPackage(context, actionConfiguration, manager, mavenProject);

        // 5. index
        LOG.info("Index code");
        doAnalyzeAndStore(context, actionConfiguration, manager, prompt, mavenProject);

        // 6. select from data source
        LOG.info("Selecting code");
        String dataSourceId = dataSource != null ? dataSource : abstractionSpec.getLasso().getDataSources().get(0);
        List<System> systems = select(context, dataSourceId);
        abstraction.setSystems(systems);

        // TODO
        //abstraction.getSpecification().setInterfaceSpecification(queryResult.getInterfaceSpecification());

        return abstraction;
    }

    void doPackage(LSLExecutionContext context, ActionConfiguration actionConfiguration, MavenProjectManager manager, MavenProject mavenProject) {
        File projectRoot = mavenProject.getBaseDir();

        // args passed
        List<String> args = new ArrayList<>(MavenAction.MAVEN_DEFAULT_COMMAND); // FIXME log is overridden if called multiple times
        // we need to change the deployment server to ours (foreign POMs may specify their own or none)
        // see https://maven.apache.org/plugins/maven-deploy-plugin/deploy-mojo.html
        String mojo = deploy ? "deploy" : "package";

        if(StringUtils.isBlank(repoUrl)) {
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

        if(LOG.isInfoEnabled()) {
            LOG.info("Packaging '{}' with args '{}'", mavenProject.getBaseDir(), args);
        }

        //
        ExecutionEnvironmentManager executionEnvironmentManager = context.getExecutionEnvironmentManager();
        MavenExecutionEnvironment mavenExecutionEnvironment = createMavenEnvironment(context, actionConfiguration, manager, projectRoot, args);
        executionEnvironmentManager.run(mavenExecutionEnvironment);
    }

    @Override
    public List<Abstraction> createAbstractions(LSLExecutionContext context, ActionConfiguration actionConfiguration) throws IOException {
        return null;
    }

    //@Override
    // FIXME remove
    protected MavenProjectManager createManager(LSLExecutionContext context, ActionConfiguration actionConfiguration) throws IOException {
        if(LOG.isInfoEnabled()) {
            LOG.info("Executing "+  this.getClass());
        }

        TestAdaptationManager testAdaptationManager = new TestAdaptationManager(context);

        Systems executables = testAdaptationManager.initNew(this,
                getInstanceId(),
                actionConfiguration.getAbstraction(),
                POM_TEMPLATE,
                (implementation, candidate, valueMap) -> {
                    // FIXME create dep info for deploy
                    valueMap.put("version", implementation.getVersion() + "-SNAPSHOT");
                },
                executable -> {
                    // save source
                    MavenProject targetProject = executable.getProject();

                    LOG.info("Writing source code to target '{}'", targetProject.getBaseDir());
                    try {
                        targetProject.writeCompilationUnit(executable.getCode(), false);
                    } catch (IOException e) {
                        LOG.warn("Writing source code failed", e);

                        return false;
                    }

                    return true;
                });

        Validate.notNull(executables, "Executables are null");

        // set
        setExecutables(executables);

        return testAdaptationManager;
    }

    private CompletionResponse generate(Prompt prompt) {
        OpenAiClient client = new OpenAiClient(apiUrl,
                apiKey);

        CompletionResponse response = null;
        if(StringUtils.equalsIgnoreCase(apiUrl, URL)) {
            OpenAiCompletionRequest request = new OpenAiCompletionRequest();
            Message message = new Message();
            message.setRole(prompt.getRole());
            message.setContent(prompt.getPromptContent());

            request.setN(prompt.getN());
            request.setTemperature(prompt.getTemperature());
            request.setTop_p(prompt.getTop_p());
            request.setPresence_penalty(prompt.getPresence_penalty());
            request.setFrequency_penalty(prompt.getFrequency_penalty());
            request.setMax_tokens(prompt.getMax_tokens());
            request.setStream(false); // return complete response

            request.setMessages(Collections.singletonList(message));

            request.setModel(prompt.getModel());

            response = client.complete(request);
        } else {
            Gpt4AllCompletionRequest request = new Gpt4AllCompletionRequest();
            Message message = new Message();
            message.setRole(prompt.getRole());
            message.setContent(prompt.getPromptContent());

            request.setN(prompt.getN());
            request.setTemperature(prompt.getTemperature());
            request.setTop_p(prompt.getTop_p());
            request.setTop_k(prompt.getTop_k());
            request.setRepeat_penalty(prompt.getRepeat_penalty());
            request.setMax_tokens(prompt.getMax_tokens());
            request.setStream(false); // return complete response
            request.setEcho(prompt.getEcho()); // include prompt

            request.setMessages(Collections.singletonList(message));

            // must contain file suffix
            request.setModel(prompt.getModel());

            response = client.complete(request);
        }

        return response;
    }

    List<System> select(LSLExecutionContext context, String dataSourceId) throws IOException {
        DataSource dataSource = context.getDataSourceMap().get(dataSourceId);
        try {
            MavenDataSource ds = (MavenDataSource) dataSource;
            MavenQuery mavenQuery = (MavenQuery) ds.createQueryModelForLSL();
            LassoContext ctx = new LassoContext();
            ctx.setLogger(new SimpleLogger());
            mavenQuery.setLasso(ctx);
            mavenQuery.queryForClasses("*:*");
            mavenQuery.filter("executionId:\""+ context.getExecutionId() +"\"");
            mavenQuery.filter("action:\""+ getName() +"\"");
            mavenQuery.setDirectly(true);
            QueryResult queryResult = ds.query(mavenQuery);

            return queryResult.getImplementations().stream().map(System::new).collect(Collectors.toList());
        } catch (Throwable e) {
            LOG.warn("Could not get classes", e);

            throw e;
        }
    }

    CodeUnit parse(String code) {
        JavaParser javaParser = new JavaParser();
        com.github.javaparser.ast.CompilationUnit cu = javaParser.parse(code).getResult().get();

        // parse name
        CodeUnit unit = new CodeUnit();
        unit.setId(UUID.randomUUID().toString());
        unit.setName(cu.getType(0).getNameAsString());

        // add package name
        cu.setPackageDeclaration(DE_UNI_MANNHEIM_SWT_LASSO_SYSTEMS_GAI);

        unit.setPackagename(DE_UNI_MANNHEIM_SWT_LASSO_SYSTEMS_GAI);
        unit.setContent(cu.toString());
        unit.setUnitType(CodeUnit.CodeUnitType.CLASS);

        return unit;
    }

    MavenProject createProject(LSLExecutionContext context, AbstractionSpec abstractionSpec, List<CodeUnit> units) throws IOException {
        if(LOG.isInfoEnabled()) {
            LOG.info("Executing "+  this.getClass());
        }

        Workspace workspace = context.getWorkspace();
        File abstractionRoot = workspace.createDirectory(abstractionSpec.getName());

        // init other stuff
        Map<String, String> mvnOptions = new HashMap<>();

        Candidate candidate = new Candidate();
        // set id
        candidate.setId(UUID.randomUUID().toString());
        // artifact
        MavenArtifact artifact = new MavenArtifact();
        artifact.setGroupId(DE_UNI_MANNHEIM_SWT_LASSO_SYSTEMS_GAI);
        artifact.setArtifactId(candidate.getId() + "-gai");
        artifact.setVersion(java.lang.System.currentTimeMillis() + "");
        candidate.setArtifact(artifact);

        Mavenizer mavenizer = new Mavenizer(abstractionRoot, mvnOptions);

        Map<String, Object> valueMap = new HashMap<>();
        valueMap.put("javaVersion", javaVersion);

        // mavenize, setup project
        MavenProject mavenProject = null;
        try {
            mavenProject = mavenizer.createMavenProject(context, getInstanceId(),
                    candidate, true, POM_TEMPLATE, valueMap);
        } catch (IOException e) {
            LOG.warn("Exception", e);
        }

        for(CodeUnit unit : units) {
            LOG.info("Writing source code to target '{}'", mavenProject.getBaseDir());
            try {
                mavenProject.writeCompilationUnit(unit, false);
            } catch (IOException e) {
                LOG.warn("Writing source code failed", e);
            }
        }

        return mavenProject;
    }

    MavenExecutionEnvironment createMavenEnvironment(LSLExecutionContext context, ActionConfiguration actionConfiguration, MavenProjectManager manager, File projectRoot, List<String> args) {
        //
        ExecutionEnvironmentManager executionEnvironmentManager = context.getExecutionEnvironmentManager();

        // set default commands
        Environment environment = actionConfiguration.getProfile().getEnvironment();
        if(CollectionUtils.isEmpty(environment.getCommandArgsList())) {
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
        commandArgsList.add(args);
        for (List<String> commandArgs : commandArgsList) {
            String command = String.join(" ", commandArgs);
            commands.add(command);
        }

        mavenExecutionEnvironment.setCommands(commands);

        return mavenExecutionEnvironment;
    }

    void doAnalyzeAndStore(LSLExecutionContext context, ActionConfiguration actionConfiguration, MavenProjectManager manager, Prompt prompt, MavenProject mavenProject) {
        // mvn indexer-maven-plugin:index
        File projectRoot = mavenProject.getBaseDir();

        // args passed
        List<String> args = new ArrayList<>(MavenAction.MAVEN_DEFAULT_COMMAND);

        // FIXME add more metadata
        // metadata=key1,value1|key2,value2 etc.
        String metadata = "\"executionId," + context.getExecutionId()
                + "|" + "action," + getName()
                + "|" + "model," + prompt.getModel()
                + "\"";

        ExecutableCorpus executableCorpus = context.getConfiguration().getExecutableCorpus();
        // FIXME select default ds
        Datasource ds = executableCorpus.getDatasources().get(0);

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

        if(LOG.isInfoEnabled()) {
            LOG.info("Indexing '{}' with args '{}'", mavenProject.getBaseDir(), args);
        }

        //
        ExecutionEnvironmentManager executionEnvironmentManager = context.getExecutionEnvironmentManager();
        MavenExecutionEnvironment mavenExecutionEnvironment = createMavenEnvironment(context, actionConfiguration, manager, projectRoot, args);
        executionEnvironmentManager.run(mavenExecutionEnvironment);
    }

    @Override
    public void execute(LSLExecutionContext context, ActionConfiguration actionConfiguration) throws IOException {
        if(LOG.isInfoEnabled()) {
            LOG.info("Executing "+  this.getClass());
        }

        Abstraction abstraction = actionConfiguration.getAbstraction();

        LOG.info("Abstraction = {}", abstraction.getName());
        LOG.info("Systems = {}", abstraction.getImplementations().size());
        for(System implementation : abstraction.getImplementations()) {
            LOG.info(">> System = {}, {}", implementation.getId(), implementation.getCode().toFQName());
        }

        setExecutables(Systems.fromAbstraction(abstraction, getName()));
    }
}
