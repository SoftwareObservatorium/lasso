package de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.eval;

/**
 * Wraps an interpreter like Beanshell or Groovy.
 *
 * @author Marcus Kessel
 */
public interface Eval {

    Object eval(String expression) throws EvalException;

    void setClassLoader(ClassLoader classLoader);

    Class resolveClass(String className) throws ClassNotFoundException;
}
