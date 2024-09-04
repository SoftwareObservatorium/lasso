package de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.perf;

/**
 * A simple stop watch.
 *
 * @author Marcus Kessel
 */
public class StopWatch {

    private long startTime;
    private long stopTime;

    public void start() {
        startTime = System.nanoTime();
    }

    public void stop() {
        stopTime = System.nanoTime();
    }

    public long getExecutionNanoTime() {
        return (stopTime - startTime);
    }
}
