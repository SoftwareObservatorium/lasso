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
package de.uni_mannheim.swt.lasso.engine.data;

import de.uni_mannheim.swt.lasso.core.data.LassoReport;
import de.uni_mannheim.swt.lasso.core.model.Abstraction;
import de.uni_mannheim.swt.lasso.core.model.System;
import tech.tablesaw.api.Table;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * LASSO Report operations.
 *
 * @author Marcus Kessel
 * @see LassoReport
 */
public interface ReportOperations {

    Class<? extends LassoReport> toLassoReportClass(String executionId, String reportId);

    void put(String executionId, ReportKey key, LassoReport report);
    <T extends LassoReport> void remove(String executionId, ReportKey key, Class<T> reportType);

    <T extends LassoReport> T get(String executionId, ReportKey key, Class<T> reportType);

    <T extends LassoReport> T getLast(String executionId, ReportKey key, Class<T> reportType);
    <T extends LassoReport> T getFirst(String executionId, ReportKey key, Class<T> reportType);

    Table reportToTable(String executionId, Class<? extends LassoReport> reportType) throws IOException;
    Table reportToTable(String executionId, String tableName) throws IOException;

    Table reportToTable(String executionId) throws IOException;

    @Deprecated
    List<Integer> getPermutationIds(String executionId, ReportKey key, Class<? extends LassoReport> reportType) throws IOException;

    void export(Table table, File file) throws IOException;

    void export(String executionId, String actionName, Class<? extends LassoReport> reportType, File file) throws IOException;

    void export(String executionId, String actionName, File file) throws IOException;

    // select
    Table getValues(String executionId, Abstraction abstraction, String qualifiedField) throws IOException;

    Table select(String executionId, String sql) throws IOException;

    void newValuesReport(String executionId, String reportName, Map<String, String> valueTypes);
    void putValues(String executionId, ReportKey key, String reportName, Map<String, ?> values);

    void putValues(String executionId, String action, Abstraction abstraction, System implementation, String reportName, Map<String, ?> values);
}
