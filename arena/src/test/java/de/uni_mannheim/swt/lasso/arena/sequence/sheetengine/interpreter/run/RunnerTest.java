package de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.run;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 *
 * @author Marcus Kessel
 */
public class RunnerTest {

    @Test
    public void test() throws Throwable {
        Runner runner = new Runner();
        ExecutionResult<Integer> executionResult = runner.run(() -> 5+5);

        assertEquals(10, executionResult.getValue());

        System.out.println(executionResult.getDurationNanos());
    }
}
