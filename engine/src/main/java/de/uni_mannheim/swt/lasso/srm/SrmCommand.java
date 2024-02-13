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
package de.uni_mannheim.swt.lasso.srm;

import de.uni_mannheim.swt.lasso.cluster.ClusterEngine;
import de.uni_mannheim.swt.lasso.core.model.System;
import de.uni_mannheim.swt.lasso.engine.LSLExecutionContext;
import de.uni_mannheim.swt.lasso.lsl.spec.ActionSpec;
import org.apache.commons.lang3.StringUtils;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.cache.query.QueryCursor;
import org.apache.ignite.cache.query.SqlQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.cache.Cache;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * SRM commands provided for LSL scripts.
 *
 * @author Marcus Kessel
 */
public class SrmCommand {

    private static final Logger LOG = LoggerFactory
            .getLogger(SrmCommand.class);

    private ActionSpec actionSpec;
    private final LSLExecutionContext context;

    private String sheet;
    private int statement = -1;
    private String expectedValue;
    private String abstraction;
    private String type = "observation";

    public SrmCommand(LSLExecutionContext context) {
        this.context = context;
    }

    public SrmCommand statement(int statement) {
        this.statement = statement;

        return this;
    }

    public SrmCommand expectedValue(String expectedValue) {
        this.expectedValue = expectedValue;

        return this;
    }

    public SrmCommand type(String type) {
        this.type = type;

        return this;
    }

    public SrmCommand abstraction(String abstraction) {
        this.abstraction = abstraction;

        return this;
    }

    public SrmCommand sheet(String sheet) {
        this.sheet = sheet;

        return this;
    }

    // final methods

    public void execute() {
        // TODO formulate query

    }

    public List<System> getImplementations() {
        ClusterEngine clusterEngine = context.getConfiguration().getService(ClusterEngine.class);

        //
        ClusterSRMRepository srmRepository = clusterEngine.getClusterSRMRepository();
        IgniteCache<CellId, CellValue> cache = srmRepository.getCache();

        StringBuilder sql = new StringBuilder("executionId = ? AND actionId = ?");

        List<Object> args = new LinkedList<>();
        args.add(actionSpec.getLasso().getExecutionId()); // important, cannot be retrieved from context, since LSL executes on master
        args.add(actionSpec.getName());

        if(StringUtils.isNotBlank(sheet)) {
            sql.append(" AND sheetId = ?");
            args.add(sheet);
        }
        if(StringUtils.isNotBlank(abstraction)) {
            sql.append(" AND abstractionId = ?");
            args.add(abstraction);
        }
        if(statement > -1) {
            sql.append(" AND x = 0 AND y = ?"); // FIXME assume first column
            args.add(statement - 1); // y starts at 0, statements naturally at 1
        }
        if(StringUtils.isNotBlank(expectedValue)) {
            sql.append(" AND value = ?");
            args.add(expectedValue);
        }
        if(StringUtils.isNotBlank(type)) {
            sql.append(" AND type = ?");
            args.add(type);
        }

        //SqlFieldsQuery query = new SqlFieldsQuery(sql.toString());
        //query.setArgs(args.toArray());

        //FieldsQueryCursor<List <?>> cursor = cache.query(query);

        SqlQuery<CellId, CellValue> query = new SqlQuery<>(CellValue.class, sql.toString());
        query.setArgs(args.toArray());

        if(LOG.isDebugEnabled()) {
            LOG.debug("Formulated query '{}'. Args = '{}'", sql.toString(), Arrays.toString(args.toArray()));
        }

        QueryCursor<Cache.Entry<CellId, CellValue>> cursor = cache.query(query);

        List<System> implementationList = StreamSupport
                .stream(cursor.spliterator(), false)
                .map(e -> {
                    List<System> implementations = actionSpec.getAbstractionContainerSpec()
                            .getAbstractions()
                            .get(e.getKey().getAbstractionId())
                            .getAbstraction()
                            .getImplementations();

                    Optional<System> impl = implementations.stream()
                            .filter(i -> i.getId().equals(e.getKey().getSystemId())).findFirst();

                    if(LOG.isDebugEnabled()) {
                        LOG.debug("Found system '{}' = {}", e.getKey().getSystemId(), impl.isPresent());
                    }

                    return impl.orElse(null);
                }).filter(Objects::nonNull)
                .collect(Collectors.toList());

        return implementationList;
    }

    public Set<String> distinctValues() {
        // FIXME
        execute();

        return null;
    }

    public void setActionSpec(ActionSpec actionSpec) {
        this.actionSpec = actionSpec;
    }
}
