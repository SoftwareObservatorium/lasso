package de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.event;

import de.uni_mannheim.swt.lasso.arena.adaptation.AdaptedImplementation;
import de.uni_mannheim.swt.lasso.arena.classloader.coverage.jacoco.JaCoCoContainer;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.InvocationVisitor;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.Sheet;
import de.uni_mannheim.swt.lasso.core.dto.srm.StimulusResponseMatrix;

import org.jacoco.core.analysis.CoverageBuilder;
import org.jacoco.core.analysis.IClassCoverage;
import org.jacoco.core.analysis.ICounter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 *
 * @author Marcus Kessel
 */
public class JaCoCoListener extends InvocationVisitor {

    private static final Logger LOG = LoggerFactory
            .getLogger(JaCoCoListener.class);

    // XXX should be really the test suite: List<Test>
    private StimulusResponseMatrix<String, AdaptedImplementation, Sheet> stimulusResponseMatrix = new StimulusResponseMatrix<>();

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

            // FIXME
            Optional<IClassCoverage> cutClassOp = coverageBuilder.getClasses().stream().findFirst();
            if(cutClassOp.isPresent()) {
                IClassCoverage cutClass = cutClassOp.get();
                stimulusResponseMatrix.put("jacoco_complexity", adaptedImplementation, createMetricSheet(cutClass.getComplexityCounter()));
                stimulusResponseMatrix.put("jacoco_branch", adaptedImplementation, createMetricSheet(cutClass.getBranchCounter()));
                stimulusResponseMatrix.put("jacoco_instruction", adaptedImplementation, createMetricSheet(cutClass.getInstructionCounter()));
                stimulusResponseMatrix.put("jacoco_line", adaptedImplementation, createMetricSheet(cutClass.getLineCounter()));
                stimulusResponseMatrix.put("jacoco_method", adaptedImplementation, createMetricSheet(cutClass.getMethodCounter()));

//            for (int i = cutClass.getFirstLine(); i <= cutClass.getLastLine(); i++) {
//                System.out.printf("Line %s: %s%n", Integer.valueOf(i),
//                        getColor(cutClass.getLine(i).getStatus()));
//            }
            }

        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

//    // copied from jacoco examples
//    private String getColor(final int status) {
//        switch (status) {
//            case ICounter.NOT_COVERED:
//                return "red";
//            case ICounter.PARTLY_COVERED:
//                return "yellow";
//            case ICounter.FULLY_COVERED:
//                return "green";
//        }
//        return "";
//    }

    private Sheet<Integer, Integer, Double> createMetricSheet(ICounter counter) {
        Sheet<Integer, Integer, Double> metricSheet = new Sheet<>();
        metricSheet.put(0,0, (double) counter.getMissedCount());
        metricSheet.put(0,1, (double) counter.getTotalCount());
        metricSheet.put(0,2, (double) counter.getCoveredRatio());

        return metricSheet;
    }

    public StimulusResponseMatrix<String, AdaptedImplementation, Sheet> getStimulusResponseMatrix() {
        return stimulusResponseMatrix;
    }
}
