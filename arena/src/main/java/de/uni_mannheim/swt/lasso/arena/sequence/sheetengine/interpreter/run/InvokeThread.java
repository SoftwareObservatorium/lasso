package de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.run;

/**
 * Run {@link Invoke} in a thread that can be controlled.
 *
 * @author Marcus Kessel
 */
public class InvokeThread<T> extends Thread {

    public static final String SHEETENGINE_RUNNER = "sheetengine-runner";

    private final Invoke<T> invoke;
    private final StopWatch stopWatch;

    private boolean finished = false;

    private ExecutionResult<T> executionResult = new ExecutionResult();

    public InvokeThread(ThreadGroup threadGroup, Invoke<T> invoke, StopWatch stopWatch) {
        super(threadGroup, SHEETENGINE_RUNNER);
        this.invoke = invoke;
        this.stopWatch = stopWatch;
    }

    @Override
    public void run() {
        try {
            stopWatch.start();

            T value = invoke.run();
            executionResult.setValue(value);
        } catch (Throwable e) {
            executionResult.setExceptionThrown(e);
        } finally {
            stopWatch.stop();
            executionResult.setDurationNanos(stopWatch.getExecutionNanoTime());

            // finished
            finished = true;
        }
    }

    public boolean isFinished() {
        return finished;
    }

    public ExecutionResult<T> getExecutionResult() {
        return executionResult;
    }

}
