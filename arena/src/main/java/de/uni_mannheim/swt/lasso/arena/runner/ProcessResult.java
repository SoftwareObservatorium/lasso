package de.uni_mannheim.swt.lasso.arena.runner;

/**
 * A simple DTO to hold the outcome of a single process execution.
 *
 * @author Marcus Kessel
 */
public class ProcessResult {
    private final de.uni_mannheim.swt.lasso.core.model.System system;
    private final String outcome;
    private final int exitCode;

    public ProcessResult(de.uni_mannheim.swt.lasso.core.model.System system, String outcome, int exitCode) {
        this.system = system;
        this.outcome = outcome;
        this.exitCode = exitCode;
    }

    @Override
    public String toString() {
        return String.format("Implementation '%s' finished with outcome: %s (Exit Code: %d)", system.getId(), outcome, exitCode);
    }
}
