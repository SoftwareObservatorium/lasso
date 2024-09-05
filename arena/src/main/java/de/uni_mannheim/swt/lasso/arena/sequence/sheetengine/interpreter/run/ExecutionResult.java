package de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.run;

/**
 *
 * @author Marcus Kessel
 */
public class ExecutionResult<T> {

    private T value;
    private Throwable exceptionThrown;
    private long durationNanos;

    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        this.value = value;
    }

    public Throwable getExceptionThrown() {
        return exceptionThrown;
    }

    public void setExceptionThrown(Throwable exceptionThrown) {
        this.exceptionThrown = exceptionThrown;
    }

    public long getDurationNanos() {
        return durationNanos;
    }

    public void setDurationNanos(long durationNanos) {
        this.durationNanos = durationNanos;
    }
}
