package de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter;

import bsh.Interpreter;
import de.uni_mannheim.swt.lasso.arena.search.InterfaceSpecification;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.resolve.ParsedSheet;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
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
}
