package de.uni_mannheim.swt.lasso.engine.build;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import de.uni_mannheim.swt.lasso.core.model.*;
import de.uni_mannheim.swt.lasso.corpus.Datasource;
import de.uni_mannheim.swt.lasso.corpus.ExecutableCorpus;
import de.uni_mannheim.swt.lasso.datasource.maven.build.Candidate;
import de.uni_mannheim.swt.lasso.engine.LSLExecutionContext;
import de.uni_mannheim.swt.lasso.engine.action.DefaultAction;
import de.uni_mannheim.swt.lasso.engine.action.maven.MavenAction;
import de.uni_mannheim.swt.lasso.engine.action.maven.support.MavenProjectManager;
import de.uni_mannheim.swt.lasso.engine.action.maven.support.Mavenizer;
import de.uni_mannheim.swt.lasso.engine.environment.ExecutionEnvironmentManager;
import de.uni_mannheim.swt.lasso.engine.environment.MavenExecutionEnvironment;
import de.uni_mannheim.swt.lasso.engine.workspace.Workspace;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 *
 *
 * @author Marcus Kessel
 */
public class JavaProjectBuildManager implements ProjectBuildManager {

    private static final Logger LOG = LoggerFactory
            .getLogger(JavaProjectBuildManager.class);

    @Override
    public void store(DefaultAction action, LSLExecutionContext context, ActionConfiguration actionConfiguration, Abstraction abstraction, List<CodeUnit> units, ProjectBuildConfiguration buildConfiguration) {
        try {
            // 1. store code in Maven project
            LOG.info("Creating Maven project");
            MavenProject mavenProject = createProject(action, context, abstraction, units, buildConfiguration);

            // 2. package and deploy
            LOG.info("Packaging and deploying code");
            List<String> pkgArgs = doPackage(context, mavenProject, buildConfiguration);

            // 3. index code in executable corpus
            LOG.info("Indexing code");
            // create manager
            MavenProjectManager manager = new MavenProjectManager(context);
            List<String> indexArgs = doAnalyzeAndStore(mavenProject, buildConfiguration);

            // run maven
            List<List<String>> allArgs = new ArrayList<>();
            allArgs.add(pkgArgs);
            allArgs.add(indexArgs);
            runMaven(context, actionConfiguration, manager, mavenProject, allArgs);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public CodeUnit parse(String code, String namespace) {
        if (LOG.isInfoEnabled()) {
            LOG.info("Parsing code\n{}", code);
        }

        try {
            JavaParser javaParser = new JavaParser();
            ParseResult<CompilationUnit> result = javaParser.parse(code);
            if(!result.isSuccessful()) {
                LOG.warn("Parser found the following problems: {}", result.getProblems());
            }

            com.github.javaparser.ast.CompilationUnit cu = result.getResult().get();

            // parse name
            CodeUnit unit = new CodeUnit();
            unit.setId(UUID.randomUUID().toString());
            unit.setName(cu.getType(0).getNameAsString());

            // add package name
            cu.setPackageDeclaration(namespace);

            unit.setPackagename(namespace);
            unit.setContent(cu.toString());
            unit.setUnitType(CodeUnit.CodeUnitType.CLASS);
            unit.setLang(CodeUnit.JAVA);

            return unit;
        } catch (Throwable e) {
            LOG.warn("failed to parse code", e);
            return null;
        }
    }

    List<String> doPackage(LSLExecutionContext context, MavenProject mavenProject, ProjectBuildConfiguration buildConfiguration) throws IOException {
        File projectRoot = mavenProject.getBaseDir();

        // args passed
        List<String> args = new ArrayList<>(MavenAction.MAVEN_DEFAULT_COMMAND); // FIXME log is overridden if called multiple times
        // we need to change the deployment server to ours (foreign POMs may specify their own or none)
        // see https://maven.apache.org/plugins/maven-deploy-plugin/deploy-mojo.html
        String mojo = buildConfiguration.isDeploy() ? "deploy" : "package";

        String repoUrl = buildConfiguration.getRepoUrl();
        String repoId = buildConfiguration.getRepoId();
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

    List<String> doAnalyzeAndStore(MavenProject mavenProject, ProjectBuildConfiguration buildConfiguration) {
        // mvn indexer-maven-plugin:index
        File projectRoot = mavenProject.getBaseDir();

        // args passed
        List<String> args = new ArrayList<>(MavenAction.MAVEN_DEFAULT_COMMAND);

        // add more metadata
        String metadata = "\"" + buildConfiguration.getMeta().entrySet().stream().map(e -> e.getKey() + "," + e.getValue()).collect(Collectors.joining("|")) + "\"";

        Datasource ds = buildConfiguration.getDataSource();
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

    void runMaven(LSLExecutionContext context, ActionConfiguration actionConfiguration, MavenProjectManager manager, MavenProject mavenProject, List<List<String>> args) {
        File projectRoot = mavenProject.getBaseDir();
        //
        ExecutionEnvironmentManager executionEnvironmentManager = context.getExecutionEnvironmentManager();
        MavenExecutionEnvironment mavenExecutionEnvironment = createMavenEnvironment(context, actionConfiguration, manager, projectRoot, args);
        executionEnvironmentManager.run(mavenExecutionEnvironment);
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

    MavenProject createProject(DefaultAction action, LSLExecutionContext context, Abstraction abstraction, List<CodeUnit> units, ProjectBuildConfiguration buildConfiguration) throws IOException {
        if (LOG.isInfoEnabled()) {
            LOG.info("Executing " + this.getClass());
        }

        Workspace workspace = context.getWorkspace();
        File abstractionRoot = workspace.createDirectory(abstraction.getName());

        // init other stuff
        Map<String, String> mvnOptions = new HashMap<>();

        Candidate candidate = new Candidate();
        // set id
        candidate.setId(buildConfiguration.getId());
        // artifact
        MavenArtifact artifact = new MavenArtifact();

        artifact.setGroupId(buildConfiguration.getGroupId());
        artifact.setArtifactId(buildConfiguration.getArtifactId());
        artifact.setVersion(buildConfiguration.getVersion());
        candidate.setArtifact(artifact);

        Mavenizer mavenizer = new Mavenizer(abstractionRoot, mvnOptions);

        Map<String, Object> valueMap = new HashMap<>();
        valueMap.put("javaVersion", buildConfiguration.getLangVersion());

        // add dependencies
        resolveDependencies(candidate, abstraction, mavenizer, valueMap);

        // mavenize, setup project
        MavenProject mavenProject = null;
        try {
            mavenProject = mavenizer.createMavenProject(context, action.getInstanceId(),
                    candidate, true, buildConfiguration.getProjectTemplate(), valueMap);
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
}
