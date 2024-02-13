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
package de.uni_mannheim.swt.lasso.service.controller.graphql;

import de.uni_mannheim.swt.lasso.cluster.ClusterEngine;
import de.uni_mannheim.swt.lasso.service.controller.BaseApi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import tech.tablesaw.api.Row;
import tech.tablesaw.api.Table;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

/**
 * GraphQL controller
 *
 * @author Marcus Kessel
 */
// FIXME experimental
@Controller
public class GraphController extends BaseApi {

    private static final Logger LOG = LoggerFactory
            .getLogger(GraphController.class);

    @Autowired
    ClusterEngine clusterEngine;

    @Autowired
    private Environment env;

    @QueryMapping
    public List<ObservationRecord> observationRecords(@Argument(name = "q") ObservationQuery query,
                                                      @AuthenticationPrincipal UserDetails userDetails) {
        try {
            Table df = clusterEngine.getClusterSRMRepository().sqlToTable("SELECT x,y,adapterid,value from srm.CellValue where executionId = ? and systemId = ? and type = ? order by adapterid,x,y asc",
                    query.getExecutionId(), query.getSystemId(), query.getType());

            List<ObservationRecord> records = new LinkedList<>();
            for(Row row : df) {
                ObservationRecord r = new ObservationRecord();

                r.setExecutionId(query.getExecutionId());
                r.setSystemId(query.getSystemId());
                r.setType(query.getType());
                r.setX(row.getInt(0));
                r.setY(row.getInt(1));
                r.setAdapter(row.getString(2));
                r.setValue(row.getString(3));

                records.add(r);
            }

            return records;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @QueryMapping
    public List<String> getRecordTypes(@Argument(name = "q") ObservationQuery query,
                                                  @AuthenticationPrincipal UserDetails userDetails) {
        try {
            Table df = clusterEngine.getClusterSRMRepository().sqlToTable("SELECT distinct(type) from srm.CellValue where executionId = ? and systemId = ? order by type", query.getExecutionId(), query.getSystemId());

            return df.stringColumn(0).asList();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
