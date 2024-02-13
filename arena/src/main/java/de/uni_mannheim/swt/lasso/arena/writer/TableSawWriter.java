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
package de.uni_mannheim.swt.lasso.arena.writer;

import de.uni_mannheim.swt.lasso.arena.ClassUnderTest;
import de.uni_mannheim.swt.lasso.arena.DefaultArena;

import de.uni_mannheim.swt.lasso.arena.Observation;
import de.uni_mannheim.swt.lasso.arena.adaptation.AdaptedImplementation;
import de.uni_mannheim.swt.lasso.arena.search.InterfaceSpecification;
import de.uni_mannheim.swt.lasso.arena.sequence.SequenceExecutionRecord;
import de.uni_mannheim.swt.lasso.arena.sequence.SequenceExecutionRecords;
import de.uni_mannheim.swt.lasso.srm.CellId;
import de.uni_mannheim.swt.lasso.srm.CellValue;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import tech.tablesaw.api.IntColumn;
import tech.tablesaw.api.Row;
import tech.tablesaw.api.StringColumn;
import tech.tablesaw.api.Table;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Table Saw writer (mainly for debugging purposes).
 *
 * Is NOT synchronized! Does not work in multi-threading environments.
 *
 * @author Marcus Kessel
 */
public class TableSawWriter implements CellWriter {

    private boolean append = true;

    private Table table;

    private String executionId = "";
    private String abstractionId = "";
    private String actionId = "";

    private Table table() {
        if(table == null) {
            table = Table.create("summary")
                    .addColumns(
                            StringColumn.create("executionId"),
                            StringColumn.create("abstractionId"),
                            StringColumn.create("actionId"),
                            StringColumn.create("sheetId"),
                            StringColumn.create("systemId"),
                            StringColumn.create("variantId"),
                            StringColumn.create("adapterId"),
                            IntColumn.create("x"),
                            IntColumn.create("y"),
                            StringColumn.create("type"),
                            StringColumn.create("value")
                    );
        }

        return table;
    }

    public synchronized void append(TableSawWriter writer) {
        table.append(writer.getTable());
    }

    private synchronized void appendCells(Map<CellId, CellValue> cells, DefaultArena arena) {
        for(Map.Entry<CellId, CellValue> cell : cells.entrySet()) {
            CellId id = cell.getKey();

            Row row = table().appendRow();
            row.setString(0, executionId);
            row.setString(1, abstractionId);
            if(StringUtils.isNotBlank(arena.getName())) {
                row.setString(2, String.format("%s_%s", actionId, arena.getName()));
            } else {
                row.setString(2, actionId);
            }

            row.setString(3, id.getSheetId());
            row.setString(4, id.getSystemId());
            row.setString(5, id.getVariantId());
            row.setString(6, id.getAdapterId());
            row.setInt(7, id.getX());
            row.setInt(8, id.getY());
            row.setString(9, id.getType());
            row.setString(10, cell.getValue().getValue());
        }
    }

    @Override
    public void writeExecutedSequence(SequenceExecutionRecord record, DefaultArena arena) {
        Map<CellId, CellValue> cells = record.toSheetCells();

        appendCells(cells, arena);
    }

    @Override
    public void writeObservations(SequenceExecutionRecord record, DefaultArena arena) {
        if(MapUtils.isEmpty(record.getObservations())) {
            return;
        }

        for(String obKey : record.getObservations().keySet()) {
            Observation observation = record.getObservations().get(obKey);

            Map<CellId, CellValue> cells = observation.toCells(record);

            appendCells(cells, arena);
        }
    }

    @Override
    public void writeObservations(SequenceExecutionRecords records, DefaultArena arena) {
        if(MapUtils.isEmpty(records.getObservations())) {
            return;
        }

        for(String obKey : records.getObservations().keySet()) {
            Observation observation = records.getObservations().get(obKey);

            Map<CellId, CellValue> cells = observation.toCells(records);

            appendCells(cells, arena);
        }
    }

    @Override
    public void writeAdapters(InterfaceSpecification specification, ClassUnderTest classUnderTest, List<AdaptedImplementation> adaptedImplementations) {

    }

    @Override
    public void writeSequenceCode(SequenceExecutionRecords records, DefaultArena arena) {
        Map<CellId, CellValue> cells = new LinkedHashMap<>();

        for(SequenceExecutionRecord sequenceExecutionRecord : records.getRecords()) {
            try {
                cells.putAll(sequenceExecutionRecord.writeSequenceRecord());
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }

        appendCells(cells, arena);
    }

    public Table getTable() {
        return table;
    }

    public void toCsv(String path) throws IOException {
        Optional.ofNullable(getTable()).orElseThrow(() -> new IOException("Table was null")).write().csv(path);
    }

    public String getExecutionId() {
        return executionId;
    }

    public void setExecutionId(String executionId) {
        this.executionId = executionId;
    }

    public String getAbstractionId() {
        return abstractionId;
    }

    public void setAbstractionId(String abstractionId) {
        this.abstractionId = abstractionId;
    }

    public String getActionId() {
        return actionId;
    }

    public void setActionId(String actionId) {
        this.actionId = actionId;
    }
}
