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

import java.util.Map;

/**
 * An evaluated candidate method
 * 
 * @author Marcus Kessel
 *
 */
public class CandidateMethod {

    private int permId;
    private boolean ghost;
    private String javaSig;
    private String bcSig;
    private String positions;
    
    private Map<String, Double> metrics;
    
    /**
     * @return the permId
     */
    public int getPermId() {
        return permId;
    }
    /**
     * @param permId the permId to set
     */
    public void setPermId(int permId) {
        this.permId = permId;
    }
    /**
     * @return the ghost
     */
    public boolean isGhost() {
        return ghost;
    }
    /**
     * @param ghost the ghost to set
     */
    public void setGhost(boolean ghost) {
        this.ghost = ghost;
    }
    /**
     * @return the javaSig
     */
    public String getJavaSig() {
        return javaSig;
    }
    /**
     * @param javaSig the javaSig to set
     */
    public void setJavaSig(String javaSig) {
        this.javaSig = javaSig;
    }
    /**
     * @return the bcSig
     */
    public String getBcSig() {
        return bcSig;
    }
    /**
     * @param bcSig the bcSig to set
     */
    public void setBcSig(String bcSig) {
        this.bcSig = bcSig;
    }
    /**
     * @return the positions
     */
    public String getPositions() {
        return positions;
    }
    /**
     * @param positions the positions to set
     */
    public void setPositions(String positions) {
        this.positions = positions;
    }
    /**
     * @return the metrics
     */
    public Map<String, Double> getMetrics() {
        return metrics;
    }
    /**
     * @param metrics the metrics to set
     */
    public void setMetrics(Map<String, Double> metrics) {
        this.metrics = metrics;
    }
    
}
