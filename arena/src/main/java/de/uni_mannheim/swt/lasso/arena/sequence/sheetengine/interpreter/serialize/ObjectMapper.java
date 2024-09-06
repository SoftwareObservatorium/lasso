package de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.serialize;

import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.ExecutedInvocation;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.ExecutedInvocations;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.Output;

import java.io.IOException;

/**
 * Interface for mapping values.
 *
 * @author Marcus Kessel
 */
public interface ObjectMapper {

    String writeValue(ExecutedInvocation executedInvocation) throws IOException;

    Output readValue(String value) throws IOException;
}
