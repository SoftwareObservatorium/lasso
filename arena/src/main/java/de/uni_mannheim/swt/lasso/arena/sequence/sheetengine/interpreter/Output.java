package de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter;

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

    public void setValue(Object value) {
        this.value = value;
    }

    public Throwable getException() {
        return exception;
    }

    public void setException(Throwable exception) {
        this.exception = exception;
    }
}
