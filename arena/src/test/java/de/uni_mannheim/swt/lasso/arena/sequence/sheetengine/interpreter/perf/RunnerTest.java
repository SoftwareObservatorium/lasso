package de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.perf;

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
        int actual = runner.run(() -> 5+5);

        assertEquals(10, actual);

        StopWatch stopWatch = runner.getStopWatch();
        System.out.println(stopWatch.getExecutionNanoTime());
    }
}
