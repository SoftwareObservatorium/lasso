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
package de.uni_mannheim.swt.lasso.engine.action.select.stats;

import de.uni_mannheim.swt.lasso.core.model.Systems;
import de.uni_mannheim.swt.lasso.index.SearchOptions;
import de.uni_mannheim.swt.lasso.index.query.lql.old.LQL2LuceneClassQuery;
import de.uni_mannheim.swt.lasso.index.query.lql.builder.CandidateQuery;
import de.uni_mannheim.swt.lasso.index.query.lql.builder.QueryBuilder;
import de.uni_mannheim.swt.lasso.index.repo.MavenCentralRepository;
import de.uni_mannheim.swt.lasso.core.model.Abstraction;
import de.uni_mannheim.swt.lasso.core.model.ActionConfiguration;
import de.uni_mannheim.swt.lasso.core.model.CodeUnit;
import de.uni_mannheim.swt.lasso.datasource.maven.MavenDataSource;
import de.uni_mannheim.swt.lasso.datasource.maven.support.MavenCentralIndex;

import de.uni_mannheim.swt.lasso.engine.LSLExecutionContext;
import de.uni_mannheim.swt.lasso.engine.action.DefaultAction;
import de.uni_mannheim.swt.lasso.engine.action.annotations.LassoAction;
import de.uni_mannheim.swt.lasso.engine.action.annotations.LassoInput;
import de.uni_mannheim.swt.lasso.engine.action.annotations.Local;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.solr.common.util.NamedList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.tablesaw.api.DoubleColumn;
import tech.tablesaw.api.StringColumn;
import tech.tablesaw.api.Table;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Marcus Kessel
 */
@LassoAction(desc = "Measure project-level metrics (aggregated)")
//@Stable
@Local // LOCAL!
@Deprecated
public class ProjectStats extends DefaultAction {

    private static final Logger LOG = LoggerFactory
            .getLogger(ProjectStats.class);

    @LassoInput(desc = "Index measures", optional = false)
    public List<String> measures = Arrays.asList(
            "m_static_loc_td",
            "m_static_complexity_td");

    @LassoInput(desc = "Constraints", optional = false)
    public List<String> constraints = null;

    @LassoInput(desc = "Data Source", optional = true)
    public String dataSource;

    @Override
    public void execute(LSLExecutionContext context, ActionConfiguration actionConfiguration) throws IOException {
        if(LOG.isInfoEnabled()) {
            LOG.info("Executing "+  this.getClass());
        }

        Abstraction abstraction = actionConfiguration.getAbstraction();

        if(CollectionUtils.isEmpty(abstraction.getImplementations())) {
            //
            if(LOG.isWarnEnabled()) {
                LOG.warn("Implementations were empty for '{}'", abstraction.getName());
            }

            return;
        }

        // default DS
        String dataSourceId;
        if(StringUtils.isNotBlank(dataSource)) {
            dataSourceId = dataSource;
        } else {
            dataSourceId = context.getLassoContext().getDataSources().get(0);
        }

        // get first one
        MavenDataSource dataSource = (MavenDataSource) context.getDataSourceMap().get(dataSourceId);

        MavenCentralIndex mavenCentralIndex = dataSource.getMavenCentralIndex();
        MavenCentralRepository mavenCentralRepository = mavenCentralIndex.getMavenCentralRepository();

        List<String> distinctProjects = abstraction.getImplementations()
                .stream()
                .map(i -> i.getCode().toUri())
                .distinct()
                .collect(Collectors.toList());

        String docType = abstraction.getImplementations().get(0).getCode().getUnitType() == CodeUnit.CodeUnitType.METHOD ?
                "method" : "class";

        Table table =
                Table.create("projectstats")
                        .addColumns(
                                StringColumn.create("URI"));

        for(String projectUri : distinctProjects) {
            CandidateQuery candidateQuery = createQuery(projectUri, docType);
            NamedList<Object> facetResponse = mavenCentralRepository.queryStatsUsingJsonFacet(candidateQuery, measures);

            table.stringColumn("URI").append(projectUri);

            for(Map.Entry<String, Object> entry : facetResponse) {
                if(!table.columnNames().contains(entry.getKey())) {
                    DoubleColumn dblCol = DoubleColumn.create(entry.getKey());
                    dblCol.append(toDouble(entry.getValue()));

                    table.addColumns(dblCol);
                } else {
                    table.doubleColumn(entry.getKey()).append(toDouble(entry.getValue()));
                }
            }
        }

        // export CSV
        File csv = context.getWorkspace().createFile(String.format("project_stats_%s.csv", abstraction.getName()));

        if(LOG.isInfoEnabled()) {
            LOG.info("Writing CSV for '{}' to '{}'", abstraction.getName(), csv.getAbsolutePath());
        }

        table.write().csv(csv);

        // set setExecutables() to be compliant with other actions
        Systems executables = new Systems();
        executables.setAbstractionName(abstraction.getName());
        executables.setExecutables(abstraction.getImplementations());
        setExecutables(executables);
    }

    private double toDouble(Object obj) {
        if(obj instanceof Integer) {
            return ((Integer)obj).doubleValue();
        }

        return (double) obj;
    }

    private CandidateQuery createQuery(String projectUri, String docType) throws IOException {
        QueryBuilder builder = new QueryBuilder();

        String query = "*:*";
        List<String> constraints = new LinkedList<>();
        constraints.addAll(Arrays.asList(
                "uri:\""+projectUri+"\"",
                "doctype_s:\""+docType+"\"",
                "type:\"c\""
        ));

        if(CollectionUtils.isNotEmpty(this.constraints)) {
            constraints.addAll(this.constraints);
        }

        SearchOptions searchOptions = new SearchOptions();
        searchOptions.setStrategy(LQL2LuceneClassQuery.class);

        CandidateQuery candidateQuery = builder.build(query, constraints, searchOptions);

        return candidateQuery;
    }
}
