package de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.eval;

/**
 * FIXME implement groovy evaluation.
 *
 * @author Marcus Kessel
 */
public class GroovyEval implements Eval {

    @Override
    public Object eval(String expression) throws EvalException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setClassLoader(ClassLoader classLoader) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Class resolveClass(String className) throws ClassNotFoundException {
        throw new UnsupportedOperationException();
    }
}
