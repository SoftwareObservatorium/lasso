package de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.invocation;


import de.uni_mannheim.swt.lasso.ssn.eval.BshEval;
import de.uni_mannheim.swt.lasso.ssn.eval.EvalException;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

/**
 *
 *
 * @author Marcus Kessel
 */
public class CodeInvocationTest {

    @Test
    public void test_codexpr() throws EvalException {
        Object[] obj = (Object[]) CodeInvocation.evalCode(new BshEval(), "new Object[]{1,2,3}");

        System.out.println(Arrays.toString(obj));
    }
}
