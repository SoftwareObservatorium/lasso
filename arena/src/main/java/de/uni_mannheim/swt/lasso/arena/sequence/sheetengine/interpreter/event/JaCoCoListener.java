package de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.event;

import de.uni_mannheim.swt.lasso.arena.adaptation.AdaptedImplementation;
import de.uni_mannheim.swt.lasso.arena.classloader.coverage.jacoco.JaCoCoContainer;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.InvocationVisitor;
import org.jacoco.core.analysis.CoverageBuilder;
import org.jacoco.core.analysis.IClassCoverage;
import org.jacoco.core.analysis.ICounter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Marcus Kessel
 */
public class JaCoCoListener extends InvocationVisitor {

    private static final Logger LOG = LoggerFactory
            .getLogger(JaCoCoListener.class);

    @Override
    public void visitBeforeExecution(AdaptedImplementation adaptedImplementation) {
        if(LOG.isInfoEnabled()) {
            LOG.info("Starting JaCoCo measurement");
        }

        JaCoCoContainer jaCoCoContainer = (JaCoCoContainer) adaptedImplementation.getAdaptee().getProject().getContainer();
        // FIXME
        try {
            jaCoCoContainer.start();
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    @Override
    public void visitAfterExecution(AdaptedImplementation adaptedImplementation) {
        if(LOG.isInfoEnabled()) {
            LOG.info("Stopping JaCoCo measurement");
        }

        JaCoCoContainer jaCoCoContainer = (JaCoCoContainer) adaptedImplementation.getAdaptee().getProject().getContainer();

        try {
            CoverageBuilder coverageBuilder = jaCoCoContainer.stop();

            //
            // Let's dump some metrics and line coverage information:
            for (final IClassCoverage cc : coverageBuilder.getClasses()) {
                System.out.printf("Coverage of class %s%n", cc.getName());

                printCounter("instructions", cc.getInstructionCounter());
                printCounter("branches", cc.getBranchCounter());
                printCounter("lines", cc.getLineCounter());
                printCounter("methods", cc.getMethodCounter());
                printCounter("complexity", cc.getComplexityCounter());

                for (int i = cc.getFirstLine(); i <= cc.getLastLine(); i++) {
                    System.out.printf("Line %s: %s%n", Integer.valueOf(i),
                            getColor(cc.getLine(i).getStatus()));
                }
            }

        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private String getColor(final int status) {
        switch (status) {
            case ICounter.NOT_COVERED:
                return "red";
            case ICounter.PARTLY_COVERED:
                return "yellow";
            case ICounter.FULLY_COVERED:
                return "green";
        }
        return "";
    }

    private void printCounter(final String unit, final ICounter counter) {
        final Integer missed = Integer.valueOf(counter.getMissedCount());
        final Integer total = Integer.valueOf(counter.getTotalCount());
        System.out.printf("%s of %s %s missed%n", missed, total, unit);
    }
}
