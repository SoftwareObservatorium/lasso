package de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter;

import de.uni_mannheim.swt.lasso.arena.adaptation.AdaptedImplementation;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.serialize.GsonMapper;

import java.util.List;

/**
 *
 * @author Marcus Kessel
 */
public class TestUtils {

    static GsonMapper gsonMapper = new GsonMapper();

    public static List<Sheet<Integer, Integer, String>> createSheets(AdaptedImplementation adaptedImplementation, ExecutedInvocations executedInvocations) {
        return SheetUtils.toSheets(adaptedImplementation, executedInvocations, gsonMapper);
    }
}
