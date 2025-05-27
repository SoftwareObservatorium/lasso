package de.uni_mannheim.swt.lasso.engine.build;

import de.uni_mannheim.swt.lasso.core.model.*;
import de.uni_mannheim.swt.lasso.corpus.Datasource;
import de.uni_mannheim.swt.lasso.datasource.maven.build.Candidate;
import de.uni_mannheim.swt.lasso.engine.LSLExecutionContext;
import de.uni_mannheim.swt.lasso.engine.action.DefaultAction;
import de.uni_mannheim.swt.lasso.engine.environment.ExecutionEnvironmentManager;
import de.uni_mannheim.swt.lasso.engine.environment.PythonBasicExecutionEnvironment;
import de.uni_mannheim.swt.lasso.engine.workspace.Workspace;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 *
 * @author Marcus Kessel
 */
public class PythonProjectBuildManager implements ProjectBuildManager {

    private static final Logger LOG = LoggerFactory
            .getLogger(PythonProjectBuildManager.class);

    @Override
    public void store(DefaultAction action, LSLExecutionContext context, ActionConfiguration actionConfiguration, Abstraction abstraction, List<CodeUnit> units, ProjectBuildConfiguration buildConfiguration) {
        //

        try {
            // 1. store code in project
            LOG.info("Creating Python project");
            // FIXME create sub-type: PythonProject
            MavenProject project = createProject(action, context, abstraction, units, buildConfiguration);

            // FIXME python artifact packaging?

            // (3.) index code in executable corpus
            LOG.info("Indexing Python code");
            List<String> indexArgs = doAnalyzeAndStore(buildConfiguration);
            runPython(action, context, actionConfiguration, project, indexArgs);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public CodeUnit parse(String code, String namespace) {
        if (LOG.isInfoEnabled()) {
            LOG.info("Parsing code\n{}", code);
        }

        CodeUnit unit = new CodeUnit();
        unit.setId(UUID.randomUUID().toString());
        unit.setName("Module"); // FIXME module names

        unit.setPackagename(namespace);
        unit.setUnitType(CodeUnit.CodeUnitType.MODULE);
        unit.setLang(CodeUnit.PYTHON);
        unit.setContent(code);

        return unit;
    }

    void runPython(DefaultAction action, LSLExecutionContext context, ActionConfiguration actionConfiguration, MavenProject project, List<String> args) {
        // set default commands (use copy!)
        Environment environment = actionConfiguration.getProfile().getEnvironment().copy();
        if (CollectionUtils.isEmpty(environment.getCommandArgsList())) {
            environment.setCommandArgsList(new LinkedList<>());
        }

        environment.getCommandArgsList().add(args);

        PythonBasicExecutionEnvironment executionEnvironment = PythonProjectBuildManager.createExecutionEnvironment(action, context, actionConfiguration, environment);
        // set project root
        executionEnvironment.setProjectRoot(context.getWorkspace(), project.getBaseDir());

        if(LOG.isInfoEnabled()) {
            LOG.info("Setting python arena args to '{}'", String.join(",", args));
        }

        ExecutionEnvironmentManager executionEnvironmentManager = context.getExecutionEnvironmentManager();

        executionEnvironmentManager.run(executionEnvironment);
    }

    List<String> doAnalyzeAndStore(ProjectBuildConfiguration buildConfiguration) {
        // args passed to arena

        Datasource ds = buildConfiguration.getDataSource();
        String url = ds.getHost();
        if(!StringUtils.endsWith(url, "/")) {
            url += "/";
        }

        List<String> args = new ArrayList<>(Arrays.asList(
                "--solrurl", url + "update/json/docs",
                "--projectroot", "/project",
                "--batchsize", "50"
                //"&>" + ANALYZER_LOG_TXT
        ));

        // --                 "--meta", "meta_customfield=foo"

        // add more metadata
        List<String> metadata = buildConfiguration.getMeta().entrySet().stream().map(e -> e.getKey() + "=" + e.getValue()).toList();
        for(String m : metadata) {
            args.add("--meta");
            args.add(m);
        }

        // solr
        if (StringUtils.isNotBlank(ds.getUser())) {
            args.add("--solruser");
            args.add(ds.getUser());
        }
        if (StringUtils.isNotBlank(ds.getPass())) {
            args.add("--solrpass");
            args.add(ds.getPass());
        }

        return args;
    }

    MavenProject createProject(DefaultAction action, LSLExecutionContext context, Abstraction abstraction, List<CodeUnit> units, ProjectBuildConfiguration buildConfiguration) throws IOException {
        if (LOG.isInfoEnabled()) {
            LOG.info("Executing " + this.getClass());
        }

        Workspace workspace = context.getWorkspace();
        File abstractionRoot = workspace.createDirectory(abstraction.getName());

        Candidate candidate = new Candidate();
        // set id
        candidate.setId(buildConfiguration.getId());
        // artifact
        MavenArtifact artifact = new MavenArtifact();

        artifact.setGroupId(buildConfiguration.getGroupId());
        artifact.setArtifactId(buildConfiguration.getArtifactId());
        artifact.setVersion(buildConfiguration.getVersion());
        candidate.setArtifact(artifact);

        // FIXME add dependencies
        //resolveDependencies(candidate, abstraction, mavenizer, valueMap);

        // setup project
        MavenProject mavenProject = new MavenProject(new File(abstractionRoot, action.getName() + "/" + candidate.getId()), true);

        int c = 0;
        for (CodeUnit unit : units) {
            LOG.info("Writing source code to target '{}'", mavenProject.getBaseDir());
            try {
                // write
                FileUtils.writeStringToFile(new File(mavenProject.getBaseDir(),
                                unit.getPackagename() + "_" + (c++) + ".py"),
                        unit.getContent());
            } catch (IOException e) {
                LOG.warn("Writing source code failed", e);
            }
        }

        return mavenProject;
    }

    public static PythonBasicExecutionEnvironment createExecutionEnvironment(DefaultAction action, LSLExecutionContext context, ActionConfiguration configuration, Environment environment) {
        //
        ExecutionEnvironmentManager executionEnvironmentManager = context.getExecutionEnvironmentManager();

        PythonBasicExecutionEnvironment executionEnvironment =
                (PythonBasicExecutionEnvironment) executionEnvironmentManager.createExecutionEnvironment(ExecutionEnvironmentManager.PYTHON);

        //
        File projectRoot = context.getWorkspace().getRoot(action.getInstanceId(), configuration.getAbstraction());

        // configure environment
        executionEnvironment.setImage(environment.getImage());

        executionEnvironment.setProjectRoot(context.getWorkspace(), projectRoot);
        executionEnvironment.setCommands(environment.getCommandArgsList().get(0));

        return executionEnvironment;
    }
}
