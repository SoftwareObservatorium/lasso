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
package de.uni_mannheim.swt.lasso.core.dto;

import java.util.List;

/**
 *
 * @author Marcus Kessel
 *
 */
public class RankRequest {

    private String strategy;
    private List<SystemMeta> candidates;
    private List<RankingCriterion> criteria;

    public List<SystemMeta> getCandidates() {
        return candidates;
    }

    public void setCandidates(List<SystemMeta> candidates) {
        this.candidates = candidates;
    }

    public List<RankingCriterion> getCriteria() {
        return criteria;
    }

    public void setCriteria(List<RankingCriterion> criteria) {
        this.criteria = criteria;
    }

    public String getStrategy() {
        return strategy;
    }

    public void setStrategy(String strategy) {
        this.strategy = strategy;
    }
}
