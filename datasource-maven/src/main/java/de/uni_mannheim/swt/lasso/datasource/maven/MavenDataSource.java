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
package de.uni_mannheim.swt.lasso.datasource.maven;

import de.uni_mannheim.swt.lasso.datasource.maven.filter.PojoFilter;
import de.uni_mannheim.swt.lasso.index.CandidateQueryResult;
import de.uni_mannheim.swt.lasso.index.SearchOptions;
import de.uni_mannheim.swt.lasso.index.filter.CandidateFilterManager;
import de.uni_mannheim.swt.lasso.index.repo.CandidateDocument;
import de.uni_mannheim.swt.lasso.index.repo.SolrCandidateDocument;
import de.uni_mannheim.swt.lasso.core.datasource.DataSource;
import de.uni_mannheim.swt.lasso.core.model.CodeUnit;
import de.uni_mannheim.swt.lasso.core.model.query.QueryResult;
import de.uni_mannheim.swt.lasso.datasource.maven.filter.MethodSignatureFilter;
import de.uni_mannheim.swt.lasso.datasource.maven.filter.SameOwnerClassFilter;
import de.uni_mannheim.swt.lasso.datasource.maven.lsl.MavenQuery;
import de.uni_mannheim.swt.lasso.datasource.expansion.signature.JavaSignatureFilter;
import de.uni_mannheim.swt.lasso.datasource.maven.support.MavenCentralIndex;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A data source based on Maven repositories.
 *
 * @author Marcus Kessel
 */
public class MavenDataSource extends DataSource {

    private static final Logger LOG = LoggerFactory
            .getLogger(MavenDataSource.class);

    private final MavenCentralIndex mavenCentralIndex;

    public MavenDataSource(MavenCentralIndex mavenCentralIndex) {
        this.mavenCentralIndex = mavenCentralIndex;
    }

    public MavenCentralIndex getMavenCentralIndex() {
        return mavenCentralIndex;
    }

    @Override
    public Object createQueryModelForLSL() {
        return new MavenQuery();
    }

    @Override
    public QueryResult query(Object queryModel) throws IOException {
        MavenQuery mavenQuery = (MavenQuery) queryModel;

        if(LOG.isDebugEnabled()) {
            LOG.debug("Query model => {}", ToStringBuilder.reflectionToString(queryModel));

            LOG.debug("Strategy class {}", ((MavenQuery) queryModel).getOptions().getStrategy());
        }

        SearchOptions searchOptions = mavenQuery.getOptions();

        searchOptions.setExpandedRows(mavenQuery.getExpandedRows());

        // setup post filter
        CandidateFilterManager candidateFilterManager = new CandidateFilterManager();
        searchOptions.setCandidateFilter(candidateFilterManager);

        List<String> constraints = new ArrayList<>(mavenQuery.getConstraints());

        // candidate filter?
        if(mavenQuery.isStrictTypeFilter()) {
            MethodSignatureFilter methodSignatureFilter = new MethodSignatureFilter();

            List<String> inputTypes = mavenQuery.getAllowedInputTypes();
            List<String> returnTypes = mavenQuery.getAllowedReturnTypes();

            if(CollectionUtils.isNotEmpty(inputTypes)) {
                methodSignatureFilter.getSignatureFilter().setAllowedInputTypes(new HashSet<>(inputTypes));
            }

            if(CollectionUtils.isNotEmpty(returnTypes)) {
                methodSignatureFilter.getSignatureFilter().setAllowedReturnTypes(new HashSet<>(returnTypes));
            }

            // add strict method signature type filter
            candidateFilterManager.addFilter(methodSignatureFilter);
        } else if(mavenQuery.isJavaTypeFilter()) {
            MethodSignatureFilter methodSignatureFilter = new MethodSignatureFilter();
            methodSignatureFilter.setSignatureFilter(new JavaSignatureFilter());
            candidateFilterManager.addFilter(methodSignatureFilter);
        }

        if(mavenQuery.isDropPojo()) {
            candidateFilterManager.addFilter(new PojoFilter());
        }

        // collapse by owner class?
        if(mavenQuery.isDoCollapseByClass()) {
            // add strict method signature type filter
            candidateFilterManager.addFilter(new SameOwnerClassFilter());
        }

        // FIXME realize strategy pattern

        CandidateQueryResult candidateResult = null;

        // project sampling enabled?
        if(mavenQuery.isProjectSampling()) {
            List<CandidateDocument> mergedCandidates = new ArrayList<>();
            long mergedTotal = 0;

            // 1. sample N FAs from N projects
            if(mavenQuery.isRandom()) {
                try {
                    candidateResult = mavenCentralIndex
                            .randomQuery(mavenQuery.getQuery(),
                                    mavenQuery.getOptions(),
                                    constraints.stream().toArray(String[]::new),
                                    mavenQuery.getStart(),
                                    mavenQuery.getRows(),
                                    mavenQuery.getOrderByClauses());
                } catch (Throwable e) {
                    throw new IOException("randomQuery failed", e);
                }
            } else {
                try {
                    candidateResult = mavenCentralIndex
                            .query(mavenQuery.getQuery(),
                                    mavenQuery.getOptions(),
                                    constraints.stream().toArray(String[]::new),
                                    mavenQuery.getStart(),
                                    mavenQuery.getRows(),
                                    mavenQuery.getOrderByClauses());
                } catch (Throwable e) {
                    throw new IOException("query failed", e);
                }
            }

            // 2. for each sampled FA, get all remaining methods
            List<CandidateDocument> candidates = candidateResult.getCandidates();

            for(CandidateDocument candidate : candidates) {
                // refine constraints
                List<String> projectConstraints = new ArrayList<>(constraints);

                // restrict to project
                projectConstraints.add(String.format("uri:\"%s:%s:%s\"", candidate.getGroupId(), candidate.getArtifactId(), candidate.getVersion()));

                CandidateQueryResult projectCandidateResult = null;
                try {
                    projectCandidateResult = mavenCentralIndex
                            .query(mavenQuery.getQuery(),
                                    mavenQuery.getOptions(),
                                    projectConstraints.stream().toArray(String[]::new),
                                    mavenQuery.getStart(),
                                    //mavenQuery.getRows(),
                                    Integer.MAX_VALUE,
                                    mavenQuery.getOrderByClauses());

                    // add all
                    mergedCandidates.addAll(projectCandidateResult.getCandidates());
                    mergedTotal += projectCandidateResult.getTotal();
                } catch (Throwable e) {
                    throw new IOException("query failed", e);
                }
            }

            // merge
            candidateResult.setCandidates(mergedCandidates);
            candidateResult.setTotal(mergedTotal);
        } else {
            //
            if(mavenQuery.isRandom()) {
                try {
                    candidateResult = mavenCentralIndex
                            .randomQuery(mavenQuery.getQuery(),
                                    mavenQuery.getOptions(),
                                    constraints.stream().toArray(String[]::new),
                                    mavenQuery.getStart(),
                                    mavenQuery.getRows(),
                                    mavenQuery.getOrderByClauses());
                } catch (Throwable e) {
                    throw new IOException("randomQuery failed", e);
                }
            } else {
                try {
                    if(mavenQuery.isDirectly()) {
                        candidateResult = mavenCentralIndex
                                .queryDirectly(mavenQuery.getQuery(),
                                        mavenQuery.getOptions(),
                                        constraints.stream().toArray(String[]::new),
                                        mavenQuery.getStart(),
                                        mavenQuery.getRows(),
                                        mavenQuery.getOrderByClauses());
                    } else {
                        candidateResult = mavenCentralIndex
                                .query(mavenQuery.getQuery(),
                                        mavenQuery.getOptions(),
                                        constraints.stream().toArray(String[]::new),
                                        mavenQuery.getStart(),
                                        mavenQuery.getRows(),
                                        mavenQuery.getOrderByClauses());
                    }
                } catch (Throwable e) {
                    throw new IOException("query failed", e);
                }
            }
        }

        QueryResult queryResult = new QueryResult();
        queryResult.setNumFound(candidateResult.getTotal());
        if(candidateResult.getQuery().getLqlQuery() != null) {
            if(candidateResult.getQuery().getLqlQuery().getLQLParseResult() != null) {
                if(candidateResult.getQuery().getLqlQuery().getLQLParseResult().hasInterfaceSpecification()) {
                    queryResult.setInterfaceSpecification(candidateResult.getQuery().getLqlQuery().getLQLParseResult().getInterfaceSpecification());

                    if(LOG.isInfoEnabled()) {
                        LOG.info("Found interface specification '{}'", queryResult.getInterfaceSpecification());
                    }
                }
            }
        }

        //
        if(CollectionUtils.isNotEmpty(candidateResult.getCandidates())) {
            List<CodeUnit> implementations = candidateResult.getCandidates().stream().map(c -> (SolrCandidateDocument) c)
                    .map(c -> {
                        CodeUnit implementation = MavenCodeUnitUtils.toImplementation(c.getSolrDocument());
                        implementation.setDataSource(getId());

                        return implementation;
                    })
                    .collect(Collectors.toList());

            // add alternatives?
            List<CodeUnit> allImplementations = new ArrayList<>();
            if(mavenQuery.isUseAlternatives()) {
                for(CodeUnit implementation : implementations) {
                    allImplementations.add(implementation);
                    if(CollectionUtils.isNotEmpty(implementation.getAlternatives())) {
                        allImplementations.addAll(implementation.getAlternatives());
                    }
                }

                if(LOG.isDebugEnabled()) {
                    LOG.debug(String.format("STUDY INCL ALT %s QUERIED %s", getClass().getName(), allImplementations.size()));
                }

                queryResult.setImplementations(allImplementations);
            } else {
                if(LOG.isDebugEnabled()) {
                    LOG.debug(String.format("STUDY %s QUERIED %s", getClass().getName(), implementations.size()));
                }

                queryResult.setImplementations(implementations);
            }
        } else {
            queryResult.setImplementations(new ArrayList<>());
        }

        return queryResult;
    }
}
