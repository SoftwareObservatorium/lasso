package de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.run;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeoutException;

/**
 * A simple runner with a stop watch.
 *
 * @author Marcus Kessel
 */
public class Runner {

    private static final Logger LOG = LoggerFactory
            .getLogger(Runner.class);

    public static int CALL_TIMEOUT_MILLIS = 5000;

    private int timeoutInMillis = CALL_TIMEOUT_MILLIS;

    private StopWatch stopWatch = new StopWatch();

    public boolean isTimeoutEnabled() {
        return timeoutInMillis > 0;
    }

    /**
     * Run something inside {@link Invoke} and keep track of time.
     *
     * @param invoke
     * @return
     * @throws Throwable
     */
    public <T> ExecutionResult<T> run(Invoke<T> invoke) throws Throwable {
        ExecutionResult<T> executionResult;

        if(isTimeoutEnabled()) {
            LOG.debug("Running with thread");
            try {
                executionResult = runWithTimeout(invoke);
            } catch (TimeoutException e) {
                // TODO how to handle timeouts
                throw new RuntimeException(e);
            }
        } else {
            LOG.debug("Running directly");
            executionResult = runDirectly(invoke);
        }

        return executionResult;
    }

    protected <T> ExecutionResult<T> runDirectly(Invoke<T> invoke) {
        ExecutionResult<T> executionResult = new ExecutionResult();

        try {
            stopWatch.start();

            T value = invoke.run();
            executionResult.setValue(value);
        } catch (Throwable e) {
            executionResult.setExceptionThrown(e);
        } finally {
            stopWatch.stop();
            executionResult.setDurationNanos(stopWatch.getExecutionNanoTime());
        }

        return executionResult;
    }

    protected <T> ExecutionResult<T> runWithTimeout(Invoke<T> invoke) throws TimeoutException {
        InvokeThread<T> invokeThread = new InvokeThread(null, invoke, stopWatch);

        try {
            // start execution
            invokeThread.start();

            // wait for millis
            invokeThread.join(timeoutInMillis);

            if (!invokeThread.isFinished()) {
                LOG.warn("Timeout");

                // try to stop
                invokeThread.stop();

                throw new TimeoutException();
            }

        } catch (java.lang.InterruptedException e) {
            throw new IllegalStateException();
        }

        return invokeThread.getExecutionResult();
    }

    public StopWatch getStopWatch() {
        return stopWatch;
    }

    public int getTimeoutInMillis() {
        return timeoutInMillis;
    }
}
