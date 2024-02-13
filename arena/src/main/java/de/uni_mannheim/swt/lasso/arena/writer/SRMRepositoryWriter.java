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
import de.uni_mannheim.swt.lasso.cluster.LassoClusterClient;
import de.uni_mannheim.swt.lasso.cluster.client.ArenaJob;
import de.uni_mannheim.swt.lasso.core.adapter.InterfaceDesc;
import de.uni_mannheim.swt.lasso.engine.adaptation.SystemAdapterReport;
import de.uni_mannheim.swt.lasso.engine.data.ReportKey;
import de.uni_mannheim.swt.lasso.srm.CellId;
import de.uni_mannheim.swt.lasso.srm.CellValue;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Write cells (and reports) to Apache Ignite Cluster.
 *
 * @author Marcus Kessel
 */
public class SRMRepositoryWriter implements CellWriter {

    private final LassoClusterClient client;
    private final ArenaJob arenaJob;

    public SRMRepositoryWriter(LassoClusterClient client, ArenaJob arenaJob) {
        this.client = client;
        this.arenaJob = arenaJob;
    }

    @Override
    public void writeExecutedSequence(SequenceExecutionRecord record, DefaultArena arena) {
        Map<CellId, CellValue> cells = record.toSheetCells();

        store(cells, arena);
    }

    @Override
    public void writeObservations(SequenceExecutionRecord record, DefaultArena arena) {
        if (MapUtils.isEmpty(record.getObservations())) {
            return;
        }

        for (String obKey : record.getObservations().keySet()) {
            Observation observation = record.getObservations().get(obKey);

            Map<CellId, CellValue> cells = observation.toCells(record);

            store(cells, arena);
        }
    }

    @Override
    public void writeObservations(SequenceExecutionRecords records, DefaultArena arena) {
        if (MapUtils.isEmpty(records.getObservations())) {
            return;
        }

        for (String obKey : records.getObservations().keySet()) {
            Observation observation = records.getObservations().get(obKey);

            Map<CellId, CellValue> cells = observation.toCells(records);

            store(cells, arena);
        }
    }

    @Override
    public void writeAdapters(InterfaceSpecification specification, ClassUnderTest classUnderTest, List<AdaptedImplementation> adaptedImplementations) {
        InterfaceDesc interfaceDesc = specification.toDescription();

        SystemAdapterReport systemAdapterReport = new SystemAdapterReport();
        systemAdapterReport.setInterfaceDesc(interfaceDesc);

        systemAdapterReport.setAdapters(adaptedImplementations.stream().map(a -> a.toDescription(specification)).collect(Collectors.toList()));

        ReportKey reportKey = new ReportKey(arenaJob.getActionId(), arenaJob.getAbstractionId(), classUnderTest.getId(), classUnderTest.getImplementation().getCode().getDataSource(), -1);

        client.getClientReportRepository().put(arenaJob.getExecutionId(), reportKey, systemAdapterReport);
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

        store(cells, arena);
    }

    private void store(Map<CellId, CellValue> cells, DefaultArena arena) {
        if (MapUtils.isNotEmpty(cells)) {
            cells.keySet().forEach(id -> {
                id.setExecutionId(arenaJob.getExecutionId());
                id.setAbstractionId(arenaJob.getAbstractionId());
                if (StringUtils.isNotBlank(arena.getName())) {
                    id.setArenaId(arena.getName());
                } else {
                    id.setActionId(arenaJob.getActionId());
                }
                id.setActionId(arenaJob.getActionId());
            });

            // store all cells
            client.getSrmRepository().putAll(cells);
        }
    }
}
