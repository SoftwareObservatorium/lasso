package de.uni_mannheim.swt.lasso.arena.runner;

import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

/**
 * Task to spawn external Java process that can be controlled (i.e., killed in case of timeouts).
 *
 * @author Marcus Kessel
 */
public class ProcessTask implements Callable<ProcessResult> {

    private static final org.slf4j.Logger LOG = LoggerFactory
            .getLogger(ProcessTask.class);

    private final de.uni_mannheim.swt.lasso.core.model.System system;
    private final String[] originalArgs;

    public ProcessTask(de.uni_mannheim.swt.lasso.core.model.System system, String[] originalArgs) {
        this.system = system;
        this.originalArgs = originalArgs;
    }

    @Override
    public ProcessResult call() throws Exception {
        Process process = null;
        try {
            // Build the command for this specific task
            ArrayList<String> command = buildBaseCommand();

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true); // Easier to read all output from one stream

            // Start and wait for the process
            process = pb.start();

            // Read output (important to prevent process buffer from filling up)
            new StreamGobbler(process.getInputStream(), LOG::info).run(); // We'll print results centrally

            int exitCode = process.waitFor();

            if (exitCode == 0) {
                return new ProcessResult(system, "SUCCESS", exitCode);
            } else {
                return new ProcessResult(system, "FAILED", exitCode);
            }
        } finally {
            // CRITICAL: Ensure the process is destroyed if the callable is interrupted
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
        }
    }

    // Helper to build the common part of the command
    private ArrayList<String> buildBaseCommand() {
        // Build the command to launch the new JVM
        ArrayList<String> command = new ArrayList<>();

        // Find the java executable for the current JRE
        String javaHome = System.getProperty("java.home");
        String javaBin = javaHome + File.separator + "bin" + File.separator + "java";
        command.add(javaBin);

        // Get JVM arguments from the current process (e.g., -Xmx, -Xms)
        RuntimeMXBean runtimeMxBean = ManagementFactory.getRuntimeMXBean();
        List<String> jvmArgs = runtimeMxBean.getInputArguments();

        // Filter out agent-related or other problematic args if necessary
        // Here, we'll just add them all for a near-perfect clone.
        command.addAll(jvmArgs);

        // Add the classpath from the current process (fat arena jar)
        command.add("-jar");
        command.add("/var/arena/support/arena-1.0.0-SNAPSHOT.jar");

        // add original args
        command.addAll(Arrays.asList(originalArgs));
        // only do the following implementation
        command.add("--impl");
        command.add(system.getId());
        return command;
    }

    public class StreamGobbler implements Runnable {
        private final InputStream inputStream;
        private final Consumer<String> consumer;

        public StreamGobbler(InputStream inputStream, Consumer<String> consumer) {
            this.inputStream = inputStream;
            this.consumer = consumer;
        }

        @Override
        public void run() {
            new BufferedReader(new InputStreamReader(inputStream)).lines()
                    .forEach(consumer);
        }
    }
}
