package de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.eval;

import bsh.EvalError;
import bsh.Interpreter;
import bsh.UtilEvalError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link Eval} based on BeanShell.
 *
 * @author Marcus Kessel
 */
public class BshEval implements Eval {

    private static final Logger LOG = LoggerFactory
            .getLogger(BshEval.class);

    private final Interpreter bsh;

    public BshEval(Interpreter bsh) {
        this.bsh = bsh;
    }

    public BshEval() {
        this(new Interpreter());
    }

    // FIXME timeout handling?
    @Override
    public Object eval(String expression) throws EvalException {
        try {
            LOG.debug("eval expression '{}'", expression);
            return bsh.eval(expression);
        } catch (EvalError e) {
            throw new EvalException(e);
        }
    }

    @Override
    public void setClassLoader(ClassLoader classLoader) {
        this.bsh.setClassLoader(classLoader);
    }

    @Override
    public Class resolveClass(String className) throws ClassNotFoundException {
        // resolve class
        try {
            return bsh.getNameSpace().getClass(className);
        } catch (UtilEvalError e) {
            throw new ClassNotFoundException("could not resolve", e);
        }
    }
}
