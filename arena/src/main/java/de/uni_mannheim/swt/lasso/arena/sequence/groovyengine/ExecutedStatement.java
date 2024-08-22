package de.uni_mannheim.swt.lasso.arena.sequence.groovyengine;

import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author Marcus Kessel
 */
public class ExecutedStatement {

    private CallableStatement callableStatement;

    private List<Object> inputs = new LinkedList<>();

    private List<Object> serializedInputs = new LinkedList<>();
    private Object output;
    private Object serializedOutput;

    public ExecutedStatement(CallableStatement callableStatement) {
        this.callableStatement = callableStatement;
    }

    public Object getOutput() {
        return output;
    }

    public void setOutput(Object output) {
        this.output = output;

        this.serializedOutput = ObjectSerializer.serialize(output);
    }

    public int getIndex() {
        return callableStatement.getIndex();
    }

    public List<Object> getInputs() {
        return inputs;
    }

    public void addInput(Object input) {
        this.inputs.add(input);
        this.serializedInputs.add(ObjectSerializer.serialize(input));
    }

    public CallableStatement getCallableStatement() {
        return callableStatement;
    }

    public Object getSerializedOutput() {
        return serializedOutput;
    }

    public List<Object> getSerializedInputs() {
        return serializedInputs;
    }
}
