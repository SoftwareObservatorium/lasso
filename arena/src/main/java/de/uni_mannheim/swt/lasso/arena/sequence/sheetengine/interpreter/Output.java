package de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter;

import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.adapter.InvocationInterceptor;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.util.CutUtils;

/**
 * Represents the outcome (i.e., result) of an execution of an {@link Invocation}.
 *
 * @author Marcus Kessel
 */
public class Output {

    private Object value;

    private Throwable exception;

    public static Output fromValue(Object value) {
        Output out = new Output();
        out.setValue(value);

        return out;
    }

    public static Output fromException(Throwable t) {
        Output out = new Output();
        out.setException(t);

        return out;
    }

    public Object getValue() {
        return value;
    }

    public boolean isNull() {
        return value == null;
    }

    public Class getType() {
        if(isNull()) {
            return null;
        } else {
            return value.getClass();
        }
    }

    public String getTypeAsName() {
        if(isNull()) {
            return "";
        } else {
            if(isCutProxyReference()) {
                return InvocationInterceptor.getCutClassNameFromProxy(value);
            } else {
                return value.getClass().getCanonicalName();
            }
        }
    }

    public boolean isCutProxyReference() {
        if(isNull()) {
            return false;
        }

        return InvocationInterceptor.isProxy(value);
    }

    public boolean hasException() {
        return exception != null;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public Throwable getException() {
        return exception;
    }

    public void setException(Throwable exception) {
        this.exception = exception;
    }

    @Override
    public String toString() {
        return "Output{" +
                "value=" + value +
                ", exception=" + exception +
                '}';
    }
}
