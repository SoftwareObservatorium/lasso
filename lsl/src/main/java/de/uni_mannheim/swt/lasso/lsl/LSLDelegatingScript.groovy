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
package de.uni_mannheim.swt.lasso.lsl


import de.uni_mannheim.swt.lasso.lsl.spec.StudySpec

/**
 *
 * @author Marcus Kessel
 */
class LSLDelegatingScript {

    LSLLogger logger

    LassoContext lasso

    /**
     * Init LSLLogger
     *
     * @param logger
     */
    void init(LSLLogger logger) {
        this.logger = logger

        lasso = new LassoContext()
        lasso.logger = logger
    }

    void dataSource(String dataSourceId) {
        lasso.registerDataSource(dataSourceId)
    }

    void study(Map<String, ?> map, Closure<StudySpec> closure) {
        StudySpec studySpec = new StudySpec(map: map, closure: closure)

        lasso.registerStudy(studySpec)
    }

    /**
     * Returns a benchmark model to work with as part of LSL.
     *
     * @see module 'benchmarks'
     *
     * @param benchmarkId
     * @return
     */
    Object loadBenchmark(String benchmarkId) {
        return lasso.executionContext.benchmarkManager.load(benchmarkId)
    }

    void log(String msg) {
        logger.log(msg)
    }
}
