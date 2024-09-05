package de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.run;

/**
 * Run an invocation
 *
 * @param <T>
 *
 * @author Marcus Kessel
 */
public interface Invoke<T> {

    T run() throws Throwable;
}
