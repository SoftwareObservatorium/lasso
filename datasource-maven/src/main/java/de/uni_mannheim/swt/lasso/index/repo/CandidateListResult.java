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
package de.uni_mannheim.swt.lasso.index.repo;

import java.util.List;

/**
 * A candidate result
 * 
 * @author Marcus Kessel
 *
 */
public class CandidateListResult {

    private List<CandidateDocument> candidates;
    
    private int start;
    private int rows;
    private long total;
    
    /**
     * @return the candidates
     */
    public List<CandidateDocument> getCandidates() {
        return candidates;
    }
    /**
     * @param candidates the candidates to set
     */
    public void setCandidates(List<CandidateDocument> candidates) {
        this.candidates = candidates;
    }
    /**
     * @return the start
     */
    public int getStart() {
        return start;
    }
    /**
     * @param start the start to set
     */
    public void setStart(int start) {
        this.start = start;
    }
    /**
     * @return the rows
     */
    public int getRows() {
        return rows;
    }
    /**
     * @param rows the rows to set
     */
    public void setRows(int rows) {
        this.rows = rows;
    }
    /**
     * @return the total
     */
    public long getTotal() {
        return total;
    }
    /**
     * @param total the total to set
     */
    public void setTotal(long total) {
        this.total = total;
    }
}
