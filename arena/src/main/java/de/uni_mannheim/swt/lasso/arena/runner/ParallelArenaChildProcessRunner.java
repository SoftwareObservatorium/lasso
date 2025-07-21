package de.uni_mannheim.swt.lasso.arena.runner;

import de.uni_mannheim.swt.lasso.cluster.client.ArenaJob;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * Arena runner that spawns a new Java process for each implementation. Runs Java processes in parallel if #threads > 1
 * <p>
 * This is needed in cases where implementations get stuck and Java's thread doesn't allow us to kill threads forcibly, however we can kill processes forcibly.
 *
 * @author Marcus Kessel
 */
public class ParallelArenaChildProcessRunner {

    private static final org.slf4j.Logger LOG = LoggerFactory
            .getLogger(ParallelArenaChildProcessRunner.class);

    private int timeoutInSecs = 30;

    private int threads = 1;

    public void run(ArenaJob arenaJob, String[] originalArgs) {
        LOG.info("Starting external processes with {} threads and a timeout of {} seconds", threads, timeoutInSecs);

        // Create a thread pool with a fixed number of threads
        ExecutorService executor = Executors.newFixedThreadPool(threads);

        // A list to hold the Future object for each task
        List<Future<ProcessResult>> futureList = new ArrayList<>();
        // create processes
        for (de.uni_mannheim.swt.lasso.core.model.System system : arenaJob.getImplementations()) {
            futureList.add(executor.submit(new ProcessTask(system, originalArgs)));
        }

        try {
            // Now, collect the results
            for (Future<ProcessResult> future : futureList) {
                try {
                    // future.get() is the blocking call. We add a timeout here.
                    ProcessResult result = future.get(timeoutInSecs, TimeUnit.SECONDS);
                    LOG.info("Main: Received result -> " + result);

                } catch (TimeoutException e) {
                    // This is the key for handling stuck processes.
                    // The task timed out, so we cancel it. Setting to
                    // true will interrupt the thread running the Callable. Our Callable's
                    // finally block will then kill the underlying process.
                    LOG.warn("Main: A task timed out! Cancelling it.");
                    future.cancel(true); // VERY IMPORTANT

                } catch (ExecutionException e) {
                    // This happens if the call() method itself threw an exception.
                    LOG.warn("Main: A task failed with an exception: ", e);

                } catch (InterruptedException e) {
                    // This happens if the main thread is interrupted while waiting.
                    LOG.warn("Main: The main thread was interrupted.");
                    Thread.currentThread().interrupt();
                } catch (Throwable e) {
                    LOG.warn("Unknown error", e);
                }
            }
        } finally {
            // Cleanly shut down the executor service
            LOG.info("All tasks processed. Shutting down executor.");
            executor.shutdown();
            try {
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    LOG.warn("Executor did not terminate in time. Forcing shutdown...");
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
            LOG.info("Shutdown complete.");
        }
    }

    public int getTimeoutInSecs() {
        return timeoutInSecs;
    }

    public void setTimeoutInSecs(int timeoutInSecs) {
        this.timeoutInSecs = timeoutInSecs;
    }

    public int getThreads() {
        return threads;
    }

    public void setThreads(int threads) {
        this.threads = threads;
    }
}
