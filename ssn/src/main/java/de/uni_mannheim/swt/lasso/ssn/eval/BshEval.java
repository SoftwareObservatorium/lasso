package de.uni_mannheim.swt.lasso.ssn.eval;

import bsh.EvalError;
import bsh.Interpreter;
import bsh.UtilEvalError;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * {@link Eval} based on BeanShell.
 *
 * @author Marcus Kessel
 */
public class BshEval implements Eval {

    private static final Logger LOG = LoggerFactory
            .getLogger(BshEval.class);

    // Regular expression pattern to match generic types (e.g. <Type> or <? extends Type>)
    private final Pattern GENERIC_PATTERN = Pattern.compile("\\s*(<[^<>]*>|<?\\s*extends\\s*[^<>]+>|<?\\s*super\\s*[^<>]+>)");

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
        // strip generics if bsh version < 3.0
        // FIXME better check if <> and then only do strip?
        expression = removeGenerics(expression);

        try {
            LOG.debug("eval expression '{}'", expression);
            return bsh.eval(expression);
        } catch (EvalError e) {
            throw new EvalException(e);
        }
    }

    public String removeGenerics(String javaSource) {
        Matcher matcher = GENERIC_PATTERN.matcher(javaSource);
        StringBuilder stringBuffer = new StringBuilder();
        while (matcher.find()) {
            matcher.appendReplacement(stringBuffer, "");
        }
        matcher.appendTail(stringBuffer);

        String clean = stringBuffer.toString();
        // nested generics?
        if(StringUtils.contains(clean, "<")) {
            return removeGenerics(clean);
        }

        return clean;
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
