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
package de.uni_mannheim.swt.lasso.arena.classloader.statistics;

import de.uni_mannheim.swt.lasso.arena.ArenaUtils;
import de.uni_mannheim.swt.lasso.arena.Observation;
import de.uni_mannheim.swt.lasso.arena.classloader.Container;
import de.uni_mannheim.swt.lasso.arena.sequence.SequenceExecutionRecord;
import de.uni_mannheim.swt.lasso.arena.sequence.SequenceExecutionRecords;
import de.uni_mannheim.swt.lasso.srm.CellId;
import de.uni_mannheim.swt.lasso.srm.CellValue;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Keeps statistics of artifacts and classes used by containers.
 *
 * @author Marcus Kessel
 */
public class LoaderObservation extends Observation {

    private final Container container;

    public LoaderObservation(Container container) {
        this.container = container;
    }

    @Override
    public String getType() {
        return null;
    }

    @Override
    public Map<CellId, CellValue> toCells(SequenceExecutionRecord record) {
//        Map<CellId, CellValue> cells = new LinkedHashMap<>();
//        cells.put(ArenaUtils.cellIdOf(String.valueOf(executionResult.getSheet().getName()),
//                        -1,
//                        -1,
//                        String.format("loader.classes_loaded"),
//                        executionResult.getImplementation()),
//                CellValue.of(container.getClasses().size()));
//        cells.put(ArenaUtils.cellIdOf(String.valueOf(executionResult.getSheet().getName()),
//                        -1,
//                        -1,
//                        String.format("loader.artifacts"),
//                        executionResult.getImplementation()),
//                CellValue.of(container.getImportRealms().size()));

        return new LinkedHashMap<>();
    }

    @Override
    public Map<CellId, CellValue> toCells(SequenceExecutionRecords records) {
        Map<CellId, CellValue> cells = new LinkedHashMap<>();
        cells.put(ArenaUtils.cellIdOf("all",
                        -1,
                        -1,
                        String.format("loader.classes_loaded"),
                        records.getImplementation()),
                CellValue.of(container.getClasses().size()));
        cells.put(ArenaUtils.cellIdOf("all",
                        -1,
                        -1,
                        String.format("loader.artifacts"),
                        records.getImplementation()),
                CellValue.of(container.getURLs().length));

        return cells;
    }
}
