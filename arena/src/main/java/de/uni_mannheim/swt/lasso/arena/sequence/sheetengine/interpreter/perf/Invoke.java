package de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.perf;

/**
 * Run an invocation
 *
 * @param <T>
 */
public interface Invoke<T> {

    T run() throws Throwable;
}
