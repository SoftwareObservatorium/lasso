package de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter;

import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.adapter.InvocationInterceptor;

/**
 * Represents an object (i.e., result) of an execution of an {@link Invocation} (e.g., output, inputs).
 *
 * @author Marcus Kessel
 */
public class Obj {

    public static int PRODUCER_INDEX_NONE = -1;

    private Object value;
    private Throwable exception;

    // row index of producer
    private int producerIndex = PRODUCER_INDEX_NONE;

    public static Obj fromValue(Object value, int producerIndex) {
        Obj out = new Obj();
        out.setValue(value);
        out.setProducerIndex(producerIndex);

        return out;
    }

    public static Obj fromException(Throwable t, int producerIndex) {
        Obj out = new Obj();
        out.setException(t);
        out.setProducerIndex(producerIndex);

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

    public int getProducerIndex() {
        return producerIndex;
    }

    public void setProducerIndex(int producerIndex) {
        this.producerIndex = producerIndex;
    }
}
