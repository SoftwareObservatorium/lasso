package de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.perf;

/**
 * A simple runner with a stop watch.
 *
 * @author Marcus Kessel
 */
public class Runner {

    private StopWatch stopWatch = new StopWatch();

    /**
     * Run something inside {@link Invoke} and keep track of time.
     *
     * @param invoke
     * @return
     * @throws Throwable
     */
    // FIXME timeout handling?
    public <T> T run(Invoke<T> invoke) throws Throwable {
        try {
            stopWatch.start();

            return invoke.run();
        } catch (Throwable e) {
            throw e;
        } finally {
            stopWatch.stop();
        }
    }

    public StopWatch getStopWatch() {
        return stopWatch;
    }
}
