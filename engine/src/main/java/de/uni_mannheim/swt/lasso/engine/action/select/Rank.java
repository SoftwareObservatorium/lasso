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

import de.uni_mannheim.swt.lasso.core.model.System;
import de.uni_mannheim.swt.lasso.index.odisse.socora.ranking.Criterion;
import de.uni_mannheim.swt.lasso.index.odisse.socora.ranking.smoop.SmoopRanking;
import de.uni_mannheim.swt.lasso.core.model.ActionConfiguration;
import de.uni_mannheim.swt.lasso.datasource.maven.socora.CandidateRankingManager;
import de.uni_mannheim.swt.lasso.engine.LSLExecutionContext;
import de.uni_mannheim.swt.lasso.engine.action.DefaultAction;
import de.uni_mannheim.swt.lasso.engine.action.annotations.LassoAction;
import de.uni_mannheim.swt.lasso.engine.action.annotations.LassoInput;
import de.uni_mannheim.swt.lasso.engine.action.annotations.Local;
import de.uni_mannheim.swt.lasso.engine.action.annotations.Stable;
import de.uni_mannheim.swt.lasso.engine.data.ReportKey;
import de.uni_mannheim.swt.lasso.engine.data.ReportOperations;
import de.uni_mannheim.swt.lasso.core.model.Systems;
import de.uni_mannheim.swt.lasso.index.odisse.socora.ranking.smoop.strategy.*;
import de.uni_mannheim.swt.lasso.srm.DefaultSRM;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.tablesaw.api.Table;
import tech.tablesaw.columns.Column;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * An action which produces rankings based on SOCORA and any available numerical LASSO report attributes.
 *
 * @author Marcus Kessel
 */
@LassoAction(desc = "Rank Implementations using SOCORA")
@Stable
@Local
public class Rank extends DefaultAction {

    private static final Logger LOG = LoggerFactory
            .getLogger(Rank.class);

    private static CandidateRankingManager candidateRankingManager;
    static {
        try {
            candidateRankingManager = candidateRankingManager();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @LassoInput(desc = "Ranking strategy (default HDS)", optional = true)
    public String strategy = HybridNonDominatedSortingRanking.ID;

    @LassoInput(desc = "Ranking Criteria (METRIC_ID:objective:priority)", optional = true)
    public List<String> criteria = Arrays.asList("SelectReport.score:max:1");

    @LassoInput(desc = "Rank by best match", optional = true)
    public boolean bestMatch = false;

    public String criterion(String name, String objective, int priority) {
        return String.join(":", name, objective, String.valueOf(priority));
    }

    @Override
    public void execute(LSLExecutionContext context, ActionConfiguration actionConfiguration) throws IOException {
        if(LOG.isInfoEnabled()) {
            LOG.info("Executing "+  this.getClass());
        }

        Validate.notEmpty(criteria, "Ranking criteria cannot be empty");

        try {
            // set setExecutables() to be compliant with other actions
            Systems executables = Systems.fromAbstraction(actionConfiguration.getAbstraction(), getInstanceId());
            setExecutables(executables);

            //
            List<System> implementations = actionConfiguration.getAbstraction().getImplementations();

            Criterion[] criteria = toCriteria(this.criteria);

            ReportOperations recordOperations = context.getReportOperations();

            //
            List<Table> tables = new ArrayList<>(criteria.length);
            for(Criterion criterion : criteria) {
                //
                Table table = recordOperations.getValues(context.getExecutionId(),
                        actionConfiguration.getAbstraction(), criterion.getId());

                //System.out.println(table.print());

                tables.add(table);
            }

            // get measures
            implementations.forEach(implementation -> {
                Map<String, Double> measures = new HashMap<>();

                IntStream.range(0, criteria.length).forEach(i -> {
                    Criterion criterion = criteria[i];
                    Table table = tables.get(i);

                    Column<?> valCol;
                    if(bestMatch) {
                        LOG.info("Selecting by best match");

                        valCol = table.select(criterion.getId())
                                .where(table.stringColumn("SYSTEM")
                                        .isEqualTo(implementation.getId()).and(table.intColumn("PERMID")
                                                .isEqualTo(-1)))
                                .column(0);
                    } else {
                        valCol = table.select(criterion.getId())
                                .where(table.stringColumn("SYSTEM")
                                        .isEqualTo(implementation.getId()))
                                .column(0);
                    }

                    if(valCol.size() > 0) {
                        // FIXME assumes first value, in case of permutations there are N values for N permutations

                        // must be a number column
                        Number value = (Number) valCol.get(0);

                        measures.put(criterion.getId(), value.doubleValue());
                    }
                });

                // FIXME override issue?
                implementation.getCode().getMeasures().putAll(measures);
            });

            if(LOG.isDebugEnabled()) {
                LOG.debug(strategy);
                LOG.debug(ReflectionToStringBuilder.toString(criteria));
            }

            Map<String, Integer> sortMap = candidateRankingManager.rank(implementations, strategy, criteria);

            // write select report
            actionConfiguration.getAbstraction().getImplementations().forEach(impl -> {
                RankReport report = new RankReport();
                report.setRankCriteria(Arrays.stream(criteria).map(c -> c.getId()).collect(Collectors.joining(";")));

                report.setRankPosition(sortMap.get(impl.getId()));

                recordOperations.put(context.getExecutionId(), ReportKey.of(this, actionConfiguration.getAbstraction(), impl), report);
            });
        } catch (Throwable e) {
            e.printStackTrace();

            throw e;
        }
    }

    private Criterion[] toCriteria(List<String> crit) {
        Criterion[] criteria = crit.stream().map(c -> {
            String[] split = StringUtils.split(c, ":");

            Criterion criterion = new Criterion();

            // substitute aliases
            if(DefaultSRM.ATTRIBUTES_ALIASES.containsKey(split[0])) {
                split[0] = DefaultSRM.ATTRIBUTES_ALIASES.get(split[0]);
            }

            criterion.setId(split[0]);

            String objective = split[1];
            criterion.setObjective(StringUtils.equalsIgnoreCase("max", objective) ? Criterion.OBJECTIVE_MAX : Criterion.OBJECTIVE_MIN);
            criterion.setPriority(Integer.parseInt(split[2]));

            return criterion;
        }).toArray(Criterion[]::new);

        return criteria;
    }

    private static CandidateRankingManager candidateRankingManager() throws IOException {
        CandidateRankingManager rankingManager = new CandidateRankingManager();

        // init js wrapper
        SmoopRanking smoopRanking = new SmoopRanking();
        // set ranking strategies
        rankingManager.addRankingStrategy(new SingleObjectiveRanking(smoopRanking));
        rankingManager.addRankingStrategy(new HybridNonDominatedSortingRanking(smoopRanking));
        rankingManager.addRankingStrategy(new WeightedSumRanking(smoopRanking));
        rankingManager.addRankingStrategy(new WeightedEuclideanRanking(smoopRanking));
        rankingManager.addRankingStrategy(new LRRRanking(smoopRanking));
        rankingManager.addRankingStrategy(new RPHybridNonDominatedSortingRanking(smoopRanking));
        rankingManager.addRankingStrategy(new RPNonDominatedSortingRanking(smoopRanking));
        rankingManager.addRankingStrategy(new NonDominatedSortingRanking(smoopRanking));
        rankingManager.addRankingStrategy(new NormalizedWeightedDistanceRanking(smoopRanking));

        return rankingManager;
    }
 }
