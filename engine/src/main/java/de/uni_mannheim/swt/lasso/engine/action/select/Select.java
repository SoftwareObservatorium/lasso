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
package de.uni_mannheim.swt.lasso.engine.action.select;

import de.uni_mannheim.swt.lasso.core.datasource.DataSource;
import de.uni_mannheim.swt.lasso.core.model.*;

import de.uni_mannheim.swt.lasso.core.model.System;
import de.uni_mannheim.swt.lasso.core.model.query.QueryResult;
import de.uni_mannheim.swt.lasso.datasource.maven.MavenDataSource;
import de.uni_mannheim.swt.lasso.datasource.maven.MavenCodeUnitUtils;
import de.uni_mannheim.swt.lasso.datasource.maven.lsl.MavenQuery;
import de.uni_mannheim.swt.lasso.engine.LSLExecutionContext;
import de.uni_mannheim.swt.lasso.engine.action.DefaultAction;
import de.uni_mannheim.swt.lasso.engine.action.annotations.*;
import de.uni_mannheim.swt.lasso.engine.data.ReportKey;
import de.uni_mannheim.swt.lasso.engine.data.ReportOperations;
import de.uni_mannheim.swt.lasso.engine.workspace.Workspace;
import de.uni_mannheim.swt.lasso.engine.workspace.WorkspaceManager;
import de.uni_mannheim.swt.lasso.index.repo.SolrCandidateDocument;
import de.uni_mannheim.swt.lasso.lsl.spec.AbstractionSpec;
import de.uni_mannheim.swt.lasso.lsl.spec.LassoSpec;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.tablesaw.api.Table;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.stream.Collectors;

/**
 *
 * @author Marcus Kessel
 */
@LassoAction(desc = "Select Implementations (Query/Find/Filter) from Data Sources")
@Stable
@Local
public class Select extends DefaultAction {

    private static final Logger LOG = LoggerFactory
            .getLogger(Select.class);

    public static final String FROM_EXECUTION_ID = "from.execution.id";

    //@LassoInput(desc = "Query directly", optional = true)
    //public boolean directly = false;

    @LassoInput(desc = "Data Source", optional = true)
    @Deprecated
    public String dataSource;

    @LassoInput(desc = "List of Data Sources", optional = true)
    public List<String> dataSources;

    @LassoInput(desc = "List of Method Implementations", optional = true)
    public List<String> implementations;

    @LassoInput(desc = "Reuse from past study (Select)", optional = true)
    public String reuseExecutionId;
    @LassoInput(desc = "Reuse from past study (Select)", optional = true)
    public String reuseTableName;

    @Override
    public Abstraction createAbstraction(LSLExecutionContext context, ActionConfiguration actionConfiguration, AbstractionSpec abstractionSpec) throws IOException {
        Abstraction abstraction = new Abstraction();
        abstraction.setName(abstractionSpec.getName());

        List<String> dataSourceList;
        if(CollectionUtils.isNotEmpty(dataSources)) {
            dataSourceList = this.dataSources;
        } else {
            // default DS
            String dataSourceId;
            if(StringUtils.isNotBlank(dataSource)) {
                dataSourceId = dataSource;
            } else {
                dataSourceId = abstractionSpec.getLasso().getDataSources().get(0);
            }

            dataSourceList = Arrays.asList(dataSourceId);
        }

        if(LOG.isInfoEnabled()) {
            LOG.info("Using data source(s) '{}'", String.join(",", dataSourceList));
        }

        abstraction.setImplementations(new ArrayList<>());

        QueryReport queryReport = new QueryReport();
        queryReport.setActionName(this.getName());
        for(String dataSourceId : dataSourceList) {
            // get first one
            DataSource dataSource = context.getDataSourceMap().get(dataSourceId);

            if(StringUtils.isNotBlank(reuseExecutionId) && StringUtils.isNotBlank(reuseTableName)) {
                if(LOG.isInfoEnabled()) {
                    LOG.info("Reusing implementations from study '{}' in table name '{}'", reuseExecutionId, reuseTableName);
                }

                //
                WorkspaceManager workspaceManager = context.getConfiguration().getService(WorkspaceManager.class);
                Workspace workspace;
                try {
                    workspace = workspaceManager.load(reuseExecutionId);
                } catch (Throwable e) {
                    throw new RuntimeException(e);
                }

                if(LOG.isInfoEnabled()) {
                    LOG.info("Reading from workspace root '{}'", workspace.getRoot());
                }

                File csvFile = new File(workspace.getRoot(), reuseTableName);
                if(!csvFile.exists()) {
                    throw new IOException("Cannot locate " + reuseTableName);
                }

                Table table = Table.read().csv(csvFile);
                implementations = new ArrayList<>(table.select("IMPLEMENTATION").stream()
                        .map(row -> row.getString(0))
                        .collect(Collectors.toSet()));

                List<System> implementationList = retrieveImplementations(context, dataSource);

                abstraction.getImplementations().addAll(implementationList);
            } else if(CollectionUtils.isNotEmpty(implementations)) {
                if(LOG.isInfoEnabled()) {
                    LOG.info("Using implementation list '{}'", String.join(",", implementations));
                }

                List<System> implementationList = retrieveImplementations(context, dataSource);

                abstraction.getImplementations().addAll(implementationList);
            } else {
                // get query model
                LassoSpec queryModel = (LassoSpec) dataSource.createQueryModelForLSL();
                // register query model
                abstractionSpec.getLasso().registerDataSourceQuery(dataSourceId, queryModel);

                // call queryModel with
                abstractionSpec.apply(queryModel);

//                // FIXME
//                if(type1Clones) {
//                    // required fetch "content" field (FIXME currently set explicitly).
//                }

                if(queryReport.getQuery() == null) {
                    if(queryModel instanceof MavenQuery) {
                        queryReport.setQuery(((MavenQuery)queryModel).getQuery());
                    } else {
                        queryReport.setQuery("n/a");
                    }
                }

                // do query from data source
                QueryResult queryResult = null;
                try {
                    queryResult = dataSource.query(queryModel);
                    queryReport.setNumFound(queryResult.getNumFound());
                } catch (Throwable e) {
//            throw new IOException(String.format("Query failed for model '%s'",
//                    ToStringBuilder.reflectionToString(queryModel)), e);

                    if(LOG.isWarnEnabled()) {
                        LOG.warn(String.format("Query failed for model '%s'",
                                ToStringBuilder.reflectionToString(queryModel)), e);
                    }
                }

                if(queryResult != null && queryResult.getInterfaceSpecification() != null) {
                    LOG.info("Setting interface specification '{}'", ToStringBuilder.reflectionToString(queryResult.getInterfaceSpecification()));

                    abstraction.getSpecification().setInterfaceSpecification(queryResult.getInterfaceSpecification());
                }

                if(queryResult == null || CollectionUtils.isEmpty(queryResult.getImplementations())) {
                    //abstraction.setImplementations(new LinkedList<>());
                } else {
                    abstraction.getImplementations().addAll(queryResult.getImplementations().stream().map(System::new).collect(Collectors.toList()));
                }
            }
        }

        queryReport.setTotal(abstraction.getImplementations().size());

        // persist report
        context.getReportOperations()
                .put(context.getExecutionId(),
                        ReportKey.of(this.getName(), abstractionSpec.getName(), QueryReport.UNDEFINED, QueryReport.UNDEFINED),
                        queryReport);

        return abstraction;
    }

    private List<System> retrieveImplementations(LSLExecutionContext context, DataSource dataSource) {
        // do in parallel
        ForkJoinPool customThreadPool = new ForkJoinPool(4);

        // parallel
        ForkJoinTask<List<CodeUnit>> task = customThreadPool.submit(
                () -> implementations.stream().parallel().map(implId -> {

                    if(LOG.isDebugEnabled()) {
                        LOG.debug("Querying for '{}'", implId);
                    }

                    MavenDataSource mavenDataSource = (MavenDataSource) dataSource;
                    try {
                        SolrCandidateDocument doc = (SolrCandidateDocument) mavenDataSource.getMavenCentralIndex().getMavenCentralRepository().queryForClassCandidate(implId);

                        CodeUnit implementation = MavenCodeUnitUtils.toImplementation(doc.getSolrDocument());
                        implementation.setDataSource(dataSource.getId());

                        if(LOG.isDebugEnabled()) {
                            LOG.debug("Returned '{}'", implementation.getId());
                        }

                        return implementation;
                    } catch (Throwable e) {
                        LOG.warn("Failed to retrieve implementation {}", implId);
                        LOG.warn("Failure", e);

                        return null;
                    }

//                    //
//                    MavenQuery queryModel = (MavenQuery) dataSource.createQueryModelForLSL();
//                    // register query model
//                    context.getLassoContext().registerDataSourceQuery(dataSource.getId(), queryModel);
//
//                    // FIXME make configurable
//                    queryModel.docType("method");
//                    queryModel.queryForMethods("*:*");
//                    queryModel.filter(String.format("id:\"%s\"", implId));
//                    queryModel.setRows(1);
//
//                    // do query from data source
//                    QueryResult queryResult = null;
//                    try {
//                        queryResult = dataSource.query(queryModel);
//                    } catch (IOException e) {
//                        throw new RuntimeException(String.format("Query failed for model '%s'",
//                                ToStringBuilder.reflectionToString(queryModel)), e);
//                    }
//
//                    return queryResult.getImplementations().get(0);
                })
                        .filter(Objects::nonNull) // remove NULLinger
                        .collect(Collectors.toList()));

        List<CodeUnit> implementationList = new LinkedList<>();
        try {
            implementationList = task.get();
        } catch (Throwable e) {
            LOG.warn("Getting list failed", e);
        }

        customThreadPool.shutdown();

        return CollectionUtils.isNotEmpty(implementationList) ?
                implementationList.stream().map(System::new).collect(Collectors.toList()) : new LinkedList<>();
    }

    @Override
    public void execute(LSLExecutionContext context, ActionConfiguration actionConfiguration) throws IOException {
        if(LOG.isInfoEnabled()) {
            LOG.info("Executing "+  this.getClass());
        }

//        // measure type1 clones
//        Map<String, CodeUnit> type1HashesCollapseMap = new HashMap<>();
//        actionConfiguration.getAbstraction().getImplementations().removeIf(impl -> {
//            // measure type-1 clone hash?
//            String type1Hash = null;
//            if(type1Clones && impl.getCode().getContent() != null) {
//                try {
//                    type1Hash = CloneDetection.type1Hash(impl);
//                } catch (Throwable e) {
//                    if(LOG.isWarnEnabled()) {
//                        LOG.warn(String.format("Could not get type-1 clone hash for '%s'", impl.getId()), e);
//                    }
//                }
//            }
//
//            impl.getCode().setType1Hash(type1Hash);
//
//            // collapse by type1 hash?
//            if(collapseType1Clones && impl.getType1Hash() != null) {
//                if(type1HashesCollapseMap.containsKey(impl.getType1Hash())) {
//                    CodeUnit implementation = type1HashesCollapseMap.get(impl.getType1Hash());
//                    // add as child
//                    if(implementation.getType1Clones() == null) {
//                        implementation.setType1Clones(new LinkedList<>());
//                    }
//                    implementation.getType1Clones().add(impl);
//
//                    // remove
//                    return true;
//                } else {
//                    type1HashesCollapseMap.put(impl.getType1Hash(), impl);
//                }
//            }
//
//            // do not remove
//            return false;
//        });

        //
        ReportOperations recordOperations = context.getReportOperations();

        // collect unique metrics
        Set<String> metrics = new HashSet<>();

        // write select report
        List<System> executableList = actionConfiguration.getAbstraction().getImplementations().stream().map(impl -> {
            SelectReport report = createReport(impl.getCode());
            recordOperations.put(context.getExecutionId(), ReportKey.of(this, actionConfiguration.getAbstraction(), impl), report);

            metrics.addAll(impl.getCode().getMeasures().keySet());

            return impl;
        }).collect(Collectors.toList());

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

        // set setExecutables() to be compliant with other actions
        Systems executables = new Systems();
        executables.setExecutables(executableList);
        executables.setAbstractionName(actionConfiguration.getAbstraction().getName());
        executables.setActionInstanceId(getInstanceId());
        setExecutables(executables);
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
