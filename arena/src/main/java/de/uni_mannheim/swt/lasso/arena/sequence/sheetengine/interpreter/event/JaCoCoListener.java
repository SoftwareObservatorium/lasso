package de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.event;

import de.uni_mannheim.swt.lasso.arena.adaptation.AdaptedImplementation;
import de.uni_mannheim.swt.lasso.arena.classloader.coverage.jacoco.JaCoCoContainer;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.InvocationVisitor;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.Sheet;
import de.uni_mannheim.swt.lasso.core.dto.srm.StimulusResponseMatrix;

import de.uni_mannheim.swt.lasso.core.model.Scope;
import org.apache.commons.lang3.StringUtils;
import org.jacoco.core.analysis.CoverageBuilder;
import org.jacoco.core.analysis.IClassCoverage;
import org.jacoco.core.analysis.ICounter;
import org.jacoco.core.analysis.ICoverageNode;
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

    private StimulusResponseMatrix<String, AdaptedImplementation, Sheet<Integer, Integer, Object>> stimulusResponseMatrix = new StimulusResponseMatrix<>();

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

            // determine scope
            // FIXME apply here?
            Scope scope = jaCoCoContainer.getScope();

            // identify class under test
            String byteCodeClassNotation = StringUtils.replaceChars(adaptedImplementation.getAdaptee().getClassName(), '.', '/');
            Optional<IClassCoverage> cutClassOp = coverageBuilder.getClasses().stream().filter(s -> StringUtils.equals(byteCodeClassNotation, s.getName())).findFirst();

//            for(IClassCoverage classCoverage : coverageBuilder.getClasses()) {
//                LOG.debug("JACOCO CLASS {} vs {}, {}", adaptedImplementation.getAdaptee().getClassName(), classCoverage.getName(), classCoverage.getComplexityCounter().getCoveredRatio());
//            }

            if(cutClassOp.isPresent()) {
                IClassCoverage cutClass = cutClassOp.get();
                LOG.debug("Identified CUT class for JaCoCo measurements '{}'", cutClass.getName());

                stimulusResponseMatrix.put("JACOCO", adaptedImplementation, createMetricSheet(cutClass));

//            for (int i = cutClass.getFirstLine(); i <= cutClass.getLastLine(); i++) {
//                System.out.printf("Line %s: %s%n", Integer.valueOf(i),
//                        getColor(cutClass.getLine(i).getStatus()));
//            }
            }

        } catch (Throwable e) {
            LOG.warn("code coverage failed", e);
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

    private Sheet<Integer, Integer, Object> createMetricSheet(IClassCoverage cutClass) {
        Sheet<Integer, Integer, Object> metricSheet = new Sheet<>();
        int r = 0;
        for(ICoverageNode.CounterEntity counter : ICoverageNode.CounterEntity.values()) {
            for(ICounter.CounterValue counterValue : ICounter.CounterValue.values()) {
                metricSheet.put(r,0, cutClass.getCounter(counter).getValue(counterValue));
                // add name (same column as op)
                metricSheet.put(r,1, counter.toString() + "_" + counterValue.toString());

                r++;
            }
        }

        return metricSheet;
    }

    public StimulusResponseMatrix<String, AdaptedImplementation, Sheet<Integer, Integer, Object>> getStimulusResponseMatrix() {
        return stimulusResponseMatrix;
    }
}
