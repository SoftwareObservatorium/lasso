/*
 * LASSO - an Observatorium for the Dynamic Selection, Analysis and Comparison of Software
 * Copyright (C) 2024 Marcus Kessel (University of Mannheim) and LASSO contributers
 *
 * This file is part of LASSO.
 *
 * LASSO is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LASSO is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LASSO.  If not, see <https://www.gnu.org/licenses/>.
 */
package de.uni_mannheim.swt.lasso.arena.classloader.coverage.jacoco;

import de.uni_mannheim.swt.lasso.arena.ArenaUtils;
import de.uni_mannheim.swt.lasso.arena.Observation;
import de.uni_mannheim.swt.lasso.arena.sequence.SequenceExecutionRecord;
import de.uni_mannheim.swt.lasso.arena.sequence.SequenceExecutionRecords;
import de.uni_mannheim.swt.lasso.srm.CellId;
import de.uni_mannheim.swt.lasso.srm.CellValue;
import org.apache.commons.lang3.StringUtils;
import org.jacoco.core.analysis.CoverageBuilder;
import org.jacoco.core.analysis.IClassCoverage;
import org.jacoco.core.analysis.ICounter;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * An observation for JaCoCo measurements.
 *
 * @author Marcus Kessel
 */
public class JaCoCoObservation extends Observation {

    private CoverageBuilder coverageBuilder;

    public JaCoCoObservation(CoverageBuilder coverageBuilder) {
        this.coverageBuilder = coverageBuilder;
    }

    @Override
    public String getType() {
        return null;
    }

    @Override
    public Map<CellId, CellValue> toCells(SequenceExecutionRecord record) {
//        String className = StringUtils.replace(executionResult.getImplementation().getAdaptee().getClassName(),
//                ".",
//                "/");
//
//        Optional<IClassCoverage> classCoverage = coverageBuilder.getClasses().stream()
//                .filter(clazz -> StringUtils.equals(className, clazz.getName()))
//                .findFirst();
//
//        if (!classCoverage.isPresent()) {
//            return Collections.emptyMap();
//        }
//
//        // FIXME do it for each scope
//        Map<CellId, CellValue> cells = new LinkedHashMap<>();
//        putClassScope(executionResult, "instruction", cells, classCoverage.get().getInstructionCounter());
//        putClassScope(executionResult, "line", cells, classCoverage.get().getLineCounter());
//        putClassScope(executionResult, "branch", cells, classCoverage.get().getBranchCounter());
//        putClassScope(executionResult, "complexity", cells, classCoverage.get().getComplexityCounter());
//        putClassScope(executionResult, "method", cells, classCoverage.get().getMethodCounter());

        return new LinkedHashMap<>();
    }

    private void putClassScope(SequenceExecutionRecords records, String metric, Map<CellId, CellValue> cells, ICounter counter) {
        cells.put(ArenaUtils.cellIdOf("all",
                -1,
                -1,
                String.format("jacoco_class_%s.total", metric),
                records.getImplementation()),
                CellValue.of(counter.getTotalCount()));
        cells.put(ArenaUtils.cellIdOf("all",
                        -1,
                        -1,
                        String.format("jacoco_class_%s.covered", metric),
                        records.getImplementation()),
                CellValue.of(counter.getCoveredCount()));
        cells.put(ArenaUtils.cellIdOf("all",
                        -1,
                        -1,
                        String.format("jacoco_class_%s.missed", metric),
                        records.getImplementation()),
                CellValue.of(counter.getMissedCount()));
        cells.put(ArenaUtils.cellIdOf("all",
                        -1,
                        -1,
                        String.format("jacoco_class_%s.coveredratio", metric),
                        records.getImplementation()),
                CellValue.of(nanToZero(counter.getCoveredRatio())));
        cells.put(ArenaUtils.cellIdOf("all",
                        -1,
                        -1,
                        String.format("jacoco_class_%s.missedratio", metric),
                        records.getImplementation()),
                CellValue.of(nanToZero(counter.getMissedRatio())));
    }

    /**
     * JaCoCo sometimes reports NaN in its ratio.
     *
     * @param val
     * @return
     */
    private double nanToZero(double val) {
        if(Double.isNaN(val)) {
            return 0d;
        } else {
            return val;
        }
    }

    @Override
    public Map<CellId, CellValue> toCells(SequenceExecutionRecords records) {
        String className = StringUtils.replace(records.getImplementation().getAdaptee().getClassName(),
                ".",
                "/");

        // only do for CUT for now
        Optional<IClassCoverage> classCoverage = coverageBuilder.getClasses().stream()
                .filter(clazz -> StringUtils.equals(className, clazz.getName()))
                .findFirst();

        if (!classCoverage.isPresent()) {
            return Collections.emptyMap();
        }

        classCoverage.get().getLine(0).getStatus();

        // FIXME do it for each scope
        Map<CellId, CellValue> cells = new LinkedHashMap<>();
        putClassScope(records, "instruction", cells, classCoverage.get().getInstructionCounter());
        putClassScope(records, "line", cells, classCoverage.get().getLineCounter());
        putClassScope(records, "branch", cells, classCoverage.get().getBranchCounter());
        putClassScope(records, "complexity", cells, classCoverage.get().getComplexityCounter());
        putClassScope(records, "method", cells, classCoverage.get().getMethodCounter());
        putClassScope(records, "class", cells, classCoverage.get().getClassCounter());

        return cells;
    }
}
