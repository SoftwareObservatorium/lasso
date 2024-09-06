package de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.serialize;

import de.uni_mannheim.swt.lasso.arena.adaptation.AdaptedImplementation;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.ExecutedInvocation;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.Obj;

import java.io.IOException;

/**
 * Interface for mapping values.
 *
 * @author Marcus Kessel
 */
public interface ObjectMapper {

    String writeOutput(ExecutedInvocation executedInvocation) throws IOException;
    String writeInput(ExecutedInvocation executedInvocation, int p) throws IOException;
    String writeTarget(ExecutedInvocation executedInvocation) throws IOException;
    String writeOp(ExecutedInvocation executedInvocation) throws IOException;
    String writeAdaptedOp(ExecutedInvocation executedInvocation, AdaptedImplementation adaptedImplementation) throws IOException;

    Obj readOutput(String value) throws IOException;
    Obj readInput(String value) throws IOException;
    Obj readOp(String value) throws IOException;
}
