package randoop.maven;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.*;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

//@Mojo(name = "gentests", defaultPhase = LifecyclePhase.GENERATE_TEST_SOURCES, threadSafe = true,
//        requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
@Mojo( name = "generate" , requiresDependencyResolution = ResolutionScope.TEST, requiresDependencyCollection = ResolutionScope.TEST)
@Execute(phase = LifecyclePhase.COMPILE)
public class RandoopMojo extends AbstractMojo {

    @Parameter(defaultValue = "${plugin.artifacts}", required = true, readonly = true)
    private List<Artifact> artifacts;

    @Parameter(required = true)
    private String packageName;

    @Parameter(required = true, defaultValue = "${project.build.outputDirectory}")
    private String sourceDirectory;

    @Parameter(required = true, defaultValue = "300")
    private int timeoutInSeconds;

    @Parameter(required = true, defaultValue = "60")
    private int timeLimitInSeconds;

    @Parameter(required = true, defaultValue = "500")
    private int maxNumberOfTestsPerFile;

    //-- lasso modification
    @Parameter( property = "lassoClass", defaultValue = "" )
    private String lassoClass;

    /**
     * List of classes to mutate
     */
    @Parameter(required = false, defaultValue = "")
    private String[] extraArgs;

    @Component
    private MavenProject project;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        // Collect class and jars for class path
        final List<URL> urls = new LinkedList<>();
        urls.add(loadProjectClasses());
        urls.addAll(loadProjectDependencies(project));
        try {
            urls.add(loadPluginJarWithRandoop());
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }

        getLog().info("Running in normal mode");

        // don't care
        run(urls, -1);
    }

    private void run(List<URL> urls, int mutantId) throws MojoExecutionException {
        getLog().info("Running in normal mode");

        final List<String> args = buildArgs(urls, mutantId);

        final String randoopCommandLine = args.stream().collect(Collectors.joining(" "));

        getLog().info("Call outside Maven: " + randoopCommandLine);

        runRandoopGenTests(args);
    }

    private List<String> buildArgs(final List<URL> urls, int mutantId) {
        String classPath = urls.stream()
                .map(u -> u.toString())
                .collect(Collectors.joining(File.pathSeparator));

        // Build up Randoop command line
        final List<String> args = new LinkedList<>();
        args.add("java");
        args.add("-ea");
        args.add("-classpath");
        args.add(classPath);

        args.add("randoop.main.Main");
        args.add("gentests");
//    args.add("--timelimit=" + timeoutInSeconds); new Randoop does not support this anymore

        // disable debug checks
        args.add("--debug-checks=false");
        args.add("--junit-package-name=" + packageName);
        args.add("--junit-output-dir=" + new File(this.project.getBasedir(), "randoop-tests"));

        // how many test cases per test class
        //args.add("--testsperfile=" + maxNumberOfTestsPerFile);

        // Add project classes
//        final URLClassLoader classLoader = new URLClassLoader(convert(urls));
//        List<Class<?>> allClassesOfPackage = ClassFinder.find(packageName, classLoader);
//        for (Class<?> currentClass : allClassesOfPackage) {
////            getLog().debug("FOUND class " + currentClass.getName());
////            // only merobase adapters
////            if (currentClass.getName().endsWith("_Adapter")) {
////                getLog().info("Add class " + currentClass.getName());
////                args.add("--testclass=" + currentClass.getName());
////            }
//
//        }

        getLog().debug("FOUND class " + lassoClass);
        args.add("--testclass=" + lassoClass);

        // add time limit
        //--time-limit=60
        if(timeLimitInSeconds > 0) {
            args.add("--time-limit=" + timeLimitInSeconds);
        } else {
            getLog().info("Ignoring time limit");
        }

        // omit testing of PERMUTATOR INSTANCE FIELD
        // --omit-field=
        // ASSUME THAT ONLY ADAPTERS existing in src/main/java
//        for (Class<?> currentClass : allClassesOfPackage) {
//            // only merobase adapters
//            if (currentClass.getName().endsWith("_Adapter")) {
//                String fieldToOmit = currentClass.getName() + ".PERMUTATOR_INSTANCE";
//                getLog().info("Omitting field " + fieldToOmit);
//                args.add("--omit-field=" + fieldToOmit);
//            }
//        }

//        // change name of regression suite only if an actual mutant is configured (see Config).
//        if (mutantId > 0) {
//            // set regression test name
//
//            args.add("--regression-test-basename=" + "Mutant_" + mutantId + "_RegressionTest");
//        }

        // extra args
        if(extraArgs != null && extraArgs.length > 0) {
            for(String arg : extraArgs) {
                args.add(arg);
            }
        }

        return args;
    }

    private void runRandoopGenTests(final List<String> args) throws MojoExecutionException {
        // redirect to stdout
        ProcessBuilder processBuilder = new ProcessBuilder(args).inheritIO().directory(project.getBasedir());

        try {
            Process randoopProcess = processBuilder.start();
            getLog().info("Randoop started with time limit of " + timeoutInSeconds + " seconds.");
            // be patient
            int waitForSecsBuffer = 30;
            randoopProcess.waitFor(timeoutInSeconds + waitForSecsBuffer, TimeUnit.SECONDS);

            getLog().info("Randoop finished with status " + randoopProcess.exitValue());
//      if (randoopProcess.exitValue() != 0) {
//        throw new MojoFailureException(this, "Randoop encountered an error!", "Failed to generate " +
//            "test, exit value is " + randoopProcess.exitValue());
//      }
        } catch (Exception e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

    private URL loadPluginJarWithRandoop() throws MalformedURLException {
        //return getClass().getProtectionDomain().getCodeSource().getLocation();

        Artifact randoop = null;

        for(Artifact art : artifacts){
            if(art.getArtifactId().equals("randoop")){
                randoop = art;
                break;
            }
        }

        return randoop.getFile().toURI().toURL();
    }

    private static List<URL> loadProjectDependencies(final MavenProject project) throws MojoExecutionException {
        final List<URL> urls = new LinkedList<>();
        try {
            for (Artifact artifact : project.getArtifacts()) {
                urls.add(artifact.getFile().toURI().toURL());
            }
        } catch (MalformedURLException e) {
            throw new MojoExecutionException("Could not add artifact!", e);
        }
        return urls;
    }

    private URL loadProjectClasses() throws MojoExecutionException {
        final URL source;
        try {
            source = createUrlFrom(sourceDirectory);
        } catch (MalformedURLException e) {
            throw new MojoExecutionException("Could not create source path!", e);
        }
        return source;
    }

    private static URL[] convert(final Collection<URL> urls) {
        return urls.toArray(new URL[urls.size()]);
    }

    private static URL createUrlFrom(final String path) throws MalformedURLException {
        return new File(path).toURI().toURL();
    }
}
