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
package de.uni_mannheim.swt.lasso.engine.action.search;

import de.uni_mannheim.swt.lasso.core.datasource.DataSource;
import de.uni_mannheim.swt.lasso.core.model.System;
import de.uni_mannheim.swt.lasso.core.model.*;
import de.uni_mannheim.swt.lasso.core.model.query.QueryResult;
import de.uni_mannheim.swt.lasso.corpus.Datasource;
import de.uni_mannheim.swt.lasso.datasource.maven.lsl.MavenQuery;
import de.uni_mannheim.swt.lasso.engine.LSLExecutionContext;
import de.uni_mannheim.swt.lasso.engine.LassoUtils;
import de.uni_mannheim.swt.lasso.engine.action.DefaultAction;
import de.uni_mannheim.swt.lasso.engine.action.annotations.LassoAction;
import de.uni_mannheim.swt.lasso.engine.action.annotations.LassoInput;
import de.uni_mannheim.swt.lasso.engine.action.annotations.Local;
import de.uni_mannheim.swt.lasso.engine.action.annotations.Stable;
import de.uni_mannheim.swt.lasso.engine.data.ReportKey;
import de.uni_mannheim.swt.lasso.engine.data.ReportOperations;
import groovy.lang.Closure;
import groovy.lang.GString;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 *
 * @author Marcus Kessel
 */
@LassoAction(desc = "Search Implementations (Query/Find/Filter) from Data Sources")
@Stable
@Local
public class Search extends DefaultAction {

    private static final Logger LOG = LoggerFactory
            .getLogger(Search.class);

    @LassoInput(desc = "Data Source", optional = true)
    public String dataSource;

    @Override
    public void execute(LSLExecutionContext context, ActionConfiguration actionConfiguration) throws IOException {
        if(LOG.isInfoEnabled()) {
            LOG.info("Executing "+  this.getClass());
        }

        Abstraction abstraction = actionConfiguration.getAbstraction();

        LOG.info("Abstraction = {}", abstraction.getName());
        LOG.info("Systems = {}", abstraction.getImplementations().size());

        // custom DSL command
        List<CodeQuery> queries = readQueries(context, actionConfiguration, dataSource);

        List<System> implementations = new LinkedList<>();
        for(CodeQuery codeQuery : queries) {
            try {
                String dataSourceId = LassoUtils.resolveDataSource(context, codeQuery.getDataSource());
                Datasource ds = LassoUtils.getDataSource(context, dataSourceId).orElseThrow(() -> new IllegalArgumentException("Cannot find data source " + dataSourceId));

                DataSource dataSource = context.getDataSourceMap().get(ds.getId());

                MavenQuery queryModel = new MavenQuery();
                context.getLassoContext().register(queryModel);
                queryModel.queryForClasses(codeQuery.getQueryContent(), "class-simple");
                queryModel.setRows(codeQuery.getRows());
                codeQuery.getFilters().forEach(queryModel::filter);

                // do query from data source
                QueryResult queryResult = null;
                try {
                    queryResult = dataSource.query(queryModel);
                    //queryReport.setNumFound(queryResult.getNumFound());
                } catch (Throwable e) {
    //            throw new IOException(String.format("Query failed for model '%s'",
    //                    ToStringBuilder.reflectionToString(queryModel)), e);

                    if(LOG.isWarnEnabled()) {
                        LOG.warn(String.format("Query failed for model '%s'",
                                ToStringBuilder.reflectionToString(queryModel)), e);
                    }
                }

                if(queryResult == null || CollectionUtils.isEmpty(queryResult.getImplementations())) {
                    //
                } else {
                    implementations.addAll(queryResult.getImplementations().stream().map(System::new).collect(Collectors.toList()));
                }
            } catch (Throwable e) {
                LOG.warn("Query failed", e);
            }
        }

        //
        ReportOperations recordOperations = context.getReportOperations();

        // collect unique metrics
        Set<String> metrics = new HashSet<>();

        // write select report
        List<System> executableList = implementations.stream().map(impl -> {
            SelectReport report = createReport(impl.getCode());
            recordOperations.put(context.getExecutionId(), ReportKey.of(this, actionConfiguration.getAbstraction(), impl), report);

            metrics.addAll(impl.getCode().getMeasures().keySet());

            return impl;
        }).toList();

        // write values report
        // publish schema
        String reportName = "IndexMeasurements";
        recordOperations.newValuesReport(context.getExecutionId(), reportName, metrics.stream().collect(Collectors.toMap(v -> v, v -> "java.lang.Double")));

        // now write
        executableList.stream().forEach(executable -> {
            recordOperations.putValues(
                    context.getExecutionId(),
                    ReportKey.of(this, actionConfiguration.getAbstraction(), executable),
                    reportName,
                    executable.getCode().getMeasures());
        });

        // add all
        abstraction.getSystems().addAll(implementations);
        setExecutables(Systems.fromAbstraction(abstraction, getName()));
    }

    protected List<CodeQuery> readQueries(LSLExecutionContext context, ActionConfiguration actionConfiguration, String defaultDataSource) {
        List<CodeQuery> queries = new LinkedList<>();
        if(actionConfiguration.getConfiguration().containsKey("query")) {
            Closure closure = (Closure) ((Object[]) actionConfiguration.getConfiguration().get("query"))[0];

            LOG.debug("FOUND QUERY METHOD " + closure);
            // get query model
            CodeQuery myQuery = new CodeQuery();
            context.getLassoContext().register(myQuery);
            List<Map> queryMaps = (List<Map>) context.getLassoContext().getActionContainerSpec().getActions().get(getName()).applyCustomCommand(closure, myQuery, new Object[]{actionConfiguration.getAbstraction()});

            LOG.debug("FOUND QUERY MODELS " + queryMaps);

            queries = queryMaps.stream().map(m -> {
                // FIXME add more properties
                CodeQuery codeQuery = new CodeQuery();
                codeQuery.setQueryContent(m.get("queryContent").toString()); // GString
                codeQuery.setRows((int) m.get("rows"));
                if(m.containsKey("filters")) {
                    List list = (List) m.get("filters");
                    List<String> filters = list.stream().map(f -> {
                        if(f instanceof GString) {
                            return f.toString();
                        }

                        return (String) f;
                    }).toList();

                    codeQuery.setFilters(filters);
                }

                if(m.containsKey("dataSource")) {
                    codeQuery.setDataSource(m.get("dataSource").toString());
                } else {
                    codeQuery.setDataSource(defaultDataSource);
                }

                return codeQuery;
            }).toList();
        }

        return queries;
    }

    public static SelectReport createReport(CodeUnit impl) {
        SelectReport report = new SelectReport();
        report.setName(impl.getName());
        report.setPackageName(impl.getPackagename());
        report.setScore(impl.getScore());
        report.setUri(impl.toUri());
//            if(impl instanceof MavenImplementation) {
//                report.setSignature(((MavenImplementation) impl).toMQL(true));
//            }
        report.setSignature(impl.getBytecodeName());

        report.setCloneType1Hash(impl.getType1Hash());

        report.setHash(impl.getHash());

        // special
        if(impl.getAlternatives() != null) {
            report.setCollapseByAlternatives(impl.getAlternatives().size());
        }
        if(impl.getClones() != null) {
            report.setHashClones(impl.getClones().size());
        }
        if(impl.getSimilar() != null) {
            report.setNamingClones(impl.getSimilar().size());
        }

//        if(impl.getType1Hash() != null) {
//            CodeUnit implementation = type1HashesCollapseMap.get(impl.getType1Hash());
//            report.setType1Clones(implementation.getType1Clones() != null ? implementation.getType1Clones().size() : 0);
//        }

        return report;
    }
}
