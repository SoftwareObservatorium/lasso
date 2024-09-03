package de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter;

/**
 * An executed {@link Invocation} that records the output.
 *
 * @author Marcus Kessel
 */
public class ExecutedInvocation {

    private final Invocation invocation;
    private Output output = null;

    public ExecutedInvocation(Invocation invocation) {
        this.invocation = invocation;
    }

    public Invocation getInvocation() {
        return invocation;
    }

    public Output getOutput() {
        return output;
    }

    public void setOutput(Output output) {
        this.output = output;
    }

    /**
     * Return code statement for this executed invocation
     *
     * @return
     */
    public String toCode() {
        // FIXME implement
        return "TODO";
    }

    @Override
    public String toString() {
        return "ExecutedInvocation{" +
                "invocation=" + invocation +
                ", output=" + output +
                '}';
    }
}
