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
package de.uni_mannheim.swt.lasso.service.systemtests.integration

import de.uni_mannheim.swt.lasso.core.dto.srm.Sheet
import de.uni_mannheim.swt.lasso.engine.DataSourceNotFoundException
import de.uni_mannheim.swt.lasso.service.systemtests.util.LassoTestEngine
import de.uni_mannheim.swt.lasso.srm.JDBC
import de.uni_mannheim.swt.lasso.srm.olap.Warehouse
import joinery.DataFrame
import org.apache.commons.lang3.builder.ToStringBuilder
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import tech.tablesaw.api.Table

/**
 * Playground for JDBC (debugging purposes)
 *
 * @author mkessel
 */
class JDBCPlaygroundSystemTest extends AbstractGroovySystemTest {

    @Autowired
    @Qualifier("testLassoEngine")
    LassoTestEngine lassoEngine;

    @Test
    void test_DEBUG_SCHEMA() throws IOException, DataSourceNotFoundException {
        JDBC jdbc = new JDBC();

        Table schemaTable = jdbc.sqlToTable("SELECT * FROM INFORMATION_SCHEMA.TABLES");
        System.out.println(schemaTable.printAll());
    }

    @Test
    void test_getstimulussheets() {
        String executionId = "7558a558-9719-492c-966b-2e34a8e4d90e";
        String actionId = "test";
        String abstractionId = "HumanEval_13_greatest_common_divisor";

        JDBC jdbc = new JDBC();

        List<Sheet> stimulusSheets = Warehouse.queryStimulusSheets(executionId, actionId, abstractionId);

        stimulusSheets.stream().forEach {s -> System.out.println(ToStringBuilder.reflectionToString(s))}
    }
}
